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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FolderContentInventory extends DisplayablesInventory {

    private final Folder folder;
    private final DisplayablesInventory parentInventory;

    public FolderContentInventory(
            @NotNull BuildSystemPlugin plugin,
            @NotNull Player player,
            @NotNull Folder folder,
            @NotNull DisplayablesInventory parentInventory,
            @NotNull Visibility requiredVisibility,
            @NotNull Set<@NotNull BuildWorldStatus> validStatuses
    ) {
        super(
                plugin,
                player,
                Messages.getString("folder_title", player, new SimpleEntry<>("%folder%", folder.getName())),
                null,
                requiredVisibility,
                validStatuses
        );
        this.folder = folder;
        this.parentInventory = parentInventory;
    }

    @Override
    protected @NotNull List<Displayable> collectDisplayables() {
        WorldDisplay worldDisplay = settingsManager.getSettings(player).getWorldDisplay();
        Collection<BuildWorld> buildWorlds = filterWorlds(collectWorlds(), worldDisplay);
        return new ArrayList<>(buildWorlds);
    }

    @Override
    protected Collection<BuildWorld> collectWorlds() {
        return folder.getWorldUUIDs().stream()
                .map(worldStorage::getBuildWorld)
                .collect(Collectors.toList());
    }

    @Override
    protected void returnToPreviousInventory() {
        this.parentInventory.openInventory();
    }
}
