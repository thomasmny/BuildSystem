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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

/**
 * @author einTosti
 */
public interface BuildWorld {

    String getName();

    void setName(String name);

    String getCreatorName();

    void setCreatorName(String name);

    UUID getCreatorId();

    void setCreatorId(UUID uuid);

    WorldType getType();

    boolean isPrivate();

    void setPrivate(boolean privateWorld);

    Material getMaterial();

    void setMaterial(Material material);

    WorldStatus getStatus();

    void setStatus(WorldStatus worldStatus);

    String getProject();

    void setProject(String project);

    String getPermission();

    void setPermission(String permission);

    long getCreationDate();

    ChunkGenerator getChunkGenerator();

    boolean isPhysics();

    void setPhysics(boolean physics);

    boolean isExplosions();

    void setExplosions(boolean explosions);

    boolean isMobAI();

    void setMobAI(boolean mobAI);

    String getCustomSpawn();

    void setCustomSpawn(Location location);

    void removeCustomSpawn();

    boolean isBlockBreaking();

    void setBlockBreaking(boolean blockBreaking);

    boolean isBlockPlacement();

    void setBlockPlacement(boolean blockPlacement);

    boolean isBlockInteractions();

    void setBlockInteractions(boolean blockInteractions);

    boolean isBuildersEnabled();

    List<Builder> getBuilders();

    void setBuildersEnabled(boolean buildersEnabled);

    Builder getBuilder(UUID uuid);

    Builder getBuilder(Player player);

    boolean isBuilder(UUID uuid);

    boolean isBuilder(Player player);

    void addBuilder(UUID uuid, String name);

    void addBuilder(Player player);

    void removeBuilder(Builder builder);

    void removeBuilder(UUID uuid);

    void removeBuilder(Player player);

    boolean isLoaded();

    void forceUnload();

    void unload();

    void load();
}
