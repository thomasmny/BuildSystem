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
import de.eintosti.buildsystem.world.builder.BuildersImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.util.WorldLoaderImpl;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import de.eintosti.buildsystem.world.util.WorldTeleporterImpl;
import de.eintosti.buildsystem.world.util.WorldUnloaderImpl;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class BuildWorldImpl implements BuildWorld {

    private static final BuildSystemPlugin PLUGIN = JavaPlugin.getPlugin(BuildSystemPlugin.class);

    private String name;
    private boolean loaded;

    private final BuildWorldType worldType;
    private final WorldDataImpl worldData;
    private final BuildersImpl builders;
    private final CustomGenerator customGenerator;
    private final long creationDate;

    private final WorldLoaderImpl worldLoader;
    private final WorldUnloaderImpl worldUnloader;

//    public BuildWorldImpl(
//            String name,
//            Builder creator,
//            BuildWorldType worldType,
//            long creationDate,
//            boolean privateWorld,
//            CustomGenerator customGenerator,
//            List<Builder> builders
//    ) {
//        BuildSystemPlugin plugin = JavaPlugin.getPlugin(BuildSystemPlugin.class);
//
//        this.name = name;
//        this.worldType = worldType;
//        this.worldData = new WorldDataImpl(name, plugin.getConfigValues(), privateWorld);
//        this.creationDate = creationDate;
//        this.customGenerator = customGenerator;
//        this.builders = new BuildersImpl(creator, builders);
//
//        this.worldLoader = WorldLoaderImpl.of(this);
//        this.worldUnloader = WorldUnloaderImpl.of(this);
//        this.worldUnloader.manageUnload();
//    }

    public BuildWorldImpl(
            String name,
            Builder creator,
            BuildWorldType worldType,
            long creationDate,
            boolean privateWorld,
            CustomGenerator customGenerator
    ) {
        this(
                name,
                worldType,
                new WorldDataImpl(
                        name,
                        privateWorld,
                        privateWorld ? XMaterial.PLAYER_HEAD : PLUGIN.getCustomizableIcons().getIcon(worldType),
                        PLUGIN.getConfigValues()
                ),
                creator,
                new ArrayList<>(),
                creationDate,
                customGenerator
        );
    }

    public BuildWorldImpl(
            String name,
            BuildWorldType worldType,
            WorldDataImpl worldData,
            Builder creator,
            List<Builder> builders,
            long creationDate,
            CustomGenerator customGenerator
    ) {
        this.name = name;
        this.worldType = worldType;
        this.worldData = worldData;
        this.builders = new BuildersImpl(creator, builders);
        this.creationDate = creationDate;
        this.customGenerator = customGenerator;

        this.worldLoader = WorldLoaderImpl.of(this);
        this.worldUnloader = WorldUnloaderImpl.of(this);
        this.worldUnloader.manageUnload();
    }

    /**
     * Gets the Bukkit {@link World} associated with this {@link BuildWorldImpl}.
     *
     * @return The Bukkit world, or {@code null} if not loaded
     */
    @Nullable
    public World getWorld() {
        return Bukkit.getWorld(name);
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
    public XMaterial getMaterial() {
        return this.worldData.material().get();
    }

    @Override
    public String getDisplayName(Player player) {
        return Messages.getString("world_item_title", player,
                new AbstractMap.SimpleEntry<>("%world%", this.name)
        );
    }

    @Override
    public List<String> getLore(Player player) {
        @SuppressWarnings("unchecked")
        Map.Entry<String, Object>[] placeholders = new Map.Entry[]{
                new AbstractMap.SimpleEntry<>("%status%", Messages.getString(worldData.status().get().getMessageKey(), player)),
                new AbstractMap.SimpleEntry<>("%project%", worldData.project().get()),
                new AbstractMap.SimpleEntry<>("%permission%", worldData.permission().get()),
                new AbstractMap.SimpleEntry<>("%creator%", builders.hasCreator() ? builders.getCreator().getName() : "-"),
                new AbstractMap.SimpleEntry<>("%creation%", Messages.formatDate(getCreationDate())),
                new AbstractMap.SimpleEntry<>("%lastedited%", Messages.formatDate(worldData.lastEdited().get())),
                new AbstractMap.SimpleEntry<>("%lastloaded%", Messages.formatDate(worldData.lastLoaded().get())),
                new AbstractMap.SimpleEntry<>("%lastunloaded%", Messages.formatDate(worldData.lastUnloaded().get()))
        };

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
            lore.add(line.replace("%builders%", builderLines.get(0).trim()));

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
    public ItemStack asItemStack(Player player) {
        return getMaterial().parseItem();
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
    public long getCreationDate() {
        return creationDate;
    }

    @Override
    @Nullable
    public CustomGenerator getCustomGenerator() {
        return customGenerator;
    }

    @Override
    public void cycleDifficulty() {
        switch (worldData.difficulty().get()) {
            case PEACEFUL:
                worldData.difficulty().set(Difficulty.EASY);
                break;
            case EASY:
                worldData.difficulty().set(Difficulty.NORMAL);
                break;
            case NORMAL:
                worldData.difficulty().set(Difficulty.HARD);
                break;
            case HARD:
                worldData.difficulty().set(Difficulty.PEACEFUL);
                break;
        }
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