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

import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Mutable implementation of a navigator category. Owns the ordered list of member status ids and all configurable styling
 * and the {@link Visibility visibilities} it groups; the {@link #getId() id} is immutable once created.
 */
@NullMarked
public final class NavigatorCategoryImpl extends RegistryEntryImpl implements NavigatorCategory {

    private final List<String> statusIds = new ArrayList<>();
    private final EnumSet<Visibility> visibilities = EnumSet.noneOf(Visibility.class);

    private @Nullable String iconSkullTexture;

    private NavigatorCategoryImpl(Builder builder) {
        super(
                builder.id,
                builder.builtIn,
                builder.displayName,
                builder.color,
                builder.icon,
                builder.navigatorSlot,
                builder.shownInNavigator);
        this.iconSkullTexture = builder.iconSkullTexture;
        this.visibilities.addAll(builder.visibilities);
        this.statusIds.addAll(builder.statusIds);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    @Override
    public @Nullable String getIconSkullTexture() {
        return iconSkullTexture;
    }

    public void setIconSkullTexture(@Nullable String iconSkullTexture) {
        this.iconSkullTexture = iconSkullTexture;
    }

    @Override
    public Set<Visibility> getVisibilities() {
        return Collections.unmodifiableSet(visibilities);
    }

    public void toggleVisibility(Visibility visibility) {
        if (!visibilities.add(visibility)) {
            visibilities.remove(visibility);
        }
    }

    @Override
    public List<String> getStatusIds() {
        return Collections.unmodifiableList(statusIds);
    }

    public void addStatusId(String statusId) {
        if (!statusIds.contains(statusId)) {
            statusIds.add(statusId);
        }
    }

    public void removeStatusId(String statusId) {
        statusIds.remove(statusId);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof NavigatorCategory other && getId().equals(other.getId());
    }

    @Override
    public String toString() {
        return "NavigatorCategory{" + getId() + "}";
    }

    /**
     * Fluent builder for {@link NavigatorCategoryImpl}. Only {@code id} is required; every other field defaults to a
     * neutral, navigator-shown custom category that groups {@link Visibility#EVERYONE} worlds with no member statuses.
     */
    public static final class Builder {

        private final String id;

        private String displayName;
        private String color = "&7";
        private Material icon = Material.CHEST;
        private @Nullable String iconSkullTexture = null;
        private Set<Visibility> visibilities = EnumSet.of(Visibility.EVERYONE);
        private boolean shownInNavigator = true;
        private int navigatorSlot = 0;
        private boolean builtIn = false;
        private List<String> statusIds = new ArrayList<>();

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

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder iconSkullTexture(@Nullable String iconSkullTexture) {
            this.iconSkullTexture = iconSkullTexture;
            return this;
        }

        public Builder visibilities(Set<Visibility> visibilities) {
            this.visibilities = EnumSet.copyOf(visibilities);
            return this;
        }

        public Builder shownInNavigator(boolean shownInNavigator) {
            this.shownInNavigator = shownInNavigator;
            return this;
        }

        public Builder navigatorSlot(int navigatorSlot) {
            this.navigatorSlot = navigatorSlot;
            return this;
        }

        public Builder builtIn(boolean builtIn) {
            this.builtIn = builtIn;
            return this;
        }

        public Builder statusIds(List<String> statusIds) {
            this.statusIds = new ArrayList<>(statusIds);
            return this;
        }

        public NavigatorCategoryImpl build() {
            return new NavigatorCategoryImpl(this);
        }
    }
}
