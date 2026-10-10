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
package de.eintosti.buildsystem.world;

import de.eintosti.buildsystem.util.FileUtils;
import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A world's name doubles as its namespaced id: {@code maps:lobby} lives in the {@code maps} namespace, while a name
 * without one belongs to {@code minecraft}. Every world was named that way before namespaces existed, so the
 * {@code minecraft:} prefix is never stored and a bare stored name never depends on the configured default namespace.
 *
 * <p>Only Paper can create a world outside {@code minecraft}. Spigot's {@link WorldCreator} takes a name alone, so the
 * keyed constructor is resolved reflectively.
 */
@NullMarked
public final class WorldNames {

    private static final char SEPARATOR = ':';
    // Stricter than NamespacedKey, which also accepts "." and "..": a namespace is a directory under dimensions/.
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9_-][a-z0-9._-]*");
    private static final @Nullable Constructor<WorldCreator> KEYED_CREATOR = resolveKeyedCreator();

    private WorldNames() {}

    public static String namespace(String worldName) {
        int separator = worldName.indexOf(SEPARATOR);
        return separator < 0
                ? NamespacedKey.MINECRAFT
                : worldName.substring(0, separator).toLowerCase(Locale.ROOT);
    }

    public static String path(String worldName) {
        return worldName.substring(worldName.indexOf(SEPARATOR) + 1);
    }

    public static boolean isNamespaced(String worldName) {
        return !namespace(worldName).equals(NamespacedKey.MINECRAFT);
    }

    /**
     * {@return the case-insensitive {@code namespace:path} two names are compared by}
     */
    public static String id(String worldName) {
        return namespace(worldName) + SEPARATOR + path(worldName).toLowerCase(Locale.ROOT);
    }

    /**
     * {@return the name stored for a world in {@code namespace}} The {@code minecraft} namespace is left out.
     */
    public static String of(String namespace, String path) {
        String lowerNamespace = namespace.toLowerCase(Locale.ROOT);
        return lowerNamespace.equals(NamespacedKey.MINECRAFT) ? path : lowerNamespace + SEPARATOR + path;
    }

    /**
     * {@return {@code worldName} with its namespace lower-cased and a {@code minecraft:} prefix dropped}
     */
    public static String normalize(String worldName) {
        return of(namespace(worldName), path(worldName));
    }

    /**
     * {@return the name a loaded Bukkit world is known by} Paper names the world {@code maps:lobby}
     * {@code maps_lobby}, so the Bukkit name is only used inside the {@code minecraft} namespace, where it also covers
     * the main worlds whose keys ({@code minecraft:overworld}) differ from their names. Spigot cannot create namespaced
     * worlds, so its keys are never consulted.
     */
    public static String of(World world) {
        if (!namespacesSupported()) {
            return world.getName();
        }
        NamespacedKey key = world.getKey();
        return key.getNamespace().equals(NamespacedKey.MINECRAFT) ? world.getName() : key.toString();
    }

    /**
     * {@return the name of a new world a player typed} A bare name is placed in {@code defaultNamespace}.
     */
    public static String fromInput(String input, String defaultNamespace) {
        return isQualified(input) ? normalize(input) : of(defaultNamespace, input);
    }

    /**
     * {@return whether {@code input} names its namespace, {@code minecraft} included}
     */
    public static boolean isQualified(String input) {
        return input.indexOf(SEPARATOR) >= 0;
    }

    /**
     * {@return {@code worldName} with its namespace spelled out, {@code minecraft} included} Typed back, it always
     * means this one world.
     */
    public static String qualified(String worldName) {
        return namespace(worldName) + SEPARATOR + path(worldName);
    }

    /**
     * {@return the name Paper gives a world it loads under this name's key} Other plugins, such as EssentialsX, store
     * worlds by that name.
     */
    public static String bukkitName(String worldName) {
        return isNamespaced(worldName)
                ? namespace(worldName) + "_" + path(worldName).toLowerCase(Locale.ROOT)
                : path(worldName);
    }

    public static boolean isValidNamespace(String namespace) {
        return NAMESPACE_PATTERN.matcher(namespace).matches();
    }

    /**
     * {@return whether {@code worldName} reads back as itself and, when namespaced, is a valid key} A second separator
     * fails both: {@code minecraft:a:b} would be stored as {@code a:b}, which is a different world.
     */
    public static boolean isValidName(String worldName) {
        return path(worldName).indexOf(SEPARATOR) < 0
                && (!isNamespaced(worldName) || NamespacedKey.fromString(id(worldName)) != null);
    }

    /**
     * {@return whether the server can create worlds outside the {@code minecraft} namespace}
     */
    public static boolean namespacesSupported() {
        return KEYED_CREATOR != null;
    }

    public static @Nullable World bukkitWorld(String worldName) {
        if (!isNamespaced(worldName)) {
            return Bukkit.getWorld(path(worldName));
        }

        // ponytail: linear scan, since Spigot's API has no lookup by key. Cheap for the hundreds of worlds a server
        // has.
        String id = id(worldName);
        for (World world : Bukkit.getWorlds()) {
            if (world.getKey().toString().equals(id)) {
                return world;
            }
        }
        return null;
    }

    /**
     * {@return a creator that loads or creates the world at its name's key}
     *
     * <p>A namespaced world imported before namespaces existed is stored under its Bukkit name ({@code maps_lobby}).
     * When that world is loaded and the name has no folder of its own, the creator uses the world's real key: Paper
     * only hands back a loaded world when both the name and the key match it.
     *
     * @throws IllegalArgumentException if the name is not a valid key
     * @throws UnsupportedOperationException if the world is namespaced and the server is not Paper
     */
    public static WorldCreator creator(String worldName) {
        if (!isNamespaced(worldName)) {
            World loaded = namespacesSupported() ? Bukkit.getWorld(path(worldName)) : null;
            return loaded != null
                            && !loaded.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)
                            && !FileUtils.hasPlainWorldFolder(worldName)
                    ? keyedCreator(loaded.getKey())
                    : new WorldCreator(path(worldName));
        }

        NamespacedKey key = NamespacedKey.fromString(id(worldName));
        if (key == null) {
            throw new IllegalArgumentException("\"%s\" is not a valid world key".formatted(worldName));
        }
        return keyedCreator(key);
    }

    private static WorldCreator keyedCreator(NamespacedKey key) {
        if (KEYED_CREATOR == null) {
            throw new UnsupportedOperationException(
                    "World \"%s\" is in a namespace, which requires Paper".formatted(key));
        }
        try {
            return KEYED_CREATOR.newInstance(key);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create world creator for \"%s\"".formatted(key), e);
        }
    }

    private static @Nullable Constructor<WorldCreator> resolveKeyedCreator() {
        try {
            return WorldCreator.class.getConstructor(NamespacedKey.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
