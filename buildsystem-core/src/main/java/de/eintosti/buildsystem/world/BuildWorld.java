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
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.world.builder.Builder;
import de.eintosti.buildsystem.world.builder.BuilderManager;
import de.eintosti.buildsystem.world.data.WorldData;
import de.eintosti.buildsystem.world.data.WorldType;
import de.eintosti.buildsystem.world.display.Displayable;
import de.eintosti.buildsystem.world.generator.CustomGenerator;
import de.eintosti.buildsystem.world.util.WorldLoader;
import de.eintosti.buildsystem.world.util.WorldUnloader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a world in the build system. This class is the core model for world management, delegating specific functionality to specialized components.
 */
public final class BuildWorld implements Displayable {

    private final WorldData worldData;
    private final WorldType worldType;
    private final BuilderManager builderManager;
    private final CustomGenerator customGenerator;
    private final long creationDate;

    private final WorldLoader worldLoader;
    private final WorldUnloader worldUnloader;

    private String name;
    private boolean loaded;

    public BuildWorld(
            String name,
            Builder creator,
            WorldType worldType,
            WorldData worldData,
            long creationDate,
            CustomGenerator customGenerator,
            List<Builder> builders
    ) {
        this.name = name;
        this.worldType = worldType;
        this.worldData = worldData;
        this.creationDate = creationDate;
        this.customGenerator = customGenerator;

        this.builderManager = new BuilderManager(creator);
        builders.forEach(this.builderManager::addBuilder);

        this.worldLoader = new WorldLoader(this);
        this.worldUnloader = new WorldUnloader(this);

        manageUnload();
    }

    public BuildWorld(String name, WorldData worldData) {
        this(name, null, WorldType.NORMAL, worldData, System.currentTimeMillis(), null, Collections.emptyList());
    }

    /**
     * Gets the Bukkit world associated with this build world.
     *
     * @return The Bukkit world, or null if not loaded
     */
    @Nullable
    public World getWorld() {
        return Bukkit.getWorld(name);
    }

    /**
     * Set the name of the world.
     *
     * @param name The name to set to
     */
    public void setName(String name) {
        this.name = name;
        this.worldData.setWorldName(name);
    }

    public Profileable asProfilable() {
        return hasCreator()
                ? Profileable.of(builderManager.getCreator().getUniqueId())
                : Profileable.username(name);
    }

    /**
     * Gets whether the world has a creator
     *
     * @return {@code true} if the world has a creator, {@code false} otherwise
     */
    public boolean hasCreator() {
        return builderManager.hasCreator();
    }

    /**
     * Get the name of the player who created the world.
     * <p>
     * In older versions of the plugin, the creator was not saved which is why {@code null} can be returned.
     *
     * @return The name of the player who created the world
     */
    @Nullable
    public Builder getCreator() {
        return builderManager.getCreator();
    }

    /**
     * Get world's type.
     *
     * @return The {@link WorldType} of the world
     */
    public WorldType getType() {
        return worldType;
    }

    /**
     * Gets the world's data.
     *
     * @return The {@link WorldData} of the world
     */
    public WorldData getData() {
        return worldData;
    }

    /**
     * Get the creation date of the world.
     *
     * @return The amount of milliseconds that have passed since {@code January 1, 1970 UTC}, until the world was created.
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Get the custom chunk generator used to generate the world.
     *
     * @return The custom chunk generator used to generate the world.
     */
    @Nullable
    public CustomGenerator getCustomGenerator() {
        return customGenerator;
    }

    /**
     * Get the display name of a {@link Difficulty}.
     *
     * @param player The player to parse the placeholders against
     * @return the difficulty's display name
     * @see WorldData#difficulty()
     */
    public String getDifficultyName(Player player) {
        switch (worldData.difficulty().get()) {
            case PEACEFUL:
                return Messages.getString("difficulty_peaceful", player);
            case EASY:
                return Messages.getString("difficulty_easy", player);
            case NORMAL:
                return Messages.getString("difficulty_normal", player);
            case HARD:
                return Messages.getString("difficulty_hard", player);
            default:
                return "-";
        }
    }

    /**
     * Cycles to the next {@link Difficulty}.
     */
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

    /**
     * Get a list of all builders who can modify the world.
     *
     * @return the list of all builders
     */
    public List<Builder> getBuilders() {
        return builderManager.getBuilders();
    }


    /**
     * Get a builder by the given uuid.
     *
     * @param uuid The player's unique-id
     * @return The builder object, if any, or {@code null}
     */
    @Nullable
    public Builder getBuilder(UUID uuid) {
        return builderManager.getBuilder(uuid);
    }

    /**
     * Get whether the given uuid matches that of an added builder.
     *
     * @param uuid The unique-id of the player to be checked
     * @return Whether the player is a builder
     */
    public boolean isBuilder(UUID uuid) {
        return builderManager.isBuilder(uuid);
    }

    /**
     * Get whether the given player has been added as a {@link Builder}.
     *
     * @param player The player to be checked
     * @return Whether the {@link Player} is a builder
     * @see BuildWorld#isBuilder(UUID)
     */
    public boolean isBuilder(Player player) {
        return isBuilder(player.getUniqueId());
    }

    /**
     * Add a {@link Builder} to the current list of builders
     *
     * @param builder The builder object
     */
    public void addBuilder(Builder builder) {
        builderManager.addBuilder(builder);
    }

    /**
     * Remove a {@link Builder} from the current list of builders
     *
     * @param builder The builder object
     */
    private void removeBuilder(Builder builder) {
        builderManager.removeBuilder(builder);
    }

    /**
     * Add a {@link Builder} to the current list of builders
     *
     * @param uuid The builder's unique ID
     * @see BuildWorld#removeBuilder(Builder)
     */
    public void removeBuilder(UUID uuid) {
        removeBuilder(getBuilder(uuid));
    }

    /***
     * Save the list of {@link Builder}s in a string which is suitable to be stored.
     *
     * @return The list of builders as a string
     */
    private String saveBuilders() {
        return builderManager.serializeBuilders();
    }

    /**
     * Get the time in the {@link World} linked to the build world.
     *
     * @return The world time
     */
    public String getWorldTime() {
        World bukkitWorld = getWorld();
        if (bukkitWorld == null) {
            return "?";
        }
        return String.valueOf(bukkitWorld.getTime());
    }

    @Override
    public String getName() {
        return this.name;
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
        List<String> lore = new ArrayList<>();
        lore.add(Messages.getString("world_item_creator", player,
                new AbstractMap.SimpleEntry<>("%creator%", hasCreator() ? getCreator().getName() : "N/A"))
        );
        lore.add(Messages.getString("world_item_type", player,
                new AbstractMap.SimpleEntry<>("%type%", getType().getName(player)))
        );
        return lore;
    }

    @Override
    public ItemStack asItemStack(Player player) {
        return getMaterial().parseItem();
    }

    public BuilderManager getBuilderManager() {
        return builderManager;
    }

    /**
     * Get whether the world has been loaded, allowing a player to enter it.
     *
     * @return {@code true} if the world is loaded, otherwise {@code false}
     */
    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void manageUnload() {
        worldUnloader.manageUnload();
    }

    public void resetUnloadTask() {
        worldUnloader.resetUnloadTask();
    }

    public void forceUnload(boolean save) {
        worldUnloader.forceUnload(save);
    }

    public void load(Player player) {
        worldLoader.loadForPlayer(player);
    }

    public void load() {
        worldLoader.load();
    }

    public enum Time {
        SUNRISE, NOON, NIGHT, UNKNOWN
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildWorld that = (BuildWorld) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}