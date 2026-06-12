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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.menu.InventoryUtils;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackupsConfirmationMenu extends Menu {

    private final Backup backup;
    private final String dateFormat;

    public BackupsConfirmationMenu(BuildSystemPlugin plugin, Backup backup, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("restore_backup_title", player));
        this.backup = backup;
        this.dateFormat = plugin.getConfigService().current().messages().dateFormat();
    }

    @Override
    protected void populate(Player player) {
        for (int slot : new int[]{0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21}) {
            getInventory().setItem(slot, InventoryUtils.createItem(XMaterial.LIME_STAINED_GLASS_PANE, "§a"));
        }
        for (int slot : new int[]{4, 13, 22}) {
            getInventory().setItem(slot, InventoryUtils.createItem(XMaterial.BLACK_STAINED_GLASS_PANE, "§0"));
        }
        for (int slot : new int[]{5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26}) {
            getInventory().setItem(slot, InventoryUtils.createItem(XMaterial.RED_STAINED_GLASS_PANE, "§c"));
        }

        getInventory().setItem(11, InventoryUtils.createItem(XMaterial.LIME_DYE,
                messages.getString("restore_backup_confirm_name", player),
                messages.getStringList("restore_backup_confirm_lore", player,
                        Map.entry("%timestamp%", StringUtils.formatTime(backup.creationTime(), dateFormat))
                )
        ));
        getInventory().setItem(15, InventoryUtils.createItem(XMaterial.RED_DYE,
                messages.getString("restore_backup_cancel_name", player)
        ));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 11 -> {
                player.closeInventory();
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                backup.owner().restoreBackup(backup, player);
            }
            case 15 -> {
                player.closeInventory();
                XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            }
        }
    }
}
