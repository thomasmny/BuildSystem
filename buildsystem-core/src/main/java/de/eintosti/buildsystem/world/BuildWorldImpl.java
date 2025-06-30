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
package de.eintosti.buildsystem.world;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import de.eintosti.buildsystem.api.world.util.WorldTeleporter;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.world.builder.BuildersImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.util.WorldLoaderImpl;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import de.eintosti.buildsystem.world.util.WorldTeleporterImpl;
import de.eintosti.buildsystem.world.util.WorldUnloaderImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BuildWorldImpl implements BuildWorld {

    private static final BuildSystemPlugin PLUGIN = JavaPlugin.getPlugin(BuildSystemPlugin.class);

    private final UUID uuid;
    private String name;
    private boolean loaded;

    private final BuildWorldType worldType;
    private final WorldDataImpl worldData;
    private final BuildersImpl builders;
    @Nullable
    private final CustomGenerator customGenerator;
    private final long creation;

    private final WorldLoaderImpl worldLoader;
    private final WorldUnloaderImpl worldUnloader;

    public BuildWorldImpl(
            String name,
            Builder creator,
            BuildWorldType worldType,
            long creation,
            boolean privateWorld,
            @Nullable CustomGenerator customGenerator
    ) {
        this(
                UUID.randomUUID(),
                name,
                worldType,
                new WorldDataImpl(
                        name,
                        privateWorld,
                        privateWorld ? XMaterial.PLAYER_HEAD : PLUGIN.getCustomizableIcons().getIcon(worldType)
                ),
                creator,
                new ArrayList<>(),
                creation,
                customGenerator
        );
    }

    public BuildWorldImpl(
            UUID uuid,
            String name,
            BuildWorldType worldType,
            WorldDataImpl worldData,
            @Nullable Builder creator,
            List<Builder> builders,
            long creation,
            @Nullable CustomGenerator customGenerator
    ) {
        this.uuid = uuid;
        this.name = name;
        this.worldType = worldType;
        this.worldData = worldData;
        this.builders = new BuildersImpl(creator, builders);
        this.creation = creation;
        this.customGenerator = customGenerator;

        this.worldLoader = WorldLoaderImpl.of(this);
        this.worldUnloader = WorldUnloaderImpl.of(this);
        this.worldUnloader.manageUnload();
    }

    /**
     * Gets the Bukkit {@link World} associated with this {@link BuildWorld}.
     *
     * @return The Bukkit world, or {@code null} if not loaded
     */
    @Nullable
    public World getWorld() {
        return Bukkit.getWorld(name);
    }

    @Override
    public UUID getUniqueId() {
        return this.uuid;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        this.worldData.setWorldName(name);
    }

    @Override
    public XMaterial getIcon() {
        return this.worldData.material().get();
    }

    @Override
    public void setIcon(XMaterial material) {
        this.worldData.material().set(material);
    }

    @Override
    public String getDisplayName(Player player) {
        return Messages.getString("world_item_title", player,
                Map.entry("%world%", this.name)
        );
    }

    @Override
    public List<String> getLore(Player player) {
        @SuppressWarnings("unchecked")
        Map.Entry<String, Object>[] placeholders = List.of(
                Map.entry("%status%", Messages.getString(Messages.getMessageKey(worldData.status().get()), player)),
                Map.entry("%project%", worldData.project().get()),
                Map.entry("%permission%", worldData.permission().get()),
                Map.entry("%creator%", builders.hasCreator() ? builders.getCreator().getName() : "-"),
                Map.entry("%creation%", Messages.formatDate(getCreation())),
                Map.entry("%lastedited%", Messages.formatDate(worldData.lastEdited().get())),
                Map.entry("%lastloaded%", Messages.formatDate(worldData.lastLoaded().get())),
                Map.entry("%lastunloaded%", Messages.formatDate(worldData.lastUnloaded().get()))
        ).toArray(Map.Entry[]::new);

        List<String> messageList = getPermissions().canPerformCommand(player, WorldsTabComplete.WorldsArgument.EDIT.getPermission())
                ? Messages.getStringList("world_item_lore_edit", player, placeholders)
                : Messages.getStringList("world_item_lore_normal", player, placeholders);

        List<String> lore = new ArrayList<>();

        // Replace %builders% placeholder
        for (String line : messageList) {
            if (!line.contains("%builders%")) {
                lore.add(line);
                continue;
            }

            List<String> builderLines = builders.formatBuildersForLore(player);
            if (builderLines.isEmpty()) {
                continue;
            }

            // Replace the placeholder in the first line only
            lore.add(line.replace("%builders%", builderLines.getFirst().trim()));

            // Add any additional lines
            for (int i = 1; i < builderLines.size(); i++) {
                String builderLine = builderLines.get(i).trim();
                if (!builderLine.isEmpty()) {
                    lore.add(builderLine);
                }
            }
        }

        return lore;
    }

    @Override
    public void addToInventory(Inventory inventory, int slot, Player player) {
        if (getIcon() == XMaterial.PLAYER_HEAD) {
            InventoryUtils.addWorldItem(inventory, slot, this, getDisplayName(player), getLore(player));
            return;
        }
        BuildWorld.super.addToInventory(inventory, slot, player);
    }

    @Override
    public Profileable asProfilable() {
        return builders.hasCreator()
                ? Profileable.of(builders.getCreator().getUniqueId())
                : Profileable.username(name);
    }

    @Override
    public BuildWorldType getType() {
        return worldType;
    }

    @Override
    public WorldData getData() {
        return worldData;
    }

    @Override
    public long getCreation() {
        return creation;
    }

    @Override
    @Nullable
    public CustomGenerator getCustomGenerator() {
        return customGenerator;
    }

    @Override
    public Difficulty cycleDifficulty() {
        Difficulty newDifficulty = switch (worldData.difficulty().get()) {
            case PEACEFUL -> Difficulty.EASY;
            case EASY -> Difficulty.NORMAL;
            case NORMAL -> Difficulty.HARD;
            case HARD -> Difficulty.PEACEFUL;
        };
        worldData.difficulty().set(newDifficulty);
        return newDifficulty;
    }

    @Override
    public Builders getBuilders() {
        return builders;
    }

    @Override
    public String getWorldTime() {
        World bukkitWorld = getWorld();
        if (bukkitWorld == null) {
            return "?";
        }
        return String.valueOf(bukkitWorld.getTime());
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    @Override
    public WorldLoaderImpl getLoader() {
        return worldLoader;
    }

    @Override
    public WorldUnloaderImpl getUnloader() {
        return worldUnloader;
    }

    @Override
    public WorldTeleporter getTeleporter() {
        return WorldTeleporterImpl.of(this);
    }

    @Override
    public WorldPermissions getPermissions() {
        return WorldPermissionsImpl.of(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildWorldImpl that = (BuildWorldImpl) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}