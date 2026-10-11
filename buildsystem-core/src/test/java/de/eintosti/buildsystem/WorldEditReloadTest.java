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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.world.World;
import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedConstruction;

/**
 * Drives the real plugin through a disable with WorldEdit present. WorldEdit's event bus outlives the plugin, so a
 * listener left on it keeps judging edits after a reload.
 */
@NullMarked
class WorldEditReloadTest {

    private ServerMock server;
    private MockedConstruction<BuildSystemMetrics> metrics;
    private BuildSystemPlugin plugin;
    private PlayerMock player;

    @TempDir
    File worldContainer;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public File getWorldContainer() {
                return worldContainer;
            }
        });
        // bStats refuses to run unrelocated, which it only is in the shaded jar.
        metrics = mockConstruction(BuildSystemMetrics.class);
        MockBukkit.createMockPlugin("WorldEdit");
        server.addSimpleWorld("archive");
        plugin = MockBukkit.load(BuildSystemPlugin.class);
        // The scoreboard needs a real server class, so it is turned off before anyone joins.
        plugin.getConfig().set("settings.scoreboard", false);
        plugin.saveConfig();
        plugin.reloadConfigData(false);

        worldStorage()
                .addBuildWorld(new BuildWorldImpl(
                        TestData.worldContext(),
                        UUID.randomUUID(),
                        "archive",
                        BuildWorldType.NORMAL,
                        WorldDataSchema.create("archive", TestData.ARCHIVE_STATUS),
                        Builder.of(UUID.randomUUID(), "Creator"),
                        List.of(),
                        System.currentTimeMillis(),
                        null,
                        null));
        player = SoundlessPlayer.join(server, "Player");
    }

    @AfterEach
    void tearDown() {
        metrics.close();
        MockBukkit.unmock();
    }

    @Test
    void disabling_stopsJudgingWorldEditEdits() {
        assertInstanceOf(NullExtent.class, postEdit("archive").getExtent(), "blocked while enabled");

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(postEdit("archive").getExtent() instanceof NullExtent);
    }

    private EditSessionEvent postEdit(String worldName) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);
        Actor actor = mock(Actor.class);
        when(actor.isPlayer()).thenReturn(true);
        when(actor.getUniqueId()).thenReturn(player.getUniqueId());
        EditSessionEvent event = new EditSessionEvent(world, actor, -1, EditSession.Stage.BEFORE_CHANGE);
        WorldEdit.getInstance().getEventBus().post(event);
        return event;
    }

    private WorldStorageImpl worldStorage() {
        return (WorldStorageImpl)
                Objects.requireNonNull(server.getServicesManager().load(BuildSystem.class))
                        .getWorldService()
                        .getWorldStorage();
    }
}
