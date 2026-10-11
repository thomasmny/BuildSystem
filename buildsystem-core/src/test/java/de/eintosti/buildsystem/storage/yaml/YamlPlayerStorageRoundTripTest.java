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
package de.eintosti.buildsystem.storage.yaml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlPlayerStorageRoundTripTest {

    @TempDir
    File dataFolder;

    private BuildSystemPlugin plugin;
    private TaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        scheduler = new TaskScheduler(plugin);
    }

    private PlayerStorageImpl newStorage() {
        return new PlayerStorageImpl(plugin, scheduler);
    }

    private BuildPlayerImpl samplePlayer(UUID uuid) {
        SettingsImpl settings = new SettingsImpl();
        settings.setNavigatorType(NavigatorType.NEW);
        settings.setNoClip(true);
        settings.setNightVision(true);
        settings.setKeepNavigator(true);
        settings.setSlabBreaking(true);
        settings.setClearInventory(true);

        BuildPlayerImpl player = new BuildPlayerImpl(uuid, settings);
        player.setLogoutLocation(new LogoutLocation("lobby", 1.5, 64.0, -3.25, 90.0f, -10.0f));
        return player;
    }

    @Test
    void saveAndLoad_roundTripsSettingsAndLogoutLocation() throws Exception {
        UUID uuid = UUID.randomUUID();
        BuildPlayerImpl original = samplePlayer(uuid);

        newStorage().save(original).join();

        Collection<BuildPlayer> loaded = newStorage().load().get();
        assertEquals(1, loaded.size());

        BuildPlayerImpl reloaded = BuildPlayerImpl.of(loaded.iterator().next());
        assertEquals(uuid, reloaded.getUniqueId());
        assertEquals(NavigatorType.NEW, reloaded.getSettings().getNavigatorType());
        assertTrue(reloaded.getSettings().isNoClip());
        assertTrue(reloaded.getSettings().isNightVision());
        assertTrue(reloaded.getSettings().isKeepNavigator());
        assertTrue(reloaded.getSettings().isSlabBreaking());
        assertTrue(reloaded.getSettings().isClearInventory());
        assertFalse(reloaded.getSettings().isHidePlayers());

        LogoutLocation logoutLocation = reloaded.getLogoutLocation();
        assertNotNull(logoutLocation);
        assertEquals(original.getLogoutLocation().toString(), logoutLocation.toString());
    }

    @Test
    void load_missingFile_returnsEmptyCollection() throws Exception {
        Collection<BuildPlayer> loaded = newStorage().load().get();

        assertTrue(loaded.isEmpty());
    }

    @Test
    void aFileWithBrokenSyntax_isKept_andNotSavedOver_untilItLoadsAgain() throws Exception {
        assertKeptAndNotSavedOver(healthy -> healthy + "broken: [unclosed\n");
    }

    @Test
    void aFileWithAnUnreadableArchiveItem_isKept_andNotSavedOver_untilItLoadsAgain() throws Exception {
        // An item the server cannot read back, such as one another server software saved, fails the whole file.
        assertKeptAndNotSavedOver(healthy -> {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(healthy);
            String snapshot = "players." + UUID.randomUUID() + ".archive-snapshot";
            config.set(snapshot + ".gamemode", "SURVIVAL");
            config.set(snapshot + ".inventory", List.of(Map.of("==", "com.example.UnknownItem", "count", 5)));
            config.set(snapshot + ".armor", List.of());
            return config.saveToString();
        });
    }

    private void assertKeptAndNotSavedOver(BrokenFile breaker) throws Exception {
        UUID stored = UUID.randomUUID();
        newStorage().save(samplePlayer(stored)).join();
        Path players = dataFolder.toPath().resolve("players.yml");
        String healthy = Files.readString(players);
        String broken = breaker.breakFile(healthy);
        Files.writeString(players, broken);

        PlayerStorageImpl storage = newStorage();
        assertTrue(storage.load().get().isEmpty());
        storage.save(samplePlayer(UUID.randomUUID())).join();
        assertEquals(broken, Files.readString(players), "a file that failed to load is never written over");
        File[] copies = dataFolder.listFiles((dir, name) -> name.startsWith("players.yml.broken-"));
        assertNotNull(copies);
        assertEquals(1, copies.length);
        assertEquals(broken, Files.readString(copies[0].toPath()));

        Files.writeString(players, healthy);
        assertEquals(
                List.of(stored),
                storage.load().get().stream().map(BuildPlayer::getUniqueId).toList());
        UUID joined = UUID.randomUUID();
        storage.save(samplePlayer(joined)).join();
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(players.toFile());
        assertTrue(saved.isConfigurationSection("players." + stored));
        assertTrue(saved.isConfigurationSection("players." + joined), "saving works again once the file loads");
    }

    @FunctionalInterface
    private interface BrokenFile {
        String breakFile(String healthy) throws Exception;
    }
}
