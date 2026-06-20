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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.menu.SetupMenu;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Editor for the default {@link BuildWorldType} icons new worlds use. Each type is a button; clicking it opens the
 * {@link MaterialPickerMenu} to choose the icon, which is stored immediately.
 */
@NullMarked
public class DefaultIconsMenu extends ButtonMenu<MenuButton> {

    private static final int INVENTORY_SIZE = 27;
    private static final int SLOT_RESET = 4;
    private static final int SLOT_BACK = 18;

    private static final List<WorldTypeLayout> WORLD_LAYOUTS = List.of(
            new WorldTypeLayout(BuildWorldType.NORMAL, 10, "setup_normal_world"),
            new WorldTypeLayout(BuildWorldType.FLAT, 11, "setup_flat_world"),
            new WorldTypeLayout(BuildWorldType.NETHER, 12, "setup_nether_world"),
            new WorldTypeLayout(BuildWorldType.END, 13, "setup_end_world"),
            new WorldTypeLayout(BuildWorldType.VOID, 14, "setup_void_world"),
            new WorldTypeLayout(BuildWorldType.IMPORTED, 16, "setup_imported_world"));

    private final BuildSystemPlugin plugin;
    private final CustomizableIcons icons;

    public DefaultIconsMenu(BuildSystemPlugin plugin, Player player) {
        super(
                plugin.getMessages(),
                INVENTORY_SIZE,
                plugin.getMessages().getString("setup_default_icons_title", player));
        this.plugin = plugin;
        this.icons = plugin.getCustomizableIcons();

        setupButtons();
    }

    private void setupButtons() {
        for (WorldTypeLayout layout : WORLD_LAYOUTS) {
            register(layout.slot(), createTypeButton(layout));
        }
        register(SLOT_RESET, createResetButton());
        register(SLOT_BACK, createBackButton());
    }

    private MenuButton createResetButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(Profileable.detect(SkullTextures.RESET))
                        .name(messages.getString("setup_reset", player))
                        .lore(messages.getStringList("setup_reset_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> new DeletionConfirmMenu(
                                plugin,
                                player,
                                messages.getString("setup_reset", player),
                                messages.getStringList("setup_reset_confirm_lore", player),
                                () -> {
                                    icons.resetToDefaults();
                                    XSound.ENTITY_CHICKEN_EGG.play(player);
                                    new DefaultIconsMenu(plugin, player).open(player);
                                },
                                () -> new DefaultIconsMenu(plugin, player).open(player))
                        .open(player))
                .build();
    }

    private MenuButton createTypeButton(WorldTypeLayout layout) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(icons.getIcon(layout.type()))
                        .name(messages.getString(layout.translationKey(), player))
                        .lore(messages.getStringList("setup_icon_lore", player))
                        .into(inventory, slot))
                .onClick((player, event) -> plugin.getMenus()
                        .openMaterialPicker(
                                player,
                                material -> {
                                    icons.setIcon(layout.type(), material);
                                    this.open(player);
                                },
                                () -> this.open(player)))
                .build();
    }

    private MenuButton createBackButton() {
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

    /**
     * Immutable structure organizing layout mapping data for world type configuration slots.
     */
    private record WorldTypeLayout(BuildWorldType type, int slot, String translationKey) {}
}
