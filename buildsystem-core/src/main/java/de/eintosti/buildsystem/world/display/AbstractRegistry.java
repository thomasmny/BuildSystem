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
package de.eintosti.buildsystem.world.display;

import de.eintosti.buildsystem.api.world.display.RegistryEntry;
import de.eintosti.buildsystem.storage.yaml.YamlRegistryStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/**
 * What the status and category registries share: the entries keyed by id, seeding the built-ins, the default, the two
 * resets and deletion. What differs between them sits behind the hooks.
 */
@NullMarked
public abstract class AbstractRegistry<E extends RegistryEntryImpl> {

    protected final Map<String, E> entries = new LinkedHashMap<>();
    protected final YamlRegistryStorage<E> storage;
    private final Comparator<? super E> order;

    protected AbstractRegistry(YamlRegistryStorage<E> storage, Comparator<? super E> order) {
        this.storage = storage;
        this.order = order;
    }

    /** {@return the built-in entries in their default state, freshly built and not yet registered} */
    protected abstract Map<String, E> buildDefaults();

    /** {@return the id of the entry that is the default while it exists} */
    protected abstract String preferredDefaultId();

    /** Repairs whatever pointed at an entry that was deleted or discarded by a reset. */
    protected abstract void onDiscarded(String id);

    /** {@return whether the last entry may be deleted} */
    protected abstract boolean mayBeEmpty();

    /** Loads the stored entries, seeding the built-ins when there are none. Called once by the subclass constructor. */
    protected final void loadOrSeed() {
        entries.putAll(storage.load());
        if (entries.isEmpty()) {
            seed();
        }
    }

    private void seed() {
        entries.putAll(buildDefaults());
        storage.saveAll(entries.values());
    }

    /** {@return the entries in display order} */
    protected final List<E> sorted() {
        List<E> sorted = new ArrayList<>(entries.values());
        sorted.sort(order);
        return sorted;
    }

    /**
     * {@return the preferred default entry, or the first in display order once it has been deleted} An empty registry
     * reseeds the built-ins on demand, so there is always a default.
     */
    public E getDefault() {
        if (entries.isEmpty()) {
            seed();
        }
        E preferred = entries.get(preferredDefaultId());
        return preferred != null ? preferred : sorted().getFirst();
    }

    /**
     * Restores the built-ins and discards every other entry, cascading each discarded one through
     * {@link #onDiscarded(String)} exactly as {@link #delete(String)} does. Backs the setup menu's "reset to defaults".
     */
    public void resetToDefaults() {
        Set<String> discarded = new LinkedHashSet<>(entries.keySet());
        entries.clear();
        seed();
        // saveAll rewrote the whole section, so the discarded entries are already gone from disk.
        discarded.removeAll(entries.keySet());
        discarded.forEach(this::onDiscarded);
    }

    /**
     * Puts the built-ins back in their default slots and takes every other entry out of the menu, without otherwise
     * changing them.
     */
    public void resetLayout() {
        Map<String, E> defaults = buildDefaults();
        for (E entry : entries.values()) {
            E preset = defaults.get(entry.getId());
            if (preset != null) {
                entry.setSlot(preset.getSlot());
                entry.setShown(preset.isShown());
            } else {
                entry.setShown(false);
            }
            storage.save(entry);
        }
    }

    /**
     * Deletes an entry, built-in or not, and cascades it through {@link #onDiscarded(String)} while any entry remains.
     *
     * @return {@code true} if it was deleted, {@code false} if it was unknown or the last one of a registry that may not
     *     be empty
     */
    public boolean delete(String id) {
        if (!entries.containsKey(id) || (entries.size() == 1 && !mayBeEmpty())) {
            return false;
        }
        entries.remove(id);
        storage.delete(id);
        if (!entries.isEmpty()) {
            onDiscarded(id);
        }
        return true;
    }

    /** Writes an entry this registry holds; an unknown id is ignored. */
    protected final void save(RegistryEntry entry) {
        E known = entries.get(entry.getId());
        if (known != null) {
            storage.save(known);
        }
    }
}
