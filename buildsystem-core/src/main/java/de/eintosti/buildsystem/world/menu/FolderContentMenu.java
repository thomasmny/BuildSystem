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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.WorldDisplay;
import de.eintosti.buildsystem.world.display.DisplayOrdering;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FolderContentMenu extends DisplayablesMenu {

    private final Folder folder;
    private final DisplayablesMenu parentInventory;

    public FolderContentMenu(
            BuildSystemPlugin plugin,
            Player player,
            NavigatorCategory category,
            Folder folder,
            DisplayablesMenu parentInventory,
            Visibility requiredVisibility,
            Set<BuildWorldStatus> validStatuses) {
        super(
                plugin,
                player,
                category,
                plugin.getMessages().getString("folder_title", player, new SimpleEntry<>("%folder%", folder.getName())),
                null,
                requiredVisibility,
                validStatuses);
        this.folder = folder;
        this.parentInventory = parentInventory;
    }

    @Override
    protected void addExtraItems(Inventory inventory, Player player) {
        parentInventory.addExtraItems(inventory, player);
    }

    @Override
    protected List<Displayable> collectDisplayables() {
        WorldDisplay worldDisplay =
                this.settingsManager.getSettings(this.player).getWorldDisplay();

        Collection<Folder> folders = collectFolders();
        Collection<BuildWorld> buildWorlds = filterWorlds(collectWorlds(), worldDisplay);

        List<Displayable> displayables = new ArrayList<>();
        displayables.addAll(folders);
        displayables.addAll(buildWorlds);
        displayables.sort(
                DisplayOrdering.withPriorities(worldDisplay.getWorldSort().getComparator()));
        return displayables;
    }

    @Override
    @Unmodifiable
    protected Collection<Folder> collectFolders() {
        return this.folder.getSubFolders().stream()
                .filter(folder -> folder.canView(this.player))
                .toList();
    }

    @Override
    @Unmodifiable
    protected Collection<BuildWorld> collectWorlds() {
        return this.folder.getWorldUUIDs().stream()
                .map(this.worldStorage::getBuildWorld)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    protected void beginWorldCreation() {
        new CreateMenu(plugin, CreateMenu.Page.PREDEFINED, this.requiredVisibility, this.folder, this.player)
                .open(this.player);
    }

    @Override
    protected Folder createFolder(String folderName) {
        return this.folderStorage.createFolder(folderName, this.category, this.folder, Builder.of(this.player));
    }

    @Override
    protected void returnToPreviousInventory() {
        this.parentInventory.open(this.player);
    }
}
