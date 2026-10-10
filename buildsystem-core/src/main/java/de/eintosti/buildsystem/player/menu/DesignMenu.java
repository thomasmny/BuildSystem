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

import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DesignMenu extends ButtonMenu {

    /**
     * The colors selectable in the design menu, keyed by the slot they occupy. {@link LinkedHashMap} keeps insertion
     * order so the rendered layout is stable.
     */
    private static final Map<Integer, ColorEntry> COLOR_SLOTS = buildColorSlots();

    private static Map<Integer, ColorEntry> buildColorSlots() {
        Map<Integer, ColorEntry> slots = new LinkedHashMap<>();
        slots.put(10, new ColorEntry("design_red", DesignColor.RED));
        slots.put(11, new ColorEntry("design_orange", DesignColor.ORANGE));
        slots.put(12, new ColorEntry("design_yellow", DesignColor.YELLOW));
        slots.put(13, new ColorEntry("design_pink", DesignColor.PINK));
        slots.put(14, new ColorEntry("design_magenta", DesignColor.MAGENTA));
        slots.put(15, new ColorEntry("design_purple", DesignColor.PURPLE));
        slots.put(16, new ColorEntry("design_brown", DesignColor.BROWN));
        slots.put(18, new ColorEntry("design_lime", DesignColor.LIME));
        slots.put(19, new ColorEntry("design_green", DesignColor.GREEN));
        slots.put(20, new ColorEntry("design_blue", DesignColor.BLUE));
        slots.put(21, new ColorEntry("design_aqua", DesignColor.CYAN));
        slots.put(22, new ColorEntry("design_light_blue", DesignColor.LIGHT_BLUE));
        slots.put(23, new ColorEntry("design_white", DesignColor.WHITE));
        slots.put(24, new ColorEntry("design_grey", DesignColor.LIGHT_GRAY));
        slots.put(25, new ColorEntry("design_dark_grey", DesignColor.GRAY));
        slots.put(26, new ColorEntry("design_black", DesignColor.BLACK));
        return slots;
    }

    private record ColorEntry(String messageKey, DesignColor color) {}

    private final SettingsService settingsService;

    public DesignMenu(MenuContext context, SettingsService settingsService, Player player) {
        super(context, 36, context.messages().getString("design_title", player));
        this.settingsService = settingsService;

        COLOR_SLOTS.forEach((slot, entry) -> register(slot, colorButton(entry)));
    }

    private MenuButton colorButton(ColorEntry entry) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    Settings settings = settingsService.getSettings(player);
                    boolean selected = settings.getDesignColor() == entry.color();
                    ItemBuilder.of(MenuItems.glass(entry.color()))
                            .name((selected ? "§a" : "§7") + messages.getString(entry.messageKey(), player))
                            .glow(selected)
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    settingsService.getSettings(player).setDesignColor(entry.color());
                    menus.openDesign(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        menuItems.fillRange(player, getInventory(), 0, 9);
        menuItems.fillRange(player, getInventory(), 27, 36);

        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // A click on the border glass returns to the settings menu.
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack != null && itemStack.getType().toString().contains("STAINED_GLASS_PANE")) {
            menus.openSettings(player);
        }
    }
}
