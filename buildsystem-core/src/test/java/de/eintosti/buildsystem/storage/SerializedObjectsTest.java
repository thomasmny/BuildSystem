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
package de.eintosti.buildsystem.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SerializedObjectsTest {

    /**
     * Casts a nested level to Integer, like some of Bukkit's item meta does.
     */
    public record Enchanted(int level) implements ConfigurationSerializable {

        @Override
        public Map<String, Object> serialize() {
            return Map.of("enchants", Map.of("sharpness", level));
        }

        public static Enchanted deserialize(Map<String, Object> args) {
            return new Enchanted((Integer) ((Map<?, ?>) args.get("enchants")).get("sharpness"));
        }
    }

    static {
        ConfigurationSerialization.registerClass(Enchanted.class);
    }

    @Test
    void revive_givesDeserializersSmallWholeNumbersAsInteger_atAnyDepth() {
        // What Gson reads back from the JSON column: every whole number as a Long.
        Map<String, Object> fromJson = Map.of(
                "items",
                List.of(Map.of(
                        ConfigurationSerialization.SERIALIZED_TYPE_KEY,
                        ConfigurationSerialization.getAlias(Enchanted.class),
                        "enchants",
                        Map.of("sharpness", 5L))),
                "date",
                5L);

        Map<String, Object> revived = SerializedObjects.revive(fromJson);

        assertEquals(List.of(new Enchanted(5)), revived.get("items"));
        assertEquals(5L, revived.get("date"), "numbers outside an object stay as they were read");
    }

    @Test
    void flatten_writesTheTypeKeyYamlWrites() {
        assertEquals(
                Map.of(
                        "item",
                        Map.of(
                                ConfigurationSerialization.SERIALIZED_TYPE_KEY,
                                ConfigurationSerialization.getAlias(Enchanted.class),
                                "enchants",
                                Map.of("sharpness", 3))),
                SerializedObjects.flatten(Map.of("item", new Enchanted(3))));
    }
}
