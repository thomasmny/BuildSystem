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

import de.eintosti.buildsystem.util.MaterialUtils;
import de.eintosti.buildsystem.world.data.WorldStatusImpl;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;

/** {@link Codec} for the statuses in {@code statuses.yml}, keyed by status id. */
@NullMarked
public final class StatusCodec implements Codec<WorldStatusImpl> {

    @Override
    public String key(WorldStatusImpl status) {
        return status.getId();
    }

    @Override
    public Map<String, Object> serialize(WorldStatusImpl status) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put("display-name", status.getDisplayName());
        serialized.put("color", status.getColor());
        serialized.put("icon", status.getIcon().name());
        serialized.put("order", status.getOrder());
        serialized.put("building-allowed", status.isBuildingAllowed());
        Codec.putIfPresent(serialized, "progresses-to", status.getProgressesTo().orElse(null));
        serialized.put("built-in", status.isBuiltIn());
        serialized.put("status-slot", status.getSlot());
        serialized.put("shown-in-status-menu", status.isShown());
        return serialized;
    }

    @Override
    public WorldStatusImpl deserialize(String id, ConfigurationSection section) {
        Material icon = Objects.requireNonNullElse(MaterialUtils.match(section.getString("icon")), Material.WHITE_DYE);
        String progressesTo = section.getString("progresses-to");
        return WorldStatusImpl.builder(id)
                .displayName(section.getString("display-name", id))
                .color(section.getString("color", "&7"))
                .icon(icon)
                .order(section.getInt("order", 0))
                .buildingAllowed(section.getBoolean("building-allowed", true))
                .progressesTo(progressesTo != null && !progressesTo.isBlank() ? progressesTo : null)
                .builtIn(section.getBoolean("built-in", false))
                // A pre-layout statuses.yml has neither key: the status is left unplaced (slot -1) so the registry can
                // migrate it into the grid by order on load.
                .statusSlot(section.getInt("status-slot", -1))
                .shownInStatusMenu(section.getBoolean("shown-in-status-menu", true))
                .build();
    }
}
