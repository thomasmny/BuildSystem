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
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
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

    /**
     * Where a configurable icon sits and what message names it. Pairs the slot and message key that were previously held
     * in two parallel maps per section.
     *
     * @param slot The inventory slot the icon occupies
     * @param nameKey The message key for the icon's display name
     */
    private record IconDef(int slot, String nameKey) {}

    private static final Map<BuildWorldType, IconDef> CREATE_ICONS = Map.ofEntries(
            entry(BuildWorldType.NORMAL, new IconDef(11, "setup_normal_world")),
            entry(BuildWorldType.FLAT, new IconDef(12, "setup_flat_world")),
            entry(BuildWorldType.NETHER, new IconDef(13, "setup_nether_world")),
            entry(BuildWorldType.END, new IconDef(14, "setup_end_world")),
            entry(BuildWorldType.VOID, new IconDef(15, "setup_void_world")),
            entry(BuildWorldType.IMPORTED, new IconDef(16, "setup_imported_world")));

    private static final Map<BuildWorldStatus, IconDef> STATUS_ICONS = Map.ofEntries(
            entry(BuildWorldStatus.NOT_STARTED, new IconDef(20, "status_not_started")),
            entry(BuildWorldStatus.IN_PROGRESS, new IconDef(21, "status_in_progress")),
            entry(BuildWorldStatus.ALMOST_FINISHED, new IconDef(22, "status_almost_finished")),
            entry(BuildWorldStatus.FINISHED, new IconDef(23, "status_finished")),
            entry(BuildWorldStatus.ARCHIVE, new IconDef(24, "status_archive")),
            entry(BuildWorldStatus.HIDDEN, new IconDef(25, "status_hidden")));

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
        addIconItems(inv, player, CREATE_ICONS, icons::getIcon);
        addIconItems(inv, player, STATUS_ICONS, icons::getIcon);
    }

    private void addSectionHeaders(Inventory inv, Player player) {
        ItemBuilder.skull(Profileable.detect(SkullTextures.NEXT_PAGE))
                .name(messages.getString("setup_default_item_name", player))
                .lore(messages.getStringList("setup_default_item_lore", player))
                .into(inv, SLOT_DEFAULT_HEADER);
        ItemBuilder.skull(Profileable.detect(SkullTextures.NEXT_PAGE))
                .name(messages.getString("setup_status_item_name", player))
                .lore(messages.getStringList("setup_status_item_name_lore", player))
                .into(inv, SLOT_STATUS_HEADER);
    }

    /**
     * Renders the configurable icon for each enum constant at its mapped slot.
     *
     * @param inv The inventory to populate
     * @param player The viewing player
     * @param iconDefs The slot and name key for each enum constant
     * @param iconResolver Resolves an enum constant to its currently configured icon material
     * @param <T> The enum type (e.g. {@link BuildWorldType}, {@link BuildWorldStatus})
     */
    private <T extends Enum<T>> void addIconItems(
            Inventory inv, Player player, Map<T, IconDef> iconDefs, Function<T, XMaterial> iconResolver) {
        iconDefs.forEach((constant, def) -> ItemBuilder.of(iconResolver.apply(constant))
                .name(messages.getString(def.nameKey(), player))
                .into(inv, def.slot()));
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
        processIconMapping(inv, CREATE_ICONS, icons::setIcon);
        processIconMapping(inv, STATUS_ICONS, icons::setIcon);
    }

    /**
     * A generic helper method that iterates over a map of Enum-to-{@link IconDef}, extracts the {@link ItemStack} at each
     * icon's slot, and sets the corresponding icon.
     *
     * @param inventory The inventory to get items from
     * @param iconDefs A map from an Enum constant to its {@link IconDef}
     * @param setter Stores the chosen icon material for an enum constant
     * @param <T> The type of the Enum (e.g., {@link BuildWorldType}, {@link BuildWorldStatus})
     */
    private <T extends Enum<T>> void processIconMapping(
            Inventory inventory, Map<T, IconDef> iconDefs, BiConsumer<T, XMaterial> setter) {
        iconDefs.forEach((enumConstant, def) -> {
            XMaterial material = Optional.ofNullable(inventory.getItem(def.slot()))
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
