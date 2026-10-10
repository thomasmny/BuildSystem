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
package de.eintosti.buildsystem.player.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * Who sees whom after {@link SettingsService#updateVisibility(Player)}, for a player who either hides others, is
 * vanished in an archive world, or both, next to a viewer in the same situations.
 */
@NullMarked
class SettingsServiceVisibilityTest {

    private ServerMock server;
    private WorldMock archive;
    private Plugin plugin;
    private SettingsService settingsService;
    private final Map<Player, SettingsImpl> settings = new HashMap<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // Players join in the first world, so that one is not the archive.
        server.addSimpleWorld("world");
        archive = server.addSimpleWorld("archive");
        plugin = MockBukkit.createMockPlugin();

        PlayerStorageImpl playerStorage = mock(PlayerStorageImpl.class);
        PlayerServiceImpl playerService = mock(PlayerServiceImpl.class);
        when(playerService.getPlayerStorage()).thenReturn(playerStorage);
        when(playerStorage.getBuildPlayer(any(Player.class))).thenAnswer(call -> {
            Player player = call.getArgument(0);
            return new BuildPlayerImpl(player.getUniqueId(), settings.get(player));
        });

        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.ARCHIVE_STATUS);
        BuildWorld archiveWorld = mock(BuildWorld.class);
        when(archiveWorld.getData()).thenReturn(data);
        WorldStorageImpl worldStorage = mock(WorldStorageImpl.class);
        when(worldStorage.getBuildWorld(archive)).thenReturn(archiveWorld);
        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);

        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().settings().archive().vanish()).thenReturn(true);

        settingsService = new SettingsService(
                plugin, mock(TaskScheduler.class), configService, mock(Messages.class), playerService, worldService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest(name = "player hides {0}, vanished {1}; other hides {2}, vanished {3}")
    @CsvSource({
        "false, false, false, false, true, true",
        "true, false, false, false, true, false",
        "false, false, true, false, false, true",
        "false, true, false, false, false, true",
        "false, false, false, true, true, false",
        "true, true, true, true, false, false",
    })
    void eachSideSeesTheOtherUnlessHidingOrVanished(
            boolean playerHides,
            boolean playerVanished,
            boolean otherHides,
            boolean otherVanished,
            boolean otherSeesPlayer,
            boolean playerSeesOther) {
        PlayerMock player = join("Player", playerHides, playerVanished);
        PlayerMock other = join("Other", otherHides, otherVanished);
        // Start from the opposite of what is expected, so a missing show or hide is noticed.
        setVisible(other, player, !otherSeesPlayer);
        setVisible(player, other, !playerSeesOther);

        settingsService.updateVisibility(player);

        assertEquals(otherSeesPlayer, other.canSee(player));
        assertEquals(playerSeesOther, player.canSee(other));
    }

    private PlayerMock join(String name, boolean hidesOthers, boolean vanished) {
        PlayerMock player = server.addPlayer(name);
        SettingsImpl playerSettings = new SettingsImpl();
        playerSettings.setHidePlayers(hidesOthers);
        settings.put(player, playerSettings);
        if (vanished) {
            player.teleport(archive.getSpawnLocation());
        }
        return player;
    }

    private void setVisible(PlayerMock viewer, PlayerMock target, boolean visible) {
        if (visible) {
            viewer.showPlayer(plugin, target);
        } else {
            viewer.hidePlayer(plugin, target);
        }
    }
}
