/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.expansion.placeholderapi;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.settings.Settings;
import com.eintosti.buildsystem.settings.SettingsManager;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author einTosti
 */
public class PlaceholderApiExpansion extends PlaceholderExpansion {

    private static final String SETTINGS_KEY = "settings";

    private final BuildSystem plugin;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    public PlaceholderApiExpansion(BuildSystem plugin) {
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsManager();
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

        if (identifier.matches(".*_.*") && identifier.split("_")[0].equalsIgnoreCase(SETTINGS_KEY)) {
            return parseSettingsPlaceholder(player, identifier);
        } else {
            return parseBuildWorldPlaceholder(player, identifier);
        }
    }

    /**
     * This is the method called when a placeholder with the identifier
     * {@code %buildsystem_settings_<setting>%} is found
     *
     * @param player     A Player.
     * @param identifier A String containing the identifier/value.
     * @return possibly-null String of the requested identifier.
     */
    @Nullable
    private String parseSettingsPlaceholder(Player player, String identifier) {
        Settings settings = settingsManager.getSettings(player);
        String settingIdentifier = identifier.split("_")[1];

        switch (settingIdentifier.toLowerCase()) {
            case "navigatortype":
                return settings.getNavigatorType().toString();
            case "glasscolor":
                return settings.getDesignColor().toString();
            case "worldsort":
                return settings.getWorldSort().toString();
            case "clearinventory":
                return String.valueOf(settings.isClearInventory());
            case "disableinteract":
                return String.valueOf(settings.isDisableInteract());
            case "hideplayers":
                return String.valueOf(settings.isHidePlayers());
            case "instantplacesigns":
                return String.valueOf(settings.isInstantPlaceSigns());
            case "keepnavigator":
                return String.valueOf(settings.isKeepNavigator());
            case "nightvision":
                return String.valueOf(settings.isNightVision());
            case "noclip":
                return String.valueOf(settings.isNoClip());
            case "placeplants":
                return String.valueOf(settings.isPlacePlants());
            case "scoreboard":
                return String.valueOf(settings.isScoreboard());
            case "slabbreaking":
                return String.valueOf(settings.isSlabBreaking());
            case "spawnteleport":
                return String.valueOf(settings.isSpawnTeleport());
            case "opentrapdoors":
                return String.valueOf(settings.isTrapDoor());
            default:
                return null;
        }
    }

    /**
     * This is the method called when a placeholder with the identifier needed for
     * {@link PlaceholderApiExpansion#parseSettingsPlaceholder(Player, String)} is not found
     * <p>
     * The default layout for a world placeholder is {@code %buildsystem_<value>%}.
     * If a world is not specified by using the format {@code %buildsystem_<value>_<world>%}
     * then the world the player is currently in will be used.
     *
     * @param player     A Player.
     * @param identifier A String containing the identifier/value.
     * @return possibly-null String of the requested identifier.
     */
    @Nullable
    private String parseBuildWorldPlaceholder(Player player, String identifier) {
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
                return buildWorld.getBuildersInfo();
            case "buildersenabled":
                return String.valueOf(buildWorld.isBuilders());
            case "creation":
                return buildWorld.getFormattedCreationDate();
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
                return buildWorld.getStatus().getName();
            case "time":
                return buildWorld.getWorldTime();
            case "type":
                return buildWorld.getType().getName();
            case "world":
                return buildWorld.getName();
            default:
                return null;
        }
    }
}