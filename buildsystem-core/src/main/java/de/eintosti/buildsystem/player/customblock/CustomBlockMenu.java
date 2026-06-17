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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CustomBlockMenu extends ButtonMenu<MenuButton> {

    /**
     * A selectable custom block. {@code giveMaterial} is the material the item is handed out as; the default
     * {@link XMaterial#PLAYER_HEAD} means "give as a skull". Render is always a skull for every slot.
     */
    record BlockEntry(CustomBlock block, XMaterial giveMaterial) {
        BlockEntry(CustomBlock block) {
            this(block, XMaterial.PLAYER_HEAD);
        }
    }

    /**
     * The single source of truth for the block selection grid: each slot maps to the block it gives. Slots 32, 33 and 41
     * give with a non-skull material; all others give as a skull. Drives both {@link #populate} and {@link #handleClick}.
     */
    private static final Map<Integer, BlockEntry> BLOCK_BY_SLOT = Map.ofEntries(
            Map.entry(1, new BlockEntry(CustomBlock.FULL_OAK_BARCH)),
            Map.entry(2, new BlockEntry(CustomBlock.FULL_SPRUCE_BARCH)),
            Map.entry(3, new BlockEntry(CustomBlock.FULL_BIRCH_BARCH)),
            Map.entry(4, new BlockEntry(CustomBlock.FULL_JUNGLE_BARCH)),
            Map.entry(5, new BlockEntry(CustomBlock.FULL_ACACIA_BARCH)),
            Map.entry(6, new BlockEntry(CustomBlock.FULL_DARK_OAK_BARCH)),
            Map.entry(10, new BlockEntry(CustomBlock.RED_MUSHROOM)),
            Map.entry(11, new BlockEntry(CustomBlock.BROWN_MUSHROOM)),
            Map.entry(12, new BlockEntry(CustomBlock.FULL_MUSHROOM_STEM)),
            Map.entry(13, new BlockEntry(CustomBlock.MUSHROOM_STEM)),
            Map.entry(14, new BlockEntry(CustomBlock.MUSHROOM_BLOCK)),
            Map.entry(19, new BlockEntry(CustomBlock.SMOOTH_STONE)),
            Map.entry(20, new BlockEntry(CustomBlock.DOUBLE_STONE_SLAB)),
            Map.entry(21, new BlockEntry(CustomBlock.SMOOTH_SANDSTONE)),
            Map.entry(22, new BlockEntry(CustomBlock.SMOOTH_RED_SANDSTONE)),
            Map.entry(28, new BlockEntry(CustomBlock.POWERED_REDSTONE_LAMP)),
            Map.entry(29, new BlockEntry(CustomBlock.BURNING_FURNACE)),
            Map.entry(30, new BlockEntry(CustomBlock.PISTON_HEAD)),
            Map.entry(31, new BlockEntry(CustomBlock.COMMAND_BLOCK)),
            Map.entry(32, new BlockEntry(CustomBlock.BARRIER, XMaterial.BARRIER)),
            Map.entry(33, new BlockEntry(CustomBlock.INVISIBLE_ITEM_FRAME, XMaterial.ITEM_FRAME)),
            Map.entry(37, new BlockEntry(CustomBlock.MOB_SPAWNER)),
            Map.entry(38, new BlockEntry(CustomBlock.NETHER_PORTAL)),
            Map.entry(39, new BlockEntry(CustomBlock.END_PORTAL)),
            Map.entry(40, new BlockEntry(CustomBlock.DRAGON_EGG)),
            Map.entry(41, new BlockEntry(CustomBlock.DEBUG_STICK, XMaterial.DEBUG_STICK)));

    private final BuildSystemPlugin plugin;

    public CustomBlockMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("blocks_title", player));
        this.plugin = plugin;

        BLOCK_BY_SLOT.forEach((slot, entry) -> register(slot, blockButton(entry)));
    }

    private MenuButton blockButton(BlockEntry entry) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(
                                Profileable.detect(entry.block().getSkullUrl()))
                        .name(messages.getString(entry.block().getMessageKey(), player))
                        .into(inventory, slot))
                .onClick((player, event) -> giveCustomBlock(player, entry.block(), entry.giveMaterial()))
                .build();
    }

    @Override
    protected void populate(Player player) {
        int[] glassSlots = {0, 8, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int i : glassSlots) {
            plugin.getMenuItems().addGlassPane(player, getInventory(), i);
        }

        renderButtons(player);
    }

    /**
     * The slot &rarr; block mapping. Exposed for the golden test that pins the selection grid.
     */
    Map<Integer, BlockEntry> blockBySlot() {
        return BLOCK_BY_SLOT;
    }

    private void giveCustomBlock(Player player, CustomBlock customBlock, XMaterial material) {
        ItemStack itemStack;
        if (material == XMaterial.PLAYER_HEAD) {
            itemStack = ItemBuilder.skull(Profileable.detect(customBlock.getSkullUrl()))
                    .name(messages.getString(customBlock.getMessageKey(), player))
                    .build();
        } else {
            itemStack = ItemBuilder.of(material)
                    .name(messages.getString(customBlock.getMessageKey(), player))
                    .glow(true)
                    .build();
        }
        player.getInventory().addItem(customBlock.storeCustomBlock(itemStack));
    }
}
