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
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * A reusable picker over every obtainable Minecraft item. Items fill the left eight columns and the view scrolls
 * vertically — one row at a time — via the up/down arrows down the right-hand column, which also holds a name filter.
 * Clicking an item hands the chosen {@link XMaterial} to the supplied callback. Used wherever an icon material is
 * configured (statuses, categories, default world-type icons).
 */
@NullMarked
public class MaterialPickerMenu extends ButtonMenu<MenuButton> {

    private static final int COLUMNS = 8;
    private static final int VISIBLE_ROWS = 6;
    private static final int VISIBLE_ITEMS = COLUMNS * VISIBLE_ROWS;

    private static final int SLOT_SCROLL_UP = 8;
    private static final int SLOT_FILTER = 26;
    private static final int SLOT_BACK = 44;
    private static final int SLOT_SCROLL_DOWN = 53;

    /**
     * Every obtainable item material, sorted by name. Computed once: a material is pickable when it has an item form,
     * is not a legacy alias, and is not air.
     */
    private static final List<XMaterial> PICKABLE = Arrays.stream(Material.values())
            .filter(material -> material.isItem() && !material.isLegacy() && !material.isAir())
            .map(XMaterial::matchXMaterial)
            .distinct()
            .sorted(Comparator.comparing(Enum::name))
            .toList();

    private final BuildSystemPlugin plugin;
    private final Consumer<XMaterial> onPick;
    private final Runnable onBack;

    private String filter = "";
    private int topRow = 0;

    public MaterialPickerMenu(BuildSystemPlugin plugin, Player player, Consumer<XMaterial> onPick, Runnable onBack) {
        super(plugin.getMessages(), 54, plugin.getMessages().getString("setup_item_picker_title", player));
        this.plugin = plugin;
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected void populate(Player player) {
        clearButtons();
        // Clear the grid first: without this, items from a larger previous view (before a filter/scroll) linger in
        // slots that no longer hold a button, making the filter look broken.
        getInventory().clear();
        fillRightColumn(player);

        List<XMaterial> matches = filtered();
        int maxTopRow = Math.max(0, ceilDiv(matches.size(), COLUMNS) - VISIBLE_ROWS);
        topRow = Math.min(topRow, maxTopRow);

        int firstIndex = topRow * COLUMNS;
        for (int i = 0; i < VISIBLE_ITEMS && firstIndex + i < matches.size(); i++) {
            int slot = (i / COLUMNS) * 9 + (i % COLUMNS); // left eight columns, never the right control column
            register(slot, materialButton(matches.get(firstIndex + i)));
        }

        register(SLOT_SCROLL_UP, scrollButton(true, topRow > 0));
        register(SLOT_SCROLL_DOWN, scrollButton(false, topRow < maxTopRow));
        register(SLOT_FILTER, filterButton());
        register(SLOT_BACK, backButton());

        renderButtons(player);
    }

    /**
     * Fills the right-hand control column with filler panes so the scroll/filter/back controls read as a deliberate
     * strip rather than floating in empty slots.
     */
    private void fillRightColumn(Player player) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            plugin.getMenuItems().addGlassPane(player, getInventory(), row * 9 + COLUMNS);
        }
    }

    private List<XMaterial> filtered() {
        if (filter.isEmpty()) {
            return PICKABLE;
        }
        String needle = filter.toLowerCase(Locale.ROOT);
        return PICKABLE.stream()
                .filter(material ->
                        prettyName(material).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    private MenuButton materialButton(XMaterial material) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(material)
                        .name("&b" + prettyName(material))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    onPick.accept(material);
                })
                .build();
    }

    private MenuButton scrollButton(boolean up, boolean enabled) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(
                                Profileable.detect(up ? "MHF_ArrowUp" : "MHF_ArrowDown"))
                        .name(messages.getString(up ? "setup_scroll_up" : "setup_scroll_down", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    if (!enabled) {
                        XSound.ENTITY_ITEM_BREAK.play(player);
                        return;
                    }
                    topRow += up ? -1 : 1;
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    populate(player);
                })
                .build();
    }

    private MenuButton filterButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.HOPPER)
                        .name(messages.getString("setup_filter", player))
                        .lore(messages.getStringList(
                                "setup_filter_lore",
                                player,
                                Map.entry(
                                        "%filter%",
                                        filter.isEmpty() ? messages.getString("setup_filter_none", player) : filter)))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    if (event.isRightClick()) {
                        filter = "";
                        topRow = 0;
                        populate(player);
                        return;
                    }
                    new PlayerChatInput(plugin, player, "setup_filter_prompt", input -> {
                        filter = input.strip();
                        topRow = 0;
                        open(player);
                    });
                })
                .build();
    }

    private MenuButton backButton() {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.BARRIER)
                        .name(messages.getString("setup_back", player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    onBack.run();
                })
                .build();
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    /**
     * Turns an enum-style material name ({@code DIAMOND_PICKAXE}) into a readable label ({@code Diamond Pickaxe}).
     */
    private static String prettyName(XMaterial material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                name.append(Character.toUpperCase(word.charAt(0)))
                        .append(word, 1, word.length())
                        .append(' ');
            }
        }
        return name.toString().trim();
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Filler/empty slots do nothing; navigation is via the explicit controls.
    }
}
