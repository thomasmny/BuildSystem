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

import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.Permissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerChangedWorldListener implements Listener {

    private final NavigatorService navigatorService;
    private final PlayerServiceImpl playerManager;
    private final SettingsService settingsManager;
    private final WorldStorage worldStorage;
    private final ConfigService configService;
    private final Messages messages;

    public PlayerChangedWorldListener(
            NavigatorService navigatorService,
            PlayerServiceImpl playerManager,
            SettingsService settingsManager,
            WorldStorage worldStorage,
            ConfigService configService,
            Messages messages) {
        this.navigatorService = navigatorService;
        this.playerManager = playerManager;
        this.settingsManager = settingsManager;
        this.worldStorage = worldStorage;
        this.configService = configService;
        this.messages = messages;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        event.getPlayer().setAllowFlight(true);

        BuildWorld oldWorld = worldStorage.getBuildWorld(event.getFrom());
        if (oldWorld != null && configService.current().world().unload().enabled()) {
            oldWorld.getUnloader().resetUnloadTask();
        }

        BuildWorld newWorld = worldStorage.getBuildWorld(player.getWorld());
        if (newWorld != null
                && !newWorld.getData().get(WorldDataKey.PHYSICS)
                && player.hasPermission(Permissions.PHYSICS_MESSAGE)) {
            messages.sendMessage(
                    player, "physics_deactivated_in_world", Placeholders.of("%world%", newWorld.getName()));
        }

        removeOldNavigator(player);
        removeBuildMode(player);
        checkWorldStatus(player);
        settingsManager.updateVisibility(player);

        if (settingsManager.getSettings(player).isScoreboard()) {
            settingsManager.forceUpdateSidebar(player);
        }
    }

    private void removeOldNavigator(Player player) {
        navigatorService.removeArmorStands(player);
        player.removePotionEffect(XPotion.BLINDNESS.get());
    }

    private void removeBuildMode(Player player) {
        if (!playerManager.endBuildSession(player)) {
            return;
        }

        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
        messages.sendMessage(player, "build_deactivated_self");
    }

    private void checkWorldStatus(Player player) {
        CachedValues cachedValues = cachedValues(player);
        ArchiveMode.exit(player, cachedValues);

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            return;
        }

        if (!buildWorld.getData().get(WorldDataKey.STATUS).isBuildingAllowed()) {
            ArchiveMode.enter(
                    player, cachedValues, configService.current().settings().archive());
        }
        navigatorService.giveNavigator(player);
    }

    private CachedValues cachedValues(Player player) {
        return BuildPlayerImpl.of(playerManager.getPlayerStorage().getBuildPlayer(player))
                .getCachedValues();
    }
}
