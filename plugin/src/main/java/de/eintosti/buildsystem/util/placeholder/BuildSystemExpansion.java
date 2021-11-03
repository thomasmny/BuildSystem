/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.util.placeholder;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author einTosti
 */
public class BuildSystemExpansion extends PlaceholderExpansion {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public BuildSystemExpansion(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Because this is an internal class, this check is not needed, and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * For convenience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "buildsystem";
    }

    /**
     * This is the version of the expansion.
     * You don't have to use numbers, since it is set as a String.
     * <p>
     * For convenience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * We specify the value identifier in this method.
     * Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param player     A Player.
     * @param identifier A String containing the identifier/value.
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        String worldName = player.getWorld().getName();
        if (identifier.matches(".*_.*")) {
            String[] splitString = identifier.split("_");
            worldName = splitString[1];
            identifier = splitString[0];
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            return "-";
        }

        switch (identifier.toLowerCase()) {
            case "blockbreaking":
                return String.valueOf(buildWorld.isBlockBreaking());
            case "blockplacement":
                return String.valueOf(buildWorld.isBlockPlacement());
            case "builders":
                return plugin.getBuilders(buildWorld);
            case "buildersenabled":
                return String.valueOf(buildWorld.isBuilders());
            case "creation":
                return plugin.formatDate(buildWorld.getCreationDate());
            case "creator":
                return buildWorld.getCreator();
            case "creatorid":
                return String.valueOf(buildWorld.getCreatorId());
            case "explosions":
                return String.valueOf(buildWorld.isExplosions());
            case "loaded":
                return String.valueOf(buildWorld.isLoaded());
            case "material":
                return String.valueOf(buildWorld.getMaterial().parseMaterial());
            case "mobai":
                return String.valueOf(buildWorld.isMobAI());
            case "permission":
                return buildWorld.getPermission();
            case "private":
                return String.valueOf(buildWorld.isPrivate());
            case "project":
                return buildWorld.getProject();
            case "physics":
                return String.valueOf(buildWorld.isPhysics());
            case "spawn":
                return buildWorld.getCustomSpawn();
            case "status":
                return buildWorld.getStatusName();
            case "time":
                return plugin.getWorldTime(buildWorld);
            case "type":
                return buildWorld.getTypeName();
            case "world":
                return buildWorld.getName();
        }
        return null;
    }
}
