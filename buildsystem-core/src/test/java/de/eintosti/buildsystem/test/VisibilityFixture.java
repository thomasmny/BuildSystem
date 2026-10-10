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
package de.eintosti.buildsystem.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.PlayerStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * A real {@link SettingsService} for visibility tests, backed by one {@link BuildPlayerImpl} per player so that each
 * player keeps their settings and archive state across calls.
 */
@NullMarked
public final class VisibilityFixture {

    public final Plugin plugin;
    public final ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
    public final PlayerServiceImpl playerService = mock(PlayerServiceImpl.class);
    public final PlayerStorageImpl playerStorage = mock(PlayerStorageImpl.class);
    public final SettingsService settingsService;
    private final Map<UUID, BuildPlayerImpl> buildPlayers = new HashMap<>();

    /**
     * Creates the fixture with the archive vanish on.
     *
     * @param plugin The plugin that hides and shows players
     */
    public VisibilityFixture(Plugin plugin) {
        this.plugin = plugin;
        when(playerService.getPlayerStorage()).thenReturn(playerStorage);
        when(playerStorage.getBuildPlayer(any(Player.class))).thenAnswer(call -> buildPlayer(call.getArgument(0)));
        when(playerStorage.createBuildPlayer(any(Player.class))).thenAnswer(call -> buildPlayer(call.getArgument(0)));
        archiveVanish(true);
        settingsService = new SettingsService(
                plugin,
                mock(TaskScheduler.class),
                configService,
                mock(Messages.class),
                playerService,
                mock(WorldStorageImpl.class));
    }

    /**
     * Turns the archive vanish in the config on or off.
     *
     * @param vanish Whether players in archive mode are hidden
     */
    public void archiveVanish(boolean vanish) {
        when(configService.current().settings().archive().vanish()).thenReturn(vanish);
    }

    /**
     * {@return the player's build player, created with default settings on first use}
     *
     * @param player The player
     */
    public BuildPlayerImpl buildPlayer(Player player) {
        return buildPlayers.computeIfAbsent(
                player.getUniqueId(), uuid -> new BuildPlayerImpl(uuid, new SettingsImpl()));
    }

    /**
     * Sets whether the player hides everyone else.
     *
     * @param player The player
     * @param hidesOthers Whether hide players is on
     */
    public void hidePlayers(Player player, boolean hidesOthers) {
        buildPlayer(player).getSettings().setHidePlayers(hidesOthers);
    }

    /**
     * Puts the player into archive mode, as entering an archive world does.
     *
     * @param player The player
     */
    public void enterArchive(Player player) {
        buildPlayer(player).getCachedValues().saveArchiveState(player);
    }
}
