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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * A reusable 16-colour picker: one dye per legacy Minecraft colour code. Picking a swatch hands the chosen legacy
 * colour token (e.g. {@code "&a"}) to the supplied callback; clicking anywhere else returns to the previous menu.
 */
@NullMarked
public class DyePickerMenu extends ButtonMenu<MenuButton> {

    /**
     * A selectable colour: its legacy token, the concrete swatch that represents it (the most saturated solid-colour
     * block), and the colour word shown on the item.
     *
     * @param token The legacy colour code (e.g. {@code "&a"})
     * @param swatch The dye material used as the swatch icon
     * @param label The colour's name, rendered in its own colour
     */
    private record Swatch(String token, XMaterial swatch, String label) {}

    private static final List<Swatch> SWATCHES = List.of(
            new Swatch("&0", XMaterial.BLACK_DYE, "Black"),
            new Swatch("&1", XMaterial.BLUE_DYE, "Dark Blue"),
            new Swatch("&2", XMaterial.GREEN_DYE, "Dark Green"),
            new Swatch("&3", XMaterial.CYAN_DYE, "Dark Aqua"),
            new Swatch("&4", XMaterial.RED_DYE, "Dark Red"),
            new Swatch("&5", XMaterial.PURPLE_DYE, "Dark Purple"),
            new Swatch("&6", XMaterial.ORANGE_DYE, "Gold"),
            new Swatch("&7", XMaterial.LIGHT_GRAY_DYE, "Gray"),
            new Swatch("&8", XMaterial.GRAY_DYE, "Dark Gray"),
            new Swatch("&9", XMaterial.LIGHT_BLUE_DYE, "Blue"),
            new Swatch("&a", XMaterial.LIME_DYE, "Green"),
            new Swatch("&b", XMaterial.CYAN_DYE, "Aqua"),
            new Swatch("&c", XMaterial.RED_DYE, "Red"),
            new Swatch("&d", XMaterial.MAGENTA_DYE, "Light Purple"),
            new Swatch("&e", XMaterial.YELLOW_DYE, "Yellow"),
            new Swatch("&f", XMaterial.WHITE_DYE, "White"));

    // Three rows of swatches, seven per row (the last holds the remaining two), inside the plugin's one-slot border.
    private static final int[] SWATCH_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29
    };

    private final BuildSystemPlugin plugin;
    private final Consumer<String> onPick;
    private final Runnable onBack;

    public DyePickerMenu(BuildSystemPlugin plugin, Player player, Consumer<String> onPick, Runnable onBack) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("setup_color_picker_title", player));
        this.plugin = plugin;
        this.onPick = onPick;
        this.onBack = onBack;

        for (int i = 0; i < SWATCHES.size(); i++) {
            register(SWATCH_SLOTS[i], swatchButton(SWATCHES.get(i)));
        }
    }

    private MenuButton swatchButton(Swatch swatch) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(swatch.swatch())
                        .name(ColorAPI.process(swatch.token() + swatch.label()))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    onPick.accept(swatch.token());
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
        if (event.getRawSlot() < 0 || event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        XSound.BLOCK_CHEST_OPEN.play(player);
        onBack.run();
    }
}
