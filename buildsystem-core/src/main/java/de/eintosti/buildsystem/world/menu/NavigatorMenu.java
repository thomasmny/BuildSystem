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
package de.eintosti.buildsystem.world.menu;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.player.menu.SettingsMenu;
import de.eintosti.buildsystem.menu.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class NavigatorMenu extends Menu {

    private final BuildSystemPlugin plugin;

    public NavigatorMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("old_navigator_title", player));
        this.plugin = plugin;
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillRange(player, getInventory(), 0, 27);

        getInventory().setItem(11, InventoryUtils.createSkull(messages.getString("old_navigator_world_navigator", player), Profileable.detect("d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622")));
        getInventory().setItem(12, InventoryUtils.createSkull(messages.getString("old_navigator_world_archive", player), Profileable.detect("7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8")));
        getInventory().setItem(13, InventoryUtils.createSkull(messages.getString("old_navigator_private_worlds", player), Profileable.of(player)));
        getInventory().setItem(15, InventoryUtils.createSkull(messages.getString("old_navigator_settings", player), Profileable.detect("1cba7277fc895bf3b673694159864b83351a4d14717e476ebda1c3bf38fcf37")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 11:
                new PublicWorldsMenu(plugin, player).open(player);
                break;
            case 12:
                new ArchivedWorldsMenu(plugin, player).open(player);
                break;
            case 13:
                new PrivateWorldsMenu(plugin, player).open(player);
                break;
            case 15:
                if (!player.hasPermission("buildsystem.settings")) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                new SettingsMenu(plugin, player).open(player);
                break;
            default:
                return;
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
    }
}
