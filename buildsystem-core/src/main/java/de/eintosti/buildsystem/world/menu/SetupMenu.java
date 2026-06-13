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

import static java.util.Map.entry;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.menu.InventoryUtils;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.menu.SkullTextures;
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
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SetupMenu extends Menu {

    private static final int SLOT_DEFAULT_HEADER = 10;
    private static final int SLOT_STATUS_HEADER = 19;

    private static final int FIRST_CREATE_SLOT = 11;
    private static final int LAST_CREATE_SLOT = 15;
    private static final int FIRST_STATUS_SLOT = 20;
    private static final int LAST_STATUS_SLOT = 25;

    private static final int FIRST_PLAYER_SLOT = 36;
    private static final int LAST_PLAYER_SLOT = 80;

    private static final Map<BuildWorldType, Integer> CREATE_ITEM_SLOTS = Map.ofEntries(
            entry(BuildWorldType.NORMAL, 11),
            entry(BuildWorldType.FLAT, 12),
            entry(BuildWorldType.NETHER, 13),
            entry(BuildWorldType.END, 14),
            entry(BuildWorldType.VOID, 15),
            entry(BuildWorldType.IMPORTED, 16));

    private static final Map<BuildWorldType, String> CREATE_ITEM_KEYS = Map.ofEntries(
            entry(BuildWorldType.NORMAL, "setup_normal_world"),
            entry(BuildWorldType.FLAT, "setup_flat_world"),
            entry(BuildWorldType.NETHER, "setup_nether_world"),
            entry(BuildWorldType.END, "setup_end_world"),
            entry(BuildWorldType.VOID, "setup_void_world"),
            entry(BuildWorldType.IMPORTED, "setup_imported_world"));

    private static final Map<BuildWorldStatus, Integer> STATUS_ITEM_SLOTS = Map.ofEntries(
            entry(BuildWorldStatus.NOT_STARTED, 20),
            entry(BuildWorldStatus.IN_PROGRESS, 21),
            entry(BuildWorldStatus.ALMOST_FINISHED, 22),
            entry(BuildWorldStatus.FINISHED, 23),
            entry(BuildWorldStatus.ARCHIVE, 24),
            entry(BuildWorldStatus.HIDDEN, 25));

    private static final Map<BuildWorldStatus, String> STATUS_ITEM_KEYS = Map.ofEntries(
            entry(BuildWorldStatus.NOT_STARTED, "status_not_started"),
            entry(BuildWorldStatus.IN_PROGRESS, "status_in_progress"),
            entry(BuildWorldStatus.ALMOST_FINISHED, "status_almost_finished"),
            entry(BuildWorldStatus.FINISHED, "status_finished"),
            entry(BuildWorldStatus.ARCHIVE, "status_archive"),
            entry(BuildWorldStatus.HIDDEN, "status_hidden"));

    private final BuildSystemPlugin plugin;
    private final CustomizableIcons icons;

    public SetupMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 36, plugin.getMessages().getString("setup_title", player));
        this.plugin = plugin;
        this.icons = plugin.getCustomizableIcons();
    }

    @Override
    protected void populate(Player player) {
        Inventory inv = getInventory();
        plugin.getMenuItems().fillAll(player, inv);

        addSectionHeaders(inv, player);
        addIconItems(inv, player, CREATE_ITEM_SLOTS, CREATE_ITEM_KEYS);
        addIconItems(inv, player, STATUS_ITEM_SLOTS, STATUS_ITEM_KEYS);
    }

    private void addSectionHeaders(Inventory inv, Player player) {
        inv.setItem(
                SLOT_DEFAULT_HEADER,
                InventoryUtils.createSkull(
                        messages.getString("setup_default_item_name", player),
                        Profileable.detect(SkullTextures.NEXT_PAGE),
                        messages.getStringList("setup_default_item_lore", player)));
        inv.setItem(
                SLOT_STATUS_HEADER,
                InventoryUtils.createSkull(
                        messages.getString("setup_status_item_name", player),
                        Profileable.detect(SkullTextures.NEXT_PAGE),
                        messages.getStringList("setup_status_item_name_lore", player)));
    }

    /**
     * Renders the configurable icon for each enum constant at its mapped slot.
     *
     * @param inv The inventory to populate
     * @param player The viewing player
     * @param slots The slot each enum constant occupies
     * @param keys The message key for each enum constant's display name
     * @param <T> The enum type (e.g. {@link BuildWorldType}, {@link BuildWorldStatus})
     */
    private <T extends Enum<T>> void addIconItems(
            Inventory inv, Player player, Map<T, Integer> slots, Map<T, String> keys) {
        slots.forEach((constant, slot) -> inv.setItem(
                slot, InventoryUtils.createItem(getIcon(constant), messages.getString(keys.get(constant), player))));
    }

    private XMaterial getIcon(Enum<?> constant) {
        if (constant instanceof BuildWorldType type) {
            return icons.getIcon(type);
        }
        return icons.getIcon((BuildWorldStatus) constant);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        InventoryAction action = event.getAction();
        switch (action) {
            case PICKUP_ALL,
                    PICKUP_ONE,
                    PICKUP_SOME,
                    PICKUP_HALF,
                    PLACE_ALL,
                    PLACE_SOME,
                    PLACE_ONE,
                    SWAP_WITH_CURSOR -> {
                if (event.getInventory().getType() != InventoryType.CHEST) {
                    return;
                }

                int slot = event.getRawSlot();
                event.setCancelled(slot < FIRST_PLAYER_SLOT || slot > LAST_PLAYER_SLOT);

                if (action != InventoryAction.SWAP_WITH_CURSOR) {
                    return;
                }

                boolean inPlayerInventory = slot >= FIRST_PLAYER_SLOT && slot <= LAST_PLAYER_SLOT;
                boolean isIconSlot = (slot >= FIRST_CREATE_SLOT && slot <= LAST_CREATE_SLOT)
                        || (slot >= FIRST_STATUS_SLOT && slot <= LAST_STATUS_SLOT);
                if (!inPlayerInventory && isIconSlot) {
                    ItemStack itemStack = event.getCursor();
                    event.setCurrentItem(itemStack);
                    event.getWhoClicked().setItemOnCursor(null);
                }
            }
            default -> event.setCancelled(true);
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        Inventory inv = getInventory();
        processIconMapping(inv, CREATE_ITEM_SLOTS, icons::setIcon);
        processIconMapping(inv, STATUS_ITEM_SLOTS, icons::setIcon);
    }

    /**
     * A generic helper method that iterates over a map of Enum-to-Slot, extracts the {@link ItemStack}, and sets the
     * corresponding icon.
     *
     * @param inventory The inventory to get items from
     * @param slotMapping A map from an Enum constant to its inventory slot index
     * @param <T> The type of the Enum (e.g., {@link BuildWorldType}, {@link BuildWorldStatus})
     */
    private <T extends Enum<T>> void processIconMapping(
            Inventory inventory, Map<T, Integer> slotMapping, BiConsumer<T, XMaterial> setter) {
        slotMapping.forEach((enumConstant, slot) -> {
            XMaterial material = Optional.ofNullable(inventory.getItem(slot))
                    .map(XMaterial::matchXMaterial)
                    .orElse(null);
            if (material == null) {
                plugin.getLogger()
                        .warning("Failed to set icon for " + enumConstant.name()
                                + " in setup inventory. ItemStack is null or not a valid Material.");
                return;
            }
            setter.accept(enumConstant, material);
        });
    }
}
