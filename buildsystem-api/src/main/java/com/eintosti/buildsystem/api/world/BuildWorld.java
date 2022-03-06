/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * @author einTosti
 */
public interface BuildWorld {

    /**
     * Gets the name of the world.
     *
     * @return The world's name
     */
    String getName();

    /**
     * Sets the name of the world.
     *
     * @param name The name to set to
     */
    void setName(String name);

    /**
     * Gets the name of the player who created the world.
     * <p>
     * In older versions of the plugin, the creator was not saved which is why {@code null} can be returned.
     *
     * @return The name of the player who created the world
     */
    @Nullable
    String getCreatorName();

    /**
     * Sets the name of the creator.
     *
     * @param name The name of the creator
     */
    void setCreatorName(String name);

    /**
     * Gets the unique-id of the player who created the world.
     * <p>
     * In older versions of the plugin, the creator was not saved which is why {@code null} can be returned.
     *
     * @return The unique-id of the player who created the world
     */
    @Nullable
    UUID getCreatorId();

    /**
     * Sets the unique-id of the creator.
     *
     * @param id The unique-id of the creator
     */
    void setCreatorId(UUID id);

    /**
     * Gets world's type.
     *
     * @return The {@link WorldType} of the world
     */
    WorldType getType();

    /**
     * Gets whether the world is a private world.
     * <p>
     * By default, private worlds cannot be modified by any player except for the creator.
     *
     * @return {@code true} if the world's visibility is set to private, otherwise {@code false}
     */
    boolean isPrivate();

    /**
     * Sets the world's visibility.
     *
     * @param privateWorld {@code true} to make the world private, {@code false} to make the world public
     */
    void setPrivate(boolean privateWorld);

    /**
     * Gets the material which represents the world in the navigator.
     *
     * @return The material which represents the world
     */
    Material getMaterial();

    /**
     * Sets the material which represents the world in the navigator.
     *
     * @param material The material
     */
    void setMaterial(Material material);

    /**
     * Gets the world's current status.
     *
     * @return The world's status
     */
    WorldStatus getStatus();

    /**
     * Sets the world's current status
     *
     * @param worldStatus The status to switch to
     */
    void setStatus(WorldStatus worldStatus);

    /**
     * Gets the short descriptive text which describes what the world is about.
     *
     * @return The world's current project
     */
    String getProject();

    /**
     * Sets the world's short descriptive text which describes what the world is about.
     *
     * @param project The world's project
     */
    void setProject(String project);

    /**
     * Gets the permission which is required to view the world in the navigator and to enter it.
     *
     * @return The required permission
     */
    String getPermission();

    /**
     * Sets the permission which is required to view the world in the navigator and to enter it.
     *
     * @param permission The required permission
     */
    void setPermission(String permission);

    /**
     * Gets the creation date of the world.
     *
     * @return The amount of milliseconds that have passed since {@code January 1, 1970 UTC}, until the world was created.
     */
    long getCreationDate();

    /**
     * Gets the chunk generator used to generate the world.
     *
     * @return The chunk generator used to generate the world.
     */
    ChunkGenerator getChunkGenerator();

    /**
     * Gets whether block physics are activated in the world.
     *
     * @return {@code true} if world physics are currently enabled, otherwise {@code false}
     */
    boolean isPhysics();

    /**
     * Sets whether block physics are activated in the world.
     *
     * @param physics {@code true} to make activate block physics, {@code false} to disable
     */
    void setPhysics(boolean physics);

    /**
     * Gets whether explosions are enabled in the world.
     *
     * @return {@code true} if explosions are currently enabled, otherwise {@code false}
     */
    boolean isExplosions();

    /**
     * Sets whether explosions are enabled in the world.
     *
     * @param explosions {@code true} to enable explosions, {@code false} to disable
     */
    void setExplosions(boolean explosions);

    /**
     * Gets whether mobs have their AI enabled in the world.
     *
     * @return {@code true} if all mob AIs are enabled, otherwise {@code false}
     */
    boolean isMobAI();

    /**
     * Sets whether mobs have their AI enabled in the world.
     *
     * @param mobAI {@code true} to enable mob AIs, {@code false} to disable
     */
    void setMobAI(boolean mobAI);

    /**
     * Gets whether the world has a custom spawn set.
     *
     * @return {@code true} if the world has a custom spawn set, {@code false} otherwise
     */
    boolean hasCustomSpawn();

