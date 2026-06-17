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
package de.eintosti.buildsystem.world.menu.setup;

import static java.util.Map.entry;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.menu.SetupMenu;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Editor for the default {@link BuildWorldType} icons new worlds use. Each type is a button; clicking it opens the
 * {@link MaterialPickerMenu} to choose the icon, which is stored immediately.
 */
@NullMarked
public class DefaultIconsMenu extends ButtonMenu<MenuButton> {

    private static final Map<BuildWorldType, Integer> TYPE_SLOTS = Map.ofEntries(
            entry(BuildWorldType.NORMAL, 11),
            entry(BuildWorldType.FLAT, 12),
            entry(BuildWorldType.NETHER, 13),
            entry(BuildWorldType.END, 14),
            entry(BuildWorldType.VOID, 15),
            entry(BuildWorldType.IMPORTED, 16));

    private static final Map<BuildWorldType, String> TYPE_NAME_KEYS = Map.ofEntries(
            entry(BuildWorldType.NORMAL, "setup_normal_world"),
            entry(BuildWorldType.FLAT, "setup_flat_world"),
            entry(BuildWorldType.NETHER, "setup_nether_world"),
            entry(BuildWorldType.END, "setup_end_world"),
            entry(BuildWorldType.VOID, "setup_void_world"),
            entry(BuildWorldType.IMPORTED, "setup_imported_world"));

    private static final int SLOT_BACK = 31;

    private final BuildSystemPlugin plugin;
    private final CustomizableIcons icons;

    public DefaultIconsMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 36, plugin.getMessages().getString("setup_default_icons_title", player));
        this.plugin = plugin;
        this.icons = plugin.getCustomizableIcons();

        TYPE_SLOTS.forEach((type, slot) -> register(slot, typeButton(type)));
        register(SLOT_BACK, backButton());
    }

    private MenuButton typeButton(BuildWorldType type) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(icons.getIcon(type))
                        .name(messages.getString(TYPE_NAME_KEYS.get(type), player))
                        .lore(messages.getStringList("setup_icon_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> new MaterialPickerMenu(
                                plugin,
                                player,
                                material -> {
                                    icons.setIcon(type, material);
                                    new DefaultIconsMenu(plugin, player).open(player);
                                },
                                () -> new DefaultIconsMenu(plugin, player).open(player))
                        .open(player))
                .build();
    }

    private MenuButton backButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.BARRIER)
                        .name(messages.getString("setup_back", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    new SetupMenu(plugin, player).open(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillAll(player, getInventory());
        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Filler clicks do nothing; navigation is via the explicit back button.
    }
}
