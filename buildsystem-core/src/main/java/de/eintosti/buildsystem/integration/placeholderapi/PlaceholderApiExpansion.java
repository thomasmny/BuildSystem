/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.integration.placeholderapi;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlaceholderApiExpansion extends PlaceholderExpansion {

    private static final String SETTINGS_KEY = "settings";

    private final String author;
    private final String version;
    private final SettingsService settingsService;
    private final WorldStorageImpl worldStorage;
    private final Messages messages;

    public PlaceholderApiExpansion(
            BuildSystemPlugin plugin,
            SettingsService settingsService,
            WorldStorageImpl worldStorage,
            Messages messages) {
        this.author = plugin.getDescription().getAuthors().toString();
        this.version = plugin.getDescription().getVersion();
        this.settingsService = settingsService;
        this.worldStorage = worldStorage;
        this.messages = messages;
        plugin.getLogger().info("PlaceholderAPI expansion initialized");
    }

    PlaceholderApiExpansion(
            String author,
            String version,
            SettingsService settingsService,
            WorldStorageImpl worldStorage,
            Messages messages) {
        this.author = author;
        this.version = version;
        this.settingsService = settingsService;
        this.worldStorage = worldStorage;
        this.messages = messages;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public String getIdentifier() {
        return "buildsystem";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.contains("_") && identifier.split("_")[0].equalsIgnoreCase(SETTINGS_KEY)) {
            return settingsPlaceholder(player, identifier);
        } else {
            return worldPlaceholder(player, identifier);
        }
    }

    private @Nullable String settingsPlaceholder(Player player, String identifier) {
        Settings settings = settingsService.getSettings(player);
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

    private @Nullable String worldPlaceholder(Player player, String identifier) {
        String worldName = player.getWorld().getName();
        if (identifier.contains("_")) {
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
            case "blockbreaking" -> String.valueOf(worldData.get(WorldDataKey.BLOCK_BREAKING));
            case "blockplacement" -> String.valueOf(worldData.get(WorldDataKey.BLOCK_PLACEMENT));
            case "builders" -> builders.asPlaceholder(player);
            case "buildersenabled" -> String.valueOf(worldData.get(WorldDataKey.BUILDERS_ENABLED));
            case "creation" -> messages.formatDate(buildWorld.getCreation());
            case "creator" -> builders.hasCreator() ? builders.getCreator().getName() : "-";
            case "creatorid" ->
                builders.hasCreator() ? String.valueOf(builders.getCreator().getUniqueId()) : "-";
            case "explosions" -> String.valueOf(worldData.get(WorldDataKey.EXPLOSIONS));
            case "lastedited" -> messages.formatDate(worldData.get(WorldDataKey.LAST_EDITED));
            case "lastloaded" -> messages.formatDate(worldData.get(WorldDataKey.LAST_LOADED));
            case "lastunloaded" -> messages.formatDate(worldData.get(WorldDataKey.LAST_UNLOADED));
            case "loaded" -> String.valueOf(buildWorld.isLoaded());
            case "material" -> worldData.get(WorldDataKey.MATERIAL).name();
            case "mobai" -> String.valueOf(worldData.get(WorldDataKey.MOB_AI));
            case "permission" -> worldData.get(WorldDataKey.PERMISSION);
            case "private" ->
                String.valueOf(worldData.get(WorldDataKey.VISIBILITY).isPrivate());
            case "project" -> worldData.get(WorldDataKey.PROJECT);
            case "physics" -> String.valueOf(worldData.get(WorldDataKey.PHYSICS));
            case "spawn" -> worldData.get(WorldDataKey.CUSTOM_SPAWN);
            case "status" -> worldData.get(WorldDataKey.STATUS).getDisplayName();
            case "time" -> buildWorld.getWorldTime();
            case "type" -> messages.getString(Messages.getMessageKey(buildWorld.getType()), player);
            case "world" -> buildWorld.getName();
            default -> null;
        };
    }
}
