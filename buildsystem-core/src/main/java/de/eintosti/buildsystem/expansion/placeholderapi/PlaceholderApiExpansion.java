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
package de.eintosti.buildsystem.expansion.placeholderapi;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderApiExpansion extends PlaceholderExpansion {

    private static final String SETTINGS_KEY = "settings";

    private final BuildSystemPlugin plugin;
    private final SettingsManager settingsManager;
    private final WorldStorageImpl worldStorage;

    public PlaceholderApiExpansion(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsManager();
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getLogger().info("PlaceholderAPI expansion initialized");
    }

    /**
     * Because this is an internal class, you must override this method to let PlaceholderAPI know to not unregister your expansion class when PlaceholderAPI is reloaded
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
     * The name of the person who created this expansion should go here. For convenience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here. This is what tells PlaceholderAPI to call our onRequest method to get a value if a placeholder starts with our identifier. This
     * must be unique and cannot contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "buildsystem";
    }

    /**
     * This is the version of the expansion. You don't have to use numbers since it is set as a String.
     * <p>
     * For convenience do we return the version from the plugin.yml
     *
     * @return The version as a string.
     */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier is found and needs a value. We specify the value identifier in this method.
     *
     * @param player     The player for which the placeholder is requested.
     * @param identifier The identifier of the placeholder
     * @return The value of the placeholder as a string, or {@code null} if the identifier is not recognized.
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
     * This is the method called when a placeholder with the identifier {@code %buildsystem_settings_<setting>%} is found.
     *
     * @param player     The player for which the placeholder is requested
     * @param identifier The identifier
     * @return The requested setting as a string, or {@code null} if the setting is not recognized
     */
    @Nullable
    private String parseSettingsPlaceholder(Player player, String identifier) {
        Settings settings = settingsManager.getSettings(player);
        String settingIdentifier = identifier.split("_")[1];

        return switch (settingIdentifier.toLowerCase(Locale.ROOT)) {
            case "navigatortype" -> settings.getNavigatorType().toString();
            case "glasscolor" -> settings.getDesignColor().toString();
            case "worldsort" -> settings.getWorldDisplay().getWorldSort().toString();
            case "clearinventory" -> String.valueOf(settings.isClearInventory());
            case "disableinteract" -> String.valueOf(settings.isDisableInteract());
            case "hideplayers" -> String.valueOf(settings.isHidePlayers());
            case "instantplacesigns" -> String.valueOf(settings.isInstantPlaceSigns());
            case "keepnavigator" -> String.valueOf(settings.isKeepNavigator());
            case "nightvision" -> String.valueOf(settings.isNightVision());
            case "noclip" -> String.valueOf(settings.isNoClip());
            case "placeplants" -> String.valueOf(settings.isPlacePlants());
            case "scoreboard" -> String.valueOf(settings.isScoreboard());
            case "slabbreaking" -> String.valueOf(settings.isSlabBreaking());
            case "spawnteleport" -> String.valueOf(settings.isSpawnTeleport());
            case "opentrapdoors" -> String.valueOf(settings.isOpenTrapDoors());
            default -> null;
        };
    }

    /**
     * This is the method called when a placeholder with the identifier needed for {@link PlaceholderApiExpansion#parseSettingsPlaceholder(Player, String)} is not found.
     * <p>
     * The default layout for a world placeholder is {@code %buildsystem_<value>%}. If a world is not specified by using the format {@code %buildsystem_<value>_<world>%} then the
     * world the player is currently in will be used.
     *
     * @param player     The player for which the placeholder is requested
     * @param identifier The identifier
     * @return The requested value as a string, or {@code null} if the identifier is not recognized or the world does not exist
     */
    @Nullable
    private String parseBuildWorldPlaceholder(Player player, String identifier) {
        String worldName = player.getWorld().getName();
        if (identifier.matches(".*_.*")) {
            String[] splitString = identifier.split("_");
            worldName = splitString[1];
            identifier = splitString[0];
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld == null) {
            return "-";
        }

        Builders builders = buildWorld.getBuilders();
        WorldData worldData = buildWorld.getData();
        return switch (identifier.toLowerCase(Locale.ROOT)) {
            case "blockbreaking" -> String.valueOf(worldData.blockBreaking().get());
            case "blockplacement" -> String.valueOf(worldData.blockPlacement().get());
            case "builders" -> builders.asPlaceholder(player);
            case "buildersenabled" -> String.valueOf(worldData.buildersEnabled().get());
            case "creation" -> Messages.formatDate(buildWorld.getCreation());
            case "creator" -> builders.hasCreator() ? builders.getCreator().getName() : "-";
            case "creatorid" -> builders.hasCreator() ? String.valueOf(builders.getCreator().getUniqueId()) : "-";
            case "explosions" -> String.valueOf(worldData.explosions().get());
            case "lastedited" -> Messages.formatDate(worldData.lastEdited().get());
            case "lastloaded" -> Messages.formatDate(worldData.lastLoaded().get());
            case "lastunloaded" -> Messages.formatDate(worldData.lastUnloaded().get());
            case "loaded" -> String.valueOf(buildWorld.isLoaded());
            case "material" -> worldData.material().get().name();
            case "mobai" -> String.valueOf(worldData.mobAi().get());
            case "permission" -> worldData.permission().get();
            case "private" -> String.valueOf(worldData.privateWorld().get());
            case "project" -> worldData.project().get();
            case "physics" -> String.valueOf(worldData.physics().get());
            case "spawn" -> worldData.customSpawn().get();
            case "status" -> Messages.getString(Messages.getMessageKey(worldData.status().get()), player);
            case "time" -> buildWorld.getWorldTime();
            case "type" -> Messages.getString(Messages.getMessageKey(buildWorld.getType()), player);
            case "world" -> buildWorld.getName();
            default -> null;
        };
    }
}