    /**
     * Get the location where a player spawns in the world.
     *
     * @return The spawn location
     */
    @Nullable
    Location getCustomSpawn();

    /**
     * Sets the location where a player spawns in the world.
     *
     * @param location The location object
     */
    void setCustomSpawn(Location location);

    /**
     * Remove the world's custom spawn.
     */
    void removeCustomSpawn();

    /**
     * Get whether blocks can be broken in the world.
     *
     * @return {@code true} if blocks can currently be broken, otherwise {@code false}
     */
    boolean isBlockBreaking();

    /**
     * Set whether blocks can be broken in the world.
     *
     * @param blockBreaking {@code true} to enable block breaking, {@code false} to disable
     */
    void setBlockBreaking(boolean blockBreaking);

    /**
     * Get whether blocks can be placed in the world.
     *
     * @return {@code true} if blocks can currently be placed, otherwise {@code false}
     */
    boolean isBlockPlacement();

    /**
     * Set whether blocks can be placed in the world.
     *
     * @param blockPlacement {@code true} to enable block placement, {@code false} to disable
     */
    void setBlockPlacement(boolean blockPlacement);

    /**
     * Get whether block can be interacted with in the world.
     *
     * @return {@code true} if blocks can be interacted with, otherwise {@code false}
     */
    boolean isBlockInteractions();

    /**
     * Set whether blocks can be interacted with in the world.
     *
     * @param blockInteractions {@code true} to enable block interactions, {@code false} to disable
     */
    void setBlockInteractions(boolean blockInteractions);

    /**
     * If enabled, only {@link Builder}s can break and place blocks in the world.
     *
     * @return {@code true} if builders-mode is currently enabled, otherwise {@code false}
     */
    boolean isBuildersEnabled();

    /**
     * Set whether on {@link Builder}s can modify the world.
     *
     * @param buildersEnabled {@code true} to disable world modification for all players who are not builders, {@code false} to enable
     */
    void setBuildersEnabled(boolean buildersEnabled);

    /**
     * Get a list of all builders who can modify the world.
     *
     * @return the list of all builders
     */
    List<Builder> getBuilders();

    /**
     * Get a builder by the given uuid.
     *
     * @param uuid The player's unique-id
     * @return The builder object, if any, or {@code null}
     */
    @Nullable
    Builder getBuilder(UUID uuid);

    /**
     * Get a builder by the given player object.
     *
     * @param player The player object
     * @return The builder object, if any, or {@code null}
     * @see BuildWorld#getBuilder(UUID)
     */
    @Nullable
    Builder getBuilder(Player player);

    /**
     * Get whether the given uuid matches that of an added builder.
     *
     * @param uuid The unique-id of the player to be checked
     * @return Whether the player is a builder
     */
    boolean isBuilder(UUID uuid);

    /**
     * Get whether the given player has been added as a {@link Builder}.
     *
     * @param player The player to be checked
     * @return Whether the {@link Player} is a builder
     * @see BuildWorld#isBuilder(UUID)
     */
    boolean isBuilder(Player player);

    /**
     * Create a {@link Builder} with given unique-id and name and add said builder to the list of builders.
     *
     * @param uuid The unique-id of the builder
     * @param name The name of the builder
     */
    void addBuilder(UUID uuid, String name);

    /**
     * Add a player to the list of builders.
     *
     * @param player The player to be added
     * @see BuildWorld#addBuilder(UUID, String)
     */
    void addBuilder(Player player);

    /**
     * Remove a builder from the list of builders.
     *
     * @param builder The builder to remove
     */
    void removeBuilder(Builder builder);

    /**
     * Get the {@link Builder} by the given unique-id and remove said builder from the list of builders.
     *
     * @param uuid The unique-id of the builder to remove
     * @see BuildWorld#removeBuilder(Builder)
     */
    void removeBuilder(UUID uuid);

    /**
     * Get the {@link Builder} by the given player object and remove said builder from the list of builders.
     *
     * @param player The player (builder) to remove
     */
    void removeBuilder(Player player);

    /**
     * Get whether the world has been loaded, allowing a player to enter it.
     *
     * @return {@code true} if the world is loaded, otherwise {@code false}
     */
    boolean isLoaded();

    void forceUnload();

    void unload();

    void load();
}