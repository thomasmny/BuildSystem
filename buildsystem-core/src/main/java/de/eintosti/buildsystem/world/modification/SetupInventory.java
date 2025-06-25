/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.world.modification;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.util.inventory.BuildSystemHolder;
import de.eintosti.buildsystem.util.inventory.BuildSystemInventory;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SetupInventory extends BuildSystemInventory {

    private static final Map<BuildWorldType, Integer> CREATE_ITEM_SLOTS = Map.of(
            BuildWorldType.NORMAL, 11,
            BuildWorldType.FLAT, 12,
            BuildWorldType.NETHER, 13,
            BuildWorldType.END, 14,
            BuildWorldType.VOID, 15,
            BuildWorldType.IMPORTED, 16
    );

    private static final Map<BuildWorldStatus, Integer> STATUS_ITEM_SLOTS = Map.of(
            BuildWorldStatus.NOT_STARTED, 20,
            BuildWorldStatus.IN_PROGRESS, 21,
            BuildWorldStatus.ALMOST_FINISHED, 22,
            BuildWorldStatus.FINISHED, 23,
            BuildWorldStatus.ARCHIVE, 24,
            BuildWorldStatus.HIDDEN, 25
    );

    private final CustomizableIcons icons;

    public SetupInventory(BuildSystemPlugin plugin) {
        this.icons = plugin.getCustomizableIcons();
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = new SetupInventoryHolder(this, player).getInventory();
        fillGuiWithGlass(player, inventory);

        inventory.setItem(10, InventoryUtils.createSkull(Messages.getString("setup_default_item_name", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"), Messages.getStringList("setup_default_item_lore", player)));
        inventory.setItem(19, InventoryUtils.createSkull(Messages.getString("setup_status_item_name", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158"), Messages.getStringList("setup_status_item_name_lore", player)));

        inventory.setItem(11, InventoryUtils.createItem(icons.getIcon(BuildWorldType.NORMAL), Messages.getString("setup_normal_world", player)));
        inventory.setItem(12, InventoryUtils.createItem(icons.getIcon(BuildWorldType.FLAT), Messages.getString("setup_flat_world", player)));
        inventory.setItem(13, InventoryUtils.createItem(icons.getIcon(BuildWorldType.NETHER), Messages.getString("setup_nether_world", player)));
        inventory.setItem(14, InventoryUtils.createItem(icons.getIcon(BuildWorldType.END), Messages.getString("setup_end_world", player)));
        inventory.setItem(15, InventoryUtils.createItem(icons.getIcon(BuildWorldType.VOID), Messages.getString("setup_void_world", player)));
        inventory.setItem(16, InventoryUtils.createItem(icons.getIcon(BuildWorldType.IMPORTED), Messages.getString("setup_imported_world", player)));

        inventory.setItem(20, InventoryUtils.createItem(icons.getIcon(BuildWorldStatus.NOT_STARTED), Messages.getString("status_not_started", player)));
        inventory.setItem(21, InventoryUtils.createItem(icons.getIcon(BuildWorldStatus.IN_PROGRESS), Messages.getString("status_in_progress", player)));
        inventory.setItem(22, InventoryUtils.createItem(icons.getIcon(BuildWorldStatus.ALMOST_FINISHED), Messages.getString("status_almost_finished", player)));
        inventory.setItem(23, InventoryUtils.createItem(icons.getIcon(BuildWorldStatus.FINISHED), Messages.getString("status_finished", player)));
        inventory.setItem(24, InventoryUtils.createItem(icons.getIcon(BuildWorldStatus.ARCHIVE), Messages.getString("status_archive", player)));
        inventory.setItem(25, InventoryUtils.createItem(icons.getIcon(BuildWorldStatus.HIDDEN), Messages.getString("status_hidden", player)));

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SetupInventoryHolder)) {
            return;
        }

        InventoryAction action = event.getAction();
        switch (action) {
            case PICKUP_ALL, PICKUP_ONE, PICKUP_SOME, PICKUP_HALF, PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> {
                if (event.getInventory().getType() != InventoryType.CHEST) {
                    return;
                }

                int slot = event.getRawSlot();
                event.setCancelled(slot < 36 || slot > 80);

                if (action != InventoryAction.SWAP_WITH_CURSOR) {
                    return;
                }

                if (!(slot >= 36 && slot <= 80)) {
                    if ((slot >= 11 && slot <= 15) || (slot >= 20 && slot <= 25)) {
                        ItemStack itemStack = event.getCursor();
                        event.setCurrentItem(itemStack);
                        event.getWhoClicked().setItemOnCursor(null);
                    }
                }
            }
            default -> event.setCancelled(true);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof SetupInventoryHolder)) {
            return;
        }

        processIconMapping(inventory, CREATE_ITEM_SLOTS, icons::setIcon);
        processIconMapping(inventory, STATUS_ITEM_SLOTS, icons::setIcon);
    }

    /**
     * A generic helper method that iterates over a map of Enum-to-Slot, extracts the {@link ItemStack}, and sets the corresponding icon.
     *
     * @param inventory   The inventory to get items from
     * @param slotMapping A map from an Enum constant to its inventory slot index
     * @param <T>         The type of the Enum (e.g., {@link BuildWorldType}, {@link BuildWorldStatus})
     */
    private <T extends Enum<T>> void processIconMapping(Inventory inventory, Map<T, Integer> slotMapping, BiConsumer<T, XMaterial> setter) {
        slotMapping.forEach((enumConstant, slot) -> {
            XMaterial material = Optional.ofNullable(inventory.getItem(slot))
                    .map(XMaterial::matchXMaterial)
                    .orElse(null);
            setter.accept(enumConstant, material);
        });
    }

    private static class SetupInventoryHolder extends BuildSystemHolder {

        public SetupInventoryHolder(BuildSystemInventory inventory, Player player) {
            super(inventory, 36, Messages.getString("setup_title", player));
        }
    }
}