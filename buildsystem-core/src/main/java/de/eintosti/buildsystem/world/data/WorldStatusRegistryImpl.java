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
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.storage.yaml.YamlStatusStorage;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Owns every {@link BuildWorldStatus}, seeding the built-in defaults on first run and persisting all administrator
 * changes. Deleting a status cascades: every world that used it is reset to the {@link #getDefaultStatus() default}
 * status and the id is removed from every category that grouped it. The last remaining status can never be deleted, so
 * a valid default always exists.
 */
@NullMarked
public class WorldStatusRegistryImpl implements WorldStatusRegistry {

    /**
     * The number of slots in the {@code /worlds setStatus} picker. Status layout slots live in {@code [0, SIZE)},
     * mirroring the navigator's fixed grid.
     */
    public static final int STATUS_MENU_SIZE = 27;

    /**
     * Default picker slots for the built-in statuses, applied when they are seeded and when the layout is reset.
     */
    private static final Map<String, Integer> DEFAULT_SLOTS = Map.of(
            "not_started", 10,
            "in_progress", 11,
            "almost_finished", 12,
            "finished", 13,
            "archive", 14,
            "hidden", 15);

    private final BuildSystemPlugin plugin;
    private final NavigatorCategoryRegistryImpl categoryRegistry;
    private final YamlStatusStorage storage;
    private final Map<String, WorldStatusImpl> statuses = new LinkedHashMap<>();

    public WorldStatusRegistryImpl(BuildSystemPlugin plugin, NavigatorCategoryRegistryImpl categoryRegistry) {
        this.plugin = plugin;
        this.categoryRegistry = categoryRegistry;
        this.storage = new YamlStatusStorage(plugin);

        this.statuses.putAll(storage.load());
        if (this.statuses.isEmpty()) {
            seedDefaults();
            storage.saveAll(this.statuses.values());
        }
        // A statuses.yml written before the picker became a configurable layout has no slots; give those statuses a
        // home in the grid (by order) so the upgraded picker is populated instead of empty.
        placeUnplacedStatuses();
    }

    private void seedDefaults() {
        put("not_started", "Not Started", "&c", XMaterial.RED_DYE, 1, true, "in_progress");
        put("in_progress", "In Progress", "&6", XMaterial.ORANGE_DYE, 2, true, null);
        put("almost_finished", "Almost Finished", "&a", XMaterial.LIME_DYE, 3, true, null);
        put("finished", "Finished", "&2", XMaterial.GREEN_DYE, 4, true, null);
        put("archive", "Archive", "&3", XMaterial.CYAN_DYE, 5, false, null);
        put("hidden", "Hidden", "&7", XMaterial.BONE_MEAL, 6, true, null);
    }

    private void put(
            String id,
            String displayName,
            String color,
            XMaterial icon,
            int order,
            boolean buildingAllowed,
            @Nullable String progressesTo) {
        // One-time migration: a server upgrading from pre-4.0 carries the customised status name in the legacy
        // "status_<id>" message key (the message store never prunes user keys). When present, adopt it so the
        // server's renames/translations survive the move of status names from messages.yml into statuses.yml.
        String[] styledName = migrateLegacyName(id, displayName, color);
        this.statuses.put(
                id,
                WorldStatusImpl.builder(id)
                        .displayName(styledName[1])
                        .color(styledName[0])
                        .icon(icon)
                        .order(order)
                        .buildingAllowed(buildingAllowed)
                        .progressesTo(progressesTo)
                        .builtIn(true)
                        .statusSlot(DEFAULT_SLOTS.getOrDefault(id, -1))
                        .shownInStatusMenu(true)
                        .build());
    }

    /**
     * Resolves the seeded name/colour for a built-in status, preferring the value a pre-4.0 server stored under the
     * legacy {@code status_<id>} message key. Returns {@code [color, displayName]}. The legacy value embeds the colour
     * in the text (e.g. {@code "&cNot Started"}); a leading legacy colour token is split off so the new model keeps
     * colour and name separate. Falls back to the supplied defaults when no legacy key is present.
     */
    private String[] migrateLegacyName(String id, String defaultName, String defaultColor) {
        Optional<String> legacy = plugin.getMessages().findRaw("status_" + id);
        if (legacy.isEmpty() || legacy.get().isBlank()) {
            return new String[] {defaultColor, defaultName};
        }

        String raw = legacy.get().strip();
        int colorLength = leadingColorLength(raw);
        if (colorLength > 0) {
            return new String[] {
                raw.substring(0, colorLength), raw.substring(colorLength).strip()
            };
        }
        return new String[] {defaultColor, raw};
    }

    /**
     * Returns the length of the colour token at the start of a legacy styled string, covering the formats a pre-4.0
     * server could have stored in a {@code status_<id>} message: the Spigot hex form {@code &x&R&R&G&G&B&B}, the
     * {@code &#RRGGBB} and bare {@code #RRGGBB} hex forms, and a single legacy code such as {@code &c}. Returns 0 when
     * the string does not start with a recognised colour token.
     */
    private static int leadingColorLength(String raw) {
        if (raw.length() >= 2 && (raw.charAt(0) == '&' || raw.charAt(0) == '§')) {
            if (Character.toLowerCase(raw.charAt(1)) == 'x' && raw.length() >= 14) {
                return 14; // &x&R&R&G&G&B&B
            }
            if (raw.charAt(1) == '#' && raw.length() >= 8) {
                return 8; // &#RRGGBB
            }
            return 2; // &c, &a, ...
        }
        if (raw.startsWith("#") && raw.length() >= 7) {
            return 7; // #RRGGBB
        }
        return 0;
    }

    @Override
    public Collection<BuildWorldStatus> getStatuses() {
        List<BuildWorldStatus> ordered = new ArrayList<>(this.statuses.values());
        ordered.sort(Comparator.comparingInt(BuildWorldStatus::getOrder));
        return Collections.unmodifiableList(ordered);
    }

    @Override
    public Optional<BuildWorldStatus> getStatus(@Nullable String id) {
        return Optional.ofNullable(this.statuses.get(id));
    }

    @Override
    public BuildWorldStatus getDefaultStatus() {
        if (this.statuses.isEmpty()) {
            seedDefaults();
            storage.saveAll(this.statuses.values());
        }
        WorldStatusImpl notStarted = this.statuses.get(NOT_STARTED_ID);
        // Prefer the built-in fallback, but it may have been deleted by an admin; then the lowest-order status wins.
        return notStarted != null ? notStarted : getStatuses().iterator().next();
    }

    /**
     * Restores the six built-in statuses, discarding any customizations. Used by the setup menu's "reset to defaults"
     * control so an admin can always get back to a known-good state.
     */
    public void resetToDefaults() {
        this.statuses.clear();
        seedDefaults();
        storage.saveAll(this.statuses.values());
    }

    public WorldStatusImpl createStatus(String displayName) {
        String id = StringUtils.uniqueId(displayName, "status", this.statuses::containsKey);
        int order = this.statuses.values().stream()
                        .mapToInt(WorldStatusImpl::getOrder)
                        .max()
                        .orElse(0)
                + 1;
        // Place the new status at the first free picker slot so it shows up straight away; if the grid is full it stays
        // in the editor's palette until an admin makes room.
        int slot = firstFreeSlot();
        WorldStatusImpl status = WorldStatusImpl.builder(id)
                .displayName(displayName)
                .order(order)
                .statusSlot(slot)
                .shownInStatusMenu(slot >= 0)
                .build();
        this.statuses.put(id, status);
        storage.save(status);
        categoryRegistry.addStatusToDefaultCategory(id);
        return status;
    }

    /**
     * Restores the built-in statuses to their default picker slots and hides every custom status from the picker,
     * without otherwise changing the statuses. Backs the status editor's "reset layout" control, mirroring the
     * navigator's layout reset; a full {@link #resetToDefaults()} is the separate "reset everything".
     */
    public void resetStatusLayout() {
        for (WorldStatusImpl status : this.statuses.values()) {
            Integer preset = DEFAULT_SLOTS.get(status.getId());
            if (preset != null) {
                status.setStatusSlot(preset);
                status.setShownInStatusMenu(true);
            } else {
                status.setShownInStatusMenu(false);
            }
            storage.save(status);
        }
    }

    /**
     * Gives every status that has no picker slot one, laying them out by {@link BuildWorldStatus#getOrder() order} into
     * the first free slots. Persists only the statuses it moves, so a fully-placed registry is untouched.
     */
    private void placeUnplacedStatuses() {
        List<WorldStatusImpl> unplaced = this.statuses.values().stream()
                .filter(status -> status.getStatusSlot() < 0)
                .sorted(Comparator.comparingInt(WorldStatusImpl::getOrder))
                .toList();
        for (WorldStatusImpl status : unplaced) {
            int slot = firstFreeSlot();
            if (slot < 0) {
                break;
            }
            status.setStatusSlot(slot);
            status.setShownInStatusMenu(true);
            storage.save(status);
        }
    }

    /**
     * {@return the first picker slot not occupied by a shown status, or {@code -1} when the grid is full}
     */
    private int firstFreeSlot() {
        Set<Integer> occupied = new HashSet<>();
        for (WorldStatusImpl status : this.statuses.values()) {
            if (status.isShownInStatusMenu() && status.getStatusSlot() >= 0) {
                occupied.add(status.getStatusSlot());
            }
        }
        for (int slot = 0; slot < STATUS_MENU_SIZE; slot++) {
            if (!occupied.contains(slot)) {
                return slot;
            }
        }
        return -1;
    }

    public void persist(WorldStatusImpl status) {
        storage.save(status);
    }

    /**
     * Lists the loaded worlds currently using the given status, for the deletion confirmation prompt.
     */
    public List<BuildWorld> worldsWithStatus(String id) {
        return plugin.getWorldService().getWorldStorage().getBuildWorlds().stream()
                .filter(world -> world.getData().getStatus().getId().equals(id))
                .toList();
    }

    /**
     * Deletes a status (built-in or custom), cascading every world that used it back to the
     * {@link #getDefaultStatus() default} and removing the id from every category that grouped it. The last remaining
     * status is never deleted, so a valid default always exists; an admin can restore the built-ins with
     * {@link #resetToDefaults()}.
     *
     * @return {@code true} if the status was deleted, {@code false} if it was unknown or the last remaining status
     */
    public boolean deleteStatus(String id) {
        if (!this.statuses.containsKey(id) || this.statuses.size() <= 1) {
            return false;
        }

        this.statuses.remove(id);
        BuildWorldStatus fallback = getDefaultStatus();
        for (BuildWorld world : worldsWithStatus(id)) {
            world.getData().setStatus(fallback);
            plugin.getWorldService().getWorldStorage().save(world);
        }

        clearDanglingProgression(id);
        categoryRegistry.removeStatusFromCategories(id);
        storage.delete(id);
        return true;
    }

    /**
     * Clears the {@code progressesTo} target on any sibling status that auto-advanced to the just-deleted status, so no
     * status is left pointing at an id that no longer resolves.
     */
    private void clearDanglingProgression(String deletedId) {
        for (WorldStatusImpl status : this.statuses.values()) {
            if (status.getProgressesTo().filter(deletedId::equals).isPresent()) {
                status.setProgressesTo(null);
                persist(status);
            }
        }
    }
}
