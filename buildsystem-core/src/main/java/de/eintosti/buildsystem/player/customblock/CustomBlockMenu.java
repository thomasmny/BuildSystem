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
package de.eintosti.buildsystem.player.customblock;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.menu.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CustomBlockMenu extends Menu {

    private final BuildSystemPlugin plugin;

    public CustomBlockMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("blocks_title", player));
        this.plugin = plugin;
    }

    @Override
    protected void populate(Player player) {
        int[] glassSlots = {0, 8, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int i : glassSlots) {
            plugin.getMenuItems().addGlassPane(player, getInventory(), i);
        }

        setCustomBlock(player, 1, CustomBlock.FULL_OAK_BARCH);
        setCustomBlock(player, 2, CustomBlock.FULL_SPRUCE_BARCH);
        setCustomBlock(player, 3, CustomBlock.FULL_BIRCH_BARCH);
        setCustomBlock(player, 4, CustomBlock.FULL_JUNGLE_BARCH);
        setCustomBlock(player, 5, CustomBlock.FULL_ACACIA_BARCH);
        setCustomBlock(player, 6, CustomBlock.FULL_DARK_OAK_BARCH);

        setCustomBlock(player, 10, CustomBlock.RED_MUSHROOM);
        setCustomBlock(player, 11, CustomBlock.BROWN_MUSHROOM);
        setCustomBlock(player, 12, CustomBlock.FULL_MUSHROOM_STEM);
        setCustomBlock(player, 13, CustomBlock.MUSHROOM_STEM);
        setCustomBlock(player, 14, CustomBlock.MUSHROOM_BLOCK);

        setCustomBlock(player, 19, CustomBlock.SMOOTH_STONE);
        setCustomBlock(player, 20, CustomBlock.DOUBLE_STONE_SLAB);
        setCustomBlock(player, 21, CustomBlock.SMOOTH_SANDSTONE);
        setCustomBlock(player, 22, CustomBlock.SMOOTH_RED_SANDSTONE);

        setCustomBlock(player, 28, CustomBlock.POWERED_REDSTONE_LAMP);
        setCustomBlock(player, 29, CustomBlock.BURNING_FURNACE);
        setCustomBlock(player, 30, CustomBlock.PISTON_HEAD);
        setCustomBlock(player, 31, CustomBlock.COMMAND_BLOCK);
        setCustomBlock(player, 32, CustomBlock.BARRIER);
        setCustomBlock(player, 33, CustomBlock.INVISIBLE_ITEM_FRAME);

        setCustomBlock(player, 37, CustomBlock.MOB_SPAWNER);
        setCustomBlock(player, 38, CustomBlock.NETHER_PORTAL);
        setCustomBlock(player, 39, CustomBlock.END_PORTAL);
        setCustomBlock(player, 40, CustomBlock.DRAGON_EGG);
        setCustomBlock(player, 41, CustomBlock.DEBUG_STICK);
    }

    private void setCustomBlock(Player player, int position, CustomBlock customBlock) {
        getInventory().setItem(position, InventoryUtils.createSkull(
                messages.getString(customBlock.getMessageKey(), player),
                Profileable.detect(customBlock.getSkullUrl())
        ));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 1 -> giveCustomBlock(player, CustomBlock.FULL_OAK_BARCH);
            case 2 -> giveCustomBlock(player, CustomBlock.FULL_SPRUCE_BARCH);
            case 3 -> giveCustomBlock(player, CustomBlock.FULL_BIRCH_BARCH);
            case 4 -> giveCustomBlock(player, CustomBlock.FULL_JUNGLE_BARCH);
            case 5 -> giveCustomBlock(player, CustomBlock.FULL_ACACIA_BARCH);
            case 6 -> giveCustomBlock(player, CustomBlock.FULL_DARK_OAK_BARCH);

            case 10 -> giveCustomBlock(player, CustomBlock.RED_MUSHROOM);
            case 11 -> giveCustomBlock(player, CustomBlock.BROWN_MUSHROOM);
            case 12 -> giveCustomBlock(player, CustomBlock.FULL_MUSHROOM_STEM);
            case 13 -> giveCustomBlock(player, CustomBlock.MUSHROOM_STEM);
            case 14 -> giveCustomBlock(player, CustomBlock.MUSHROOM_BLOCK);

            case 19 -> giveCustomBlock(player, CustomBlock.SMOOTH_STONE);
            case 20 -> giveCustomBlock(player, CustomBlock.DOUBLE_STONE_SLAB);
            case 21 -> giveCustomBlock(player, CustomBlock.SMOOTH_SANDSTONE);
            case 22 -> giveCustomBlock(player, CustomBlock.SMOOTH_RED_SANDSTONE);

            case 28 -> giveCustomBlock(player, CustomBlock.POWERED_REDSTONE_LAMP);
            case 29 -> giveCustomBlock(player, CustomBlock.BURNING_FURNACE);
            case 30 -> giveCustomBlock(player, CustomBlock.PISTON_HEAD);
            case 31 -> giveCustomBlock(player, CustomBlock.COMMAND_BLOCK);
            case 32 -> giveCustomBlock(player, CustomBlock.BARRIER, XMaterial.BARRIER);
            case 33 -> giveCustomBlock(player, CustomBlock.INVISIBLE_ITEM_FRAME, XMaterial.ITEM_FRAME);

            case 37 -> giveCustomBlock(player, CustomBlock.MOB_SPAWNER);
            case 38 -> giveCustomBlock(player, CustomBlock.NETHER_PORTAL);
            case 39 -> giveCustomBlock(player, CustomBlock.END_PORTAL);
            case 40 -> giveCustomBlock(player, CustomBlock.DRAGON_EGG);
            case 41 -> giveCustomBlock(player, CustomBlock.DEBUG_STICK, XMaterial.DEBUG_STICK);
        }
    }

    private void giveCustomBlock(Player player, CustomBlock customBlock) {
        giveCustomBlock(player, customBlock, XMaterial.PLAYER_HEAD);
    }

    private void giveCustomBlock(Player player, CustomBlock customBlock, XMaterial material) {
        ItemStack itemStack;
        if (material == XMaterial.PLAYER_HEAD) {
            itemStack = InventoryUtils.createSkull(messages.getString(customBlock.getMessageKey(), player), Profileable.detect(customBlock.getSkullUrl()));
        } else {
            itemStack = InventoryUtils.createItem(material, messages.getString(customBlock.getMessageKey(), player));
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.addEnchant(XEnchantment.UNBREAKING.get(), 1, true);
            itemStack.setItemMeta(itemMeta);
        }
        player.getInventory().addItem(customBlock.storeCustomBlock(itemStack));
    }
}
