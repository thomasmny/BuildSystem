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
package de.eintosti.buildsystem.player.menu;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DesignMenu extends ButtonMenu<MenuButton> {

    /**
     * The colors selectable in the design menu, keyed by the slot they occupy. {@link LinkedHashMap} keeps insertion
     * order so the rendered layout is stable.
     */
    private static final Map<Integer, ColorEntry> COLOR_SLOTS = buildColorSlots();

    private static Map<Integer, ColorEntry> buildColorSlots() {
        Map<Integer, ColorEntry> slots = new LinkedHashMap<>();
        slots.put(10, new ColorEntry(XMaterial.RED_STAINED_GLASS, "design_red", DesignColor.RED));
        slots.put(11, new ColorEntry(XMaterial.ORANGE_STAINED_GLASS, "design_orange", DesignColor.ORANGE));
        slots.put(12, new ColorEntry(XMaterial.YELLOW_STAINED_GLASS, "design_yellow", DesignColor.YELLOW));
        slots.put(13, new ColorEntry(XMaterial.PINK_STAINED_GLASS, "design_pink", DesignColor.PINK));
        slots.put(14, new ColorEntry(XMaterial.MAGENTA_STAINED_GLASS, "design_magenta", DesignColor.MAGENTA));
        slots.put(15, new ColorEntry(XMaterial.PURPLE_STAINED_GLASS, "design_purple", DesignColor.PURPLE));
        slots.put(16, new ColorEntry(XMaterial.BROWN_STAINED_GLASS, "design_brown", DesignColor.BROWN));
        slots.put(18, new ColorEntry(XMaterial.LIME_STAINED_GLASS, "design_lime", DesignColor.LIME));
        slots.put(19, new ColorEntry(XMaterial.GREEN_STAINED_GLASS, "design_green", DesignColor.GREEN));
        slots.put(20, new ColorEntry(XMaterial.BLUE_STAINED_GLASS, "design_blue", DesignColor.BLUE));
        slots.put(21, new ColorEntry(XMaterial.CYAN_STAINED_GLASS, "design_aqua", DesignColor.CYAN));
        slots.put(22, new ColorEntry(XMaterial.LIGHT_BLUE_STAINED_GLASS, "design_light_blue", DesignColor.LIGHT_BLUE));
        slots.put(23, new ColorEntry(XMaterial.WHITE_STAINED_GLASS, "design_white", DesignColor.WHITE));
        slots.put(24, new ColorEntry(XMaterial.LIGHT_GRAY_STAINED_GLASS, "design_grey", DesignColor.LIGHT_GRAY));
        slots.put(25, new ColorEntry(XMaterial.GRAY_STAINED_GLASS, "design_dark_grey", DesignColor.GRAY));
        slots.put(26, new ColorEntry(XMaterial.BLACK_STAINED_GLASS, "design_black", DesignColor.BLACK));
        return slots;
    }

    private record ColorEntry(XMaterial material, String messageKey, DesignColor color) {}

    private final BuildSystemPlugin plugin;

    public DesignMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 36, plugin.getMessages().getString("design_title", player));
        this.plugin = plugin;

        COLOR_SLOTS.forEach((slot, entry) -> register(slot, colorButton(entry)));
    }

    private MenuButton colorButton(ColorEntry entry) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    Settings settings = plugin.getSettingsService().getSettings(player);
                    boolean selected = settings.getDesignColor() == entry.color();
                    ItemBuilder.of(entry.material())
                            .name((selected ? "§a" : "§7") + messages.getString(entry.messageKey(), player))
                            .glow(selected)
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    plugin.getSettingsService().getSettings(player).setDesignColor(entry.color());
                    new DesignMenu(plugin, player).open(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillRange(player, getInventory(), 0, 9);
        plugin.getMenuItems().fillRange(player, getInventory(), 27, 36);

        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // A click on the border glass returns to the settings menu.
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack != null && itemStack.getType().toString().contains("STAINED_GLASS_PANE")) {
            new SettingsMenu(plugin, player).open(player);
        }
    }
}
