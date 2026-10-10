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
package de.eintosti.buildsystem.upgrade;

import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Everything a running server knows about its stored data, read through the API and the services, as plain strings
 * keyed by a readable path such as {@code world.lobby.data.status}. Two snapshots compare equal when a server would
 * behave the same on both, regardless of how the files order their keys.
 */
final class Snapshot {

    private Snapshot() {}

    static Map<String, String> of(UpgradeServer server) {
        Map<String, String> values = new TreeMap<>();
        BuildSystem api = server.api();
        Services services = server.services();

        for (BuildWorld world : api.getWorldService().getWorldStorage().getBuildWorlds()) {
            String prefix = "world." + world.getName() + ".";
            values.put(prefix + "uuid", world.getUniqueId().toString());
            values.put(prefix + "type", world.getType().name());
            values.put(prefix + "creator", String.valueOf(world.getBuilders().getCreator()));
            values.put(
                    prefix + "builders",
                    sorted(world.getBuilders().getAllBuilders().stream()
                            .map(Builder::toString)
                            .toList()));
            values.put(prefix + "creation", String.valueOf(world.getCreation()));
            values.put(prefix + "generator", String.valueOf(world.getCustomGenerator()));
            values.put(
                    prefix + "folder",
                    world.getFolder() == null ? "null" : world.getFolder().getName());
            WorldData data = world.getData();
            for (WorldDataKey<?> key : dataKeys()) {
                // A loaded world, such as the one holding the spawn, gets new load times on every start.
                if (world.isLoaded() && (key == WorldDataKey.LAST_LOADED || key == WorldDataKey.LAST_UNLOADED)) {
                    continue;
                }
                values.put(prefix + "data." + key.id(), format(data.get(key)));
            }
            for (PhysicsCategory category : PhysicsCategory.values()) {
                values.put(prefix + "data." + category.key().id(), format(data.get(category.key())));
            }
        }

        for (Folder folder : api.getWorldService().getFolderStorage().getFolders()) {
            String prefix = "folder." + folder.getName() + ".";
            values.put(prefix + "uuid", folder.getUniqueId().toString());
            values.put(
                    prefix + "parent",
                    folder.getParent() == null ? "null" : folder.getParent().getName());
            values.put(prefix + "category", folder.getCategory().getId());
            values.put(prefix + "creator", folder.getCreator().toString());
            values.put(prefix + "creation", String.valueOf(folder.getCreation()));
            values.put(prefix + "icon", folder.getIcon().name());
            values.put(prefix + "icon-skull-texture", String.valueOf(folder.getIconSkullTexture()));
            values.put(prefix + "permission", folder.getPermission());
            values.put(prefix + "project", folder.getProject());
            values.put(
                    prefix + "worlds",
                    sorted(folder.getWorldUUIDs().stream().map(UUID::toString).toList()));
        }

        for (BuildWorldStatus status : api.getStatusRegistry().getAll()) {
            String prefix = "status." + status.getId() + ".";
            values.put(prefix + "display-name", status.getDisplayName());
            values.put(prefix + "color", status.getColor());
            values.put(prefix + "icon", status.getIcon().name());
            values.put(prefix + "order", String.valueOf(status.getOrder()));
            values.put(prefix + "building-allowed", String.valueOf(status.isBuildingAllowed()));
            values.put(prefix + "progresses-to", status.getProgressesTo().orElse("null"));
            values.put(prefix + "slot", String.valueOf(status.getSlot()));
            values.put(prefix + "shown", String.valueOf(status.isShown()));
            values.put(prefix + "built-in", String.valueOf(status.isBuiltIn()));
        }

        for (NavigatorCategory category : api.getNavigatorCategoryRegistry().getAll()) {
            String prefix = "category." + category.getId() + ".";
            values.put(prefix + "display-name", category.getDisplayName());
            values.put(prefix + "color", category.getColor());
            values.put(prefix + "icon", category.getIcon().name());
            values.put(prefix + "icon-skull-texture", String.valueOf(category.getIconSkullTexture()));
            values.put(
                    prefix + "visibilities",
                    sorted(category.getVisibilities().stream().map(Enum::name).toList()));
            values.put(prefix + "statuses", String.join(",", category.getStatusIds()));
            values.put(prefix + "slot", String.valueOf(category.getSlot()));
            values.put(prefix + "shown", String.valueOf(category.isShown()));
            values.put(prefix + "built-in", String.valueOf(category.isBuiltIn()));
        }

        for (BuildPlayer player : api.getPlayerService().getPlayerStorage().getBuildPlayers()) {
            String prefix = "player." + player.getUniqueId() + ".";
            Settings settings = player.getSettings();
            values.put(prefix + "navigator-type", settings.getNavigatorType().name());
            values.put(prefix + "glass", settings.getDesignColor().name());
            values.put(
                    prefix + "sort", settings.getWorldDisplay().getWorldSort().name());
            values.put(
                    prefix + "filter-mode",
                    settings.getWorldDisplay().getWorldFilter().getMode().name());
            values.put(
                    prefix + "filter-text",
                    settings.getWorldDisplay().getWorldFilter().getText());
            values.put(prefix + "clear-inventory", String.valueOf(settings.isClearInventory()));
            values.put(prefix + "disable-interact", String.valueOf(settings.isDisableInteract()));
            values.put(prefix + "hide-players", String.valueOf(settings.isHidePlayers()));
            values.put(prefix + "instant-place-signs", String.valueOf(settings.isInstantPlaceSigns()));
            values.put(prefix + "keep-navigator", String.valueOf(settings.isKeepNavigator()));
            values.put(prefix + "night-vision", String.valueOf(settings.isNightVision()));
            values.put(prefix + "no-clip", String.valueOf(settings.isNoClip()));
            values.put(prefix + "place-plants", String.valueOf(settings.isPlacePlants()));
            values.put(prefix + "scoreboard", String.valueOf(settings.isScoreboard()));
            values.put(prefix + "slab-breaking", String.valueOf(settings.isSlabBreaking()));
            values.put(prefix + "spawn-teleport", String.valueOf(settings.isSpawnTeleport()));
            values.put(prefix + "trapdoors", String.valueOf(settings.isOpenTrapDoors()));
            LogoutLocation logout = BuildPlayerImpl.of(player).getLogoutLocation();
            values.put(prefix + "logout", String.valueOf(logout));
        }

        for (BuildWorldType type : BuildWorldType.values()) {
            values.put(
                    "icon." + type.name(),
                    String.valueOf(services.customizableIcons().getIcon(type)));
        }

        YamlConfiguration spawn = YamlConfiguration.loadConfiguration(
                server.dataFolder().resolve("spawn.yml").toFile());
        values.put("spawn", String.valueOf(spawn.getString("spawn")));

        leaves(server.plugin().getConfig(), "config.", values);
        File messages = server.dataFolder().resolve("messages.yml").toFile();
        leaves(YamlConfiguration.loadConfiguration(messages), "message.", values);
        return values;
    }

    /**
     *
     * Every {@link WorldDataKey} constant, so a key added later is compared without editing this class.
     *
     */
    static List<WorldDataKey<?>> dataKeys() {
        List<WorldDataKey<?>> keys = new ArrayList<>();
        for (Field field : WorldDataKey.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == WorldDataKey.class) {
                try {
                    keys.add((WorldDataKey<?>) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return keys;
    }

    private static void leaves(ConfigurationSection section, String prefix, Map<String, String> values) {
        for (String key : section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                values.put(prefix + key, String.valueOf(section.get(key)));
            }
        }
    }

    private static String format(Object value) {
        if (value instanceof BuildWorldStatus status) {
            return status.getId();
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        return String.valueOf(value);
    }

    private static String sorted(List<String> values) {
        return String.join(";", new TreeSet<>(values));
    }
}
