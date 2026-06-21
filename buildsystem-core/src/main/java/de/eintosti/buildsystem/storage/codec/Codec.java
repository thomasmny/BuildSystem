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
package de.eintosti.buildsystem.storage.codec;

import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;

/**
 * Maps a single entity of type {@code T} to and from its persisted YAML form, isolating the (de)serialization concern
 * from the file plumbing ({@link de.eintosti.buildsystem.storage.yaml.YamlStore}) and the in-memory cache. Each storage
 * holds one codec and delegates its per-entity mapping to it, so the key-string literals and conversions live in one
 * focused, unit-testable place instead of being smeared across the storage.
 *
 * <p>Implementations are pure mappers with respect to the configuration: they read from and produce plain maps and do
 * not touch the backing file or lock. Cross-entity wiring that cannot be resolved from a single entity's section — such
 * as a folder's parent link — is intentionally left to the storage's load loop rather than forced into this contract.
 *
 * @param <T> The entity type this codec maps
 */
@NullMarked
public interface Codec<T> {

    /**
     * Returns the section key the given entity is filed under within its storage's root section. This is the single
     * place the storage-identity decision lives (an entity's name today; its UUID after the v4 format migration).
     *
     * @param value The entity to key
     * @return The section key
     */
    String key(T value);

    /**
     * Serializes the entity into the map persisted under its {@link #key(Object) key}.
     *
     * @param value The entity to serialize
     * @return The persisted map form, round-trippable by {@link #deserialize(String, ConfigurationSection)}
     */
    Map<String, Object> serialize(T value);

    /**
     * Reconstructs an entity from the section stored under {@code key}.
     *
     * @param key The section key the entity was filed under (carries identity that is not duplicated inside the section)
     * @param section The configuration section holding the entity's serialized fields
     * @return The reconstructed entity
     */
    T deserialize(String key, ConfigurationSection section);
}
