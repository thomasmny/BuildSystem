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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Manages the registered {@link NavigatorCategory categories}. Deleting a custom category never orphans a status
 * (statuses are shared), so its worlds simply resolve to another matching category on the next render.
 */
@NullMarked
public class CategoryManagementMenu extends RegistryManagementMenu<NavigatorCategory> {

    private final NavigatorCategoryRegistryImpl registry;

    public CategoryManagementMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin, player, "setup_categories_title");
        this.registry = plugin.getNavigatorCategoryRegistry();
    }

    @Override
    protected List<NavigatorCategory> entries() {
        return List.copyOf(registry.getCategories());
    }

    @Override
    protected ItemBuilder icon(NavigatorCategory entry, Player player) {
        return ItemBuilder.icon(entry, player);
    }

    @Override
    protected String displayName(NavigatorCategory entry) {
        return ColorAPI.process(entry.getColor() + entry.getDisplayName());
    }

    @Override
    protected boolean isBuiltIn(NavigatorCategory entry) {
        return entry.isBuiltIn();
    }

    @Override
    protected void openEditor(Player player, NavigatorCategory entry) {
        new CategoryEditorMenu(plugin, player, entry).open(player);
    }

    @Override
    protected NavigatorCategory create(String name) {
        return registry.createCategory(name);
    }

    @Override
    protected void performDelete(NavigatorCategory entry) {
        registry.deleteCategory(entry.getId());
    }

    @Override
    protected void reopen(Player player) {
        new CategoryManagementMenu(plugin, player).open(player);
    }

    @Override
    protected void performReset() {
        registry.resetToDefaults();
    }

    @Override
    protected List<String> confirmLore(NavigatorCategory entry, Player player) {
        return messages.getStringList("setup_category_delete_lore", player);
    }

    @Override
    protected String entryLoreKey() {
        return "setup_category_entry_lore";
    }

    @Override
    protected String builtInLoreKey() {
        return "setup_category_entry_builtin_lore";
    }

    @Override
    protected String addNameKey() {
        return "setup_category_add";
    }

    @Override
    protected String addPromptKey() {
        return "setup_category_add_prompt";
    }
}
