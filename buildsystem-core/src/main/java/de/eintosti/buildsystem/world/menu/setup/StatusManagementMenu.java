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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Manages the registered {@link BuildWorldStatus statuses}; deletion cascades affected worlds back to the default
 * status (the confirmation shows how many worlds are affected).
 */
@NullMarked
public class StatusManagementMenu extends RegistryManagementMenu<BuildWorldStatus> {

    private final WorldStatusRegistryImpl registry;

    public StatusManagementMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin, player, "setup_statuses_title");
        this.registry = plugin.getWorldStatusRegistry();
    }

    @Override
    protected List<BuildWorldStatus> entries() {
        return List.copyOf(registry.getStatuses());
    }

    @Override
    protected ItemBuilder icon(BuildWorldStatus entry, Player player) {
        return ItemBuilder.of(entry.getIcon());
    }

    @Override
    protected String displayName(BuildWorldStatus entry) {
        return ColorAPI.process(entry.getStyledName());
    }

    @Override
    protected boolean isBuiltIn(BuildWorldStatus entry) {
        return entry.isBuiltIn();
    }

    @Override
    protected void openEditor(Player player, BuildWorldStatus entry) {
        new StatusEditorMenu(plugin, player, entry).open(player);
    }

    @Override
    protected BuildWorldStatus create(String name) {
        return registry.createStatus(name);
    }

    @Override
    protected void performDelete(BuildWorldStatus entry) {
        registry.deleteStatus(entry.getId());
    }

    @Override
    protected void performReset() {
        registry.resetToDefaults();
    }

    @Override
    protected void reopen(Player player) {
        new StatusManagementMenu(plugin, player).open(player);
    }

    @Override
    protected List<String> confirmLore(BuildWorldStatus entry, Player player) {
        int affected = registry.worldsWithStatus(entry.getId()).size();
        return messages.getStringList(
                "setup_status_delete_lore", player, Map.entry("%worlds%", String.valueOf(affected)));
    }

    @Override
    protected String entryLoreKey() {
        return "setup_status_entry_lore";
    }

    @Override
    protected String builtInLoreKey() {
        return "setup_status_entry_builtin_lore";
    }

    @Override
    protected String addNameKey() {
        return "setup_status_add";
    }

    @Override
    protected String addPromptKey() {
        return "setup_status_add_prompt";
    }
}
