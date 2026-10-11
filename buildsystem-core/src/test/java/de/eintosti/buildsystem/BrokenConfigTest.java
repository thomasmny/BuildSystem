/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;

import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.StorageBackend;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedConstruction;

/**
 * A config.yml with broken syntax, which Bukkit would load as empty and a save would then replace with the defaults.
 */
@NullMarked
class BrokenConfigTest {

    private ServerMock server;
    private MockedConstruction<BuildSystemMetrics> metrics;
    private BuildSystemPlugin plugin;
    private Path config;
    private String broken;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        // bStats refuses to run unrelocated, which it only is in the shaded jar.
        metrics = mockConstruction(BuildSystemMetrics.class);
        plugin = MockBukkit.load(BuildSystemPlugin.class);
        config = new File(plugin.getDataFolder(), "config.yml").toPath();
        broken = Files.readString(config).replace("  type: sqlite", "  type: postgresql") + "broken: [unclosed\n";
    }

    @AfterEach
    void tearDown() {
        metrics.close();
        MockBukkit.unmock();
    }

    @Test
    void configReload_keepsTheFileAndTheSettingsInUse() throws Exception {
        Files.writeString(config, broken);

        assertFalse(plugin.reloadConfigData(true));

        assertEquals(broken, Files.readString(config));
        assertEquals("sqlite", plugin.getConfig().getString("storage.type"));
    }

    @Test
    void disable_keepsTheFile() throws Exception {
        Files.writeString(config, broken);

        server.getPluginManager().disablePlugin(plugin);

        assertEquals(broken, Files.readString(config));
    }

    @Test
    void load_refusesToStart_andKeepsTheFile() throws Exception {
        server.getPluginManager().disablePlugin(plugin);
        Files.writeString(config, broken);

        assertThrows(IllegalStateException.class, plugin::onLoad);

        assertEquals(broken, Files.readString(config));
    }

    @Test
    void disable_withATagOnThePasswordLine_stillSavesThePlayers() throws Exception {
        PluginConfig.Storage.Database settings = new ConfigService(plugin).readStorageSettings();
        UUID player = UUID.randomUUID();
        Objects.requireNonNull(server.getServicesManager().load(BuildSystem.class))
                .getPlayerService()
                .getPlayerStorage()
                .createBuildPlayer(player);
        Files.writeString(config, withPostgresPassword("!canary"));

        server.getPluginManager().disablePlugin(plugin);

        StorageBackend backend =
                StorageBackend.connect(settings.type(), settings, plugin.getDataFolder(), plugin.getLogger());
        try {
            assertTrue(backend.collection(EntityKind.PLAYERS).loadAll().containsKey(player.toString()));
        } finally {
            backend.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"%s: x", "*%s", "!!%s x", "!%s"})
    void aSyntaxErrorOnThePasswordLine_neverShowsThePassword(String passwordLine) throws Exception {
        String canary = "canary-7f3a9c";
        Files.writeString(config, withPostgresPassword(passwordLine.formatted(canary)));
        List<String> logged = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(String.valueOf(record.getMessage()));
                for (Throwable thrown = record.getThrown(); thrown != null; thrown = thrown.getCause()) {
                    logged.add(String.valueOf(thrown));
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        plugin.getLogger().addHandler(handler);
        Bukkit.getLogger().addHandler(handler);
        try {
            assertFalse(plugin.reloadConfigData(true));
            server.dispatchCommand(server.getConsoleSender(), "buildsystem storage migrate postgresql");
            for (int i = 0; i < 20; i++) {
                server.getScheduler().performOneTick();
            }
            server.getPluginManager().disablePlugin(plugin);
            // A fresh start, where Bukkit has not read config.yml yet.
            Field loadedConfig = JavaPlugin.class.getDeclaredField("newConfig");
            loadedConfig.setAccessible(true);
            loadedConfig.set(plugin, null);
            assertThrows(IllegalStateException.class, plugin::onLoad);
        } finally {
            plugin.getLogger().removeHandler(handler);
            Bukkit.getLogger().removeHandler(handler);
        }
        List<String> chat = new ArrayList<>();
        for (String message = server.getConsoleSender().nextMessage();
                message != null;
                message = server.getConsoleSender().nextMessage()) {
            chat.add(message);
        }

        assertTrue(Files.readString(config).contains(canary), "the test must break the password line");
        assertTrue(logged.stream().anyMatch(message -> message.contains("line ")), String.valueOf(logged));
        assertTrue(chat.stream().anyMatch(message -> message.contains("line ")), String.valueOf(chat));
        assertTrue(logged.stream().noneMatch(message -> message.contains(canary)), String.valueOf(logged));
        assertTrue(chat.stream().noneMatch(message -> message.contains(canary)), String.valueOf(chat));
    }

    private String withPostgresPassword(String value) throws IOException {
        return Files.readString(config)
                .replaceFirst(
                        "(?m)^(  postgresql:\\n(?:    .*\\n)*?    password: ).*$",
                        "$1" + Matcher.quoteReplacement(value));
    }
}
