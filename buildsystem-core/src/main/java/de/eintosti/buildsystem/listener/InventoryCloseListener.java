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
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.inventory.XInventoryView;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryCloseListener implements Listener {

    private static final Map<BuildWorldType, Integer> CREATE_ITEM_SLOTS = Map.of(
            BuildWorldType.NORMAL, 11,
            BuildWorldType.FLAT, 12,
            BuildWorldType.NETHER, 13,
            BuildWorldType.END, 14,
            BuildWorldType.VOID, 15
    );

    private static final Map<BuildWorldStatus, Integer> STATUS_ITEM_SLOTS = Map.of(
            BuildWorldStatus.NOT_STARTED, 29,
            BuildWorldStatus.IN_PROGRESS, 30,
            BuildWorldStatus.ALMOST_FINISHED, 31,
            BuildWorldStatus.FINISHED, 32,
            BuildWorldStatus.ARCHIVE, 33,
            BuildWorldStatus.HIDDEN, 34
    );

    private final CustomizableIcons icons;

    public InventoryCloseListener(BuildSystemPlugin plugin) {
        this.icons = plugin.getCustomizableIcons();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSetupInventoryClose(InventoryCloseEvent event) {
        String title = XInventoryView.of(event.getView()).getTitle();
        if (!title.equals(Messages.getString("setup_title", (Player) event.getPlayer()))) {
            return;
        }

        Inventory inventory = event.getInventory();
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
}