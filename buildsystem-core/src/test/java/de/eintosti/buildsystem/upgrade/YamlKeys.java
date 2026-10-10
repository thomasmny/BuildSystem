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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Compares YAML files by the set of their leaf key paths, so a writer that orders keys differently still passes.
 */
final class YamlKeys {

    private YamlKeys() {}

    static Set<String> leaves(Path file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        Set<String> keys = new TreeSet<>();
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * Asserts that every leaf key of each file in {@code before} is still in the same file in {@code after}.
     *
     * @param rename Maps a key of the old file to where the new version keeps it, for files whose sections are re-keyed
     *     on purpose, such as name-keyed worlds becoming UUID-keyed
     * @param dropped Old keys that are removed on purpose, by file name, each with the reason in the caller
     */
    static void assertNoKeyLost(
            Path before, Path after, Map<String, UnaryOperator<String>> rename, Map<String, Set<String>> dropped) {
        for (String file : before.toFile().list((dir, name) -> name.endsWith(".yml"))) {
            UnaryOperator<String> mapping = rename.getOrDefault(file, UnaryOperator.identity());
            Set<String> expected = new TreeSet<>();
            for (String key : leaves(before.resolve(file))) {
                String mapped = mapping.apply(key);
                if (!dropped.getOrDefault(file, Set.of()).contains(mapped)) {
                    expected.add(mapped);
                }
            }
            Set<String> missing = new TreeSet<>(expected);
            missing.removeAll(leaves(after.resolve(file)));
            assertEquals(Set.of(), missing, "keys lost from " + file);
        }
    }
}
