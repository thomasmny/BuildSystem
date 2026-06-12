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
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.menu.InventoryUtils;
import de.eintosti.buildsystem.world.menu.EditInventory;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class StatusInventory extends Menu {

    private final BuildSystemPlugin plugin;
    private final PlayerServiceImpl playerService;
    private final BuildWorld buildWorld;

    public StatusInventory(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("status_title", player,
                Map.entry("%world%", formatWorldName(buildWorld))));
        this.plugin = plugin;
        this.playerService = plugin.getPlayerService();
        this.buildWorld = buildWorld;
    }

    private static String formatWorldName(BuildWorld buildWorld) {
        String worldName = buildWorld.getName();
        if (worldName.length() > 17) {
            worldName = worldName.substring(0, 14) + "...";
        }
        return worldName;
    }

    @Override
    protected void populate(Player player) {
        for (int i = 0; i <= 9; i++) {
            InventoryUtils.addGlassPane(player, getInventory(), i);
        }
        for (int i = 17; i <= 26; i++) {
            InventoryUtils.addGlassPane(player, getInventory(), i);
        }

        addStatusItem(player, 10, BuildWorldStatus.NOT_STARTED);
        addStatusItem(player, 11, BuildWorldStatus.IN_PROGRESS);
        addStatusItem(player, 12, BuildWorldStatus.ALMOST_FINISHED);
        addStatusItem(player, 13, BuildWorldStatus.FINISHED);
        addStatusItem(player, 14, BuildWorldStatus.ARCHIVE);
        addStatusItem(player, 16, BuildWorldStatus.HIDDEN);
    }

    /**
     * Adds a status item to the inventory at the specified position.
     *
     * @param player   The player who will see the item
     * @param position The position in the inventory to add the item
     * @param status   The status to represent with the item
     */
    private void addStatusItem(Player player, int position, BuildWorldStatus status) {
        XMaterial material = plugin.getCustomizableIcons().getIcon(status);
        String displayName = messages.getString(Messages.getMessageKey(status), player);

        if (!player.hasPermission(status.getPermission())) {
            material = XMaterial.BARRIER;
            displayName = "§c§m" + ChatColor.stripColor(displayName);
        }

        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(displayName);
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(itemMeta);

        if (buildWorld.getData().status().get() == status) {
            itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);
        }

        getInventory().setItem(position, itemStack);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        if (slot < 10 || slot > 14 && slot != 16) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            new EditInventory(plugin, buildWorld, player).open(player);
            return;
        }

        BuildWorldStatus status = getStatusFromSlot(slot);
        if (!player.hasPermission(status.getPermission())) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        player.closeInventory();
        buildWorld.getData().status().set(status);
        plugin.getSettingsService().forceUpdateSidebar(buildWorld);

        XSound.ENTITY_CHICKEN_EGG.play(player);
        messages.sendMessage(player, "worlds_setstatus_set",
                Map.entry("%world%", buildWorld.getName()),
                Map.entry("%status%", messages.getString(Messages.getMessageKey(status), player))
        );
    }

    private BuildWorldStatus getStatusFromSlot(int slot) {
        return switch (slot) {
            case 10 -> BuildWorldStatus.NOT_STARTED;
            case 11 -> BuildWorldStatus.IN_PROGRESS;
            case 12 -> BuildWorldStatus.ALMOST_FINISHED;
            case 13 -> BuildWorldStatus.FINISHED;
            case 14 -> BuildWorldStatus.ARCHIVE;
            case 16 -> BuildWorldStatus.HIDDEN;
            default -> throw new IllegalArgumentException("Slot " + slot + " does not correspond to status");
        };
    }
}
