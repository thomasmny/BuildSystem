/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.navigator.inventory;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.navigator.settings.WorldDisplay;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.world.creation.CreateInventory;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class FolderContentInventory extends DisplayablesInventory {

    private final Folder folder;
    private final DisplayablesInventory parentInventory;

    public FolderContentInventory(
            @NotNull BuildSystemPlugin plugin,
            @NotNull Player player,
            @NotNull NavigatorCategory category,
            @NotNull Folder folder,
            @NotNull DisplayablesInventory parentInventory,
            @NotNull Visibility requiredVisibility,
            @NotNull Set<@NotNull BuildWorldStatus> validStatuses
    ) {
        super(
                plugin,
                player,
                category,
                Messages.getString("folder_title", player, new SimpleEntry<>("%folder%", folder.getName())),
                null,
                requiredVisibility,
                validStatuses
        );
        this.folder = folder;
        this.parentInventory = parentInventory;
    }

    @Override
    protected @NotNull Inventory createBaseInventoryPage(String inventoryTitle) {
        return this.parentInventory.createBaseInventoryPage(inventoryTitle);
    }

    @Override
    protected @NotNull List<Displayable> collectDisplayables() {
        WorldDisplay worldDisplay = this.settingsManager.getSettings(this.player).getWorldDisplay();

        Collection<Folder> folders = collectFolders();
        Collection<BuildWorld> buildWorlds = filterWorlds(collectWorlds(), worldDisplay);

        List<Displayable> displayables = new ArrayList<>();
        displayables.addAll(folders);
        displayables.addAll(buildWorlds);
        displayables.sort(createDisplayOrderComparator(worldDisplay.getWorldSort()));
        return displayables;
    }

    @Override
    protected Collection<Folder> collectFolders() {
        return folderStorage.getFolders().stream()
                .filter(folder -> folder.getCategory() == this.category)
                .filter(folder -> Objects.equals(folder.getParent(), this.folder))
                .filter(folder -> folder.canView(this.player))
                .collect(Collectors.toList());
    }

    @Override
    protected Collection<BuildWorld> collectWorlds() {
        return this.folder.getWorldUUIDs().stream()
                .map(this.worldStorage::getBuildWorld)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    protected void beginWorldCreation() {
        plugin.getCreateInventory().openInventory(this.player, CreateInventory.Page.PREDEFINED, this.requiredVisibility, this.folder);
    }

    @Override
    protected Folder createFolder(String folderName) {
        return this.folderStorage.createFolder(folderName, this.category, this.folder, Builder.of(this.player));
    }

    @Override
    protected void returnToPreviousInventory() {
        this.parentInventory.openInventory();
    }
}
