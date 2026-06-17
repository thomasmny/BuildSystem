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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Mutable implementation of a world status. Styling and behaviour are configurable through the setup menu; the
 * {@link #getId() id} is immutable once created. Statuses are shared across {@code NavigatorCategory}s, so a status carries
 * no owning category of its own.
 */
@NullMarked
public final class WorldStatusImpl implements BuildWorldStatus {

    private final String id;
    private final boolean builtIn;

    private String displayName;
    private String color;
    private XMaterial icon;
    private int order;
    private boolean buildingAllowed;
    private boolean visibleInNavigator;
    private @Nullable String progressesTo;

    private WorldStatusImpl(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.color = builder.color;
        this.icon = builder.icon;
        this.order = builder.order;
        this.buildingAllowed = builder.buildingAllowed;
        this.visibleInNavigator = builder.visibleInNavigator;
        this.progressesTo = builder.progressesTo;
        this.builtIn = builder.builtIn;
    }

    /**
     * Creates a builder for a status with the given id.
     *
     * @param id The immutable status id
     * @return A new builder seeded with sensible defaults
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public XMaterial getIcon() {
        return icon;
    }

    public void setIcon(XMaterial icon) {
        this.icon = icon;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String getPermission() {
        return "buildsystem.setstatus." + id.toLowerCase(Locale.ROOT).replace("_", "");
    }

    @Override
    public boolean isBuildingAllowed() {
        return buildingAllowed;
    }

    public void setBuildingAllowed(boolean buildingAllowed) {
        this.buildingAllowed = buildingAllowed;
    }

    @Override
    public boolean isVisibleInNavigator() {
        return visibleInNavigator;
    }

    public void setVisibleInNavigator(boolean visibleInNavigator) {
        this.visibleInNavigator = visibleInNavigator;
    }

    @Override
    public Optional<String> getProgressesTo() {
        return Optional.ofNullable(progressesTo);
    }

    public void setProgressesTo(@Nullable String progressesTo) {
        this.progressesTo = progressesTo;
    }

    @Override
    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof BuildWorldStatus other && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "WorldStatus{" + id + "}";
    }

    /**
     * Fluent builder for {@link WorldStatusImpl}. Only {@code id} is required; every other field defaults to a neutral,
     * building-enabled, navigator-visible custom status.
     */
    public static final class Builder {

        private final String id;

        private String displayName;
        private String color = "&7";
        private XMaterial icon = XMaterial.WHITE_DYE;
        private int order = 0;
        private boolean buildingAllowed = true;
        private boolean visibleInNavigator = true;
        private @Nullable String progressesTo = null;
        private boolean builtIn = false;

        private Builder(String id) {
            this.id = id;
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder icon(XMaterial icon) {
            this.icon = icon;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder buildingAllowed(boolean buildingAllowed) {
            this.buildingAllowed = buildingAllowed;
            return this;
        }

        public Builder visibleInNavigator(boolean visibleInNavigator) {
            this.visibleInNavigator = visibleInNavigator;
            return this;
        }

        public Builder progressesTo(@Nullable String progressesTo) {
            this.progressesTo = progressesTo;
            return this;
        }

        public Builder builtIn(boolean builtIn) {
            this.builtIn = builtIn;
            return this;
        }

        public WorldStatusImpl build() {
            return new WorldStatusImpl(this);
        }
    }
}
