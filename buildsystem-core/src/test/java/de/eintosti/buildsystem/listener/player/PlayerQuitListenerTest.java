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
package de.eintosti.buildsystem.listener.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The inventory taken from a player entering an archive world only lives in memory, so quitting must hand it back
 * before the server saves the player, and joining inside the archive world must take it again.
 */
@NullMarked
class PlayerQuitListenerTest {

    private ServerMock server;
    private PlayerMock player;
    private SettingsImpl settings;
    private BuildPlayerImpl buildPlayer;
    private PlayerQuitListener listener;
    private PlayerService playerService;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        settings = new SettingsImpl();
        buildPlayer = new BuildPlayerImpl(UUID.randomUUID(), settings);

        playerService = mock(PlayerService.class, RETURNS_DEEP_STUBS);
        when(playerService.getPlayerStorage().getBuildPlayer(any(Player.class))).thenReturn(buildPlayer);
        when(playerService.getPlayerStorage().createBuildPlayer(any(Player.class)))
                .thenReturn(buildPlayer);
        settingsService = mock(SettingsService.class);
        when(settingsService.getSettings(any(Player.class))).thenReturn(settings);

        listener = new PlayerQuitListener(
                playerService,
                mock(NavigatorService.class),
                mock(NavigatorEditorService.class),
                mock(NoClipService.class),
                settingsService,
                mock(ConfigService.class, RETURNS_DEEP_STUBS),
                mock(Messages.class));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * What entering an archive world does: snapshot the state, then empty the inventory.
     */
    private void enterArchiveWorld() {
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
        buildPlayer.getCachedValues().saveArchiveState(player);
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
    }

    @Test
    void quittingInAnArchiveWorld_restoresTheInventoryAndGameMode() {
        enterArchiveWorld();

        listener.onPlayerQuit(new PlayerQuitEvent(player, (Component) null, QuitReason.DISCONNECTED));

        assertTrue(player.getInventory().contains(Material.DIAMOND, 3));
        assertEquals(GameMode.CREATIVE, player.getGameMode());
    }

    @Test
    void quittingInAnArchiveWorld_withClearInventory_stillClears() {
        settings.setClearInventory(true);
        enterArchiveWorld();

        listener.onPlayerQuit(new PlayerQuitEvent(player, (Component) null, QuitReason.DISCONNECTED));

        assertFalse(player.getInventory().contains(Material.DIAMOND));
    }

    @Test
    void rejoiningInAnArchiveWorld_takesTheInventoryAgain() {
        enterArchiveWorld();
        listener.onPlayerQuit(new PlayerQuitEvent(player, (Component) null, QuitReason.DISCONNECTED));

        BuildWorld archive = mock(BuildWorld.class, RETURNS_DEEP_STUBS);
        BuildWorldStatus archived = mock(BuildWorldStatus.class);
        when(archive.getData().get(WorldDataKey.STATUS)).thenReturn(archived);
        when(archive.getData().get(WorldDataKey.PHYSICS)).thenReturn(true);
        WorldStorage worldStorage = mock(WorldStorage.class);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(archive);
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().settings().archive())
                .thenReturn(new PluginConfig.Settings.Archive(false, true, GameMode.ADVENTURE));
        new PlayerJoinListener(
                        playerService,
                        settingsService,
                        mock(NavigatorService.class),
                        mock(SpawnService.class),
                        worldStorage,
                        mock(PlayerLookupService.class),
                        mock(NoClipService.class),
                        configService,
                        mock(Messages.class),
                        mock(UpdateChecker.class),
                        mock(TaskScheduler.class))
                .onPlayerJoin(new PlayerJoinEvent(player, (Component) null));

        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertEquals(GameMode.ADVENTURE, player.getGameMode());
        // Leaving the archive world hands it back again.
        buildPlayer.getCachedValues().resetArchiveStateIfPresent(player);
        assertTrue(player.getInventory().contains(Material.DIAMOND, 3));
    }
}
