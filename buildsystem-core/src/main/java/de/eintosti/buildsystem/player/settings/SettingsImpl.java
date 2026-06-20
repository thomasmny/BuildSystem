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
package de.eintosti.buildsystem.player.settings;

import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.WorldDisplay;
import de.eintosti.buildsystem.world.display.WorldDisplayImpl;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SettingsImpl implements Settings {

    private DesignColor designColor;
    private NavigatorType navigatorType;
    private final WorldDisplay worldDisplay;
    private boolean clearInventory;
    private boolean disableInteract;
    private boolean hidePlayers;
    private boolean instantPlaceSigns;
    private boolean keepNavigator;
    private boolean nightVision;
    private boolean noClip;
    private boolean placePlants;
    private boolean scoreboard;
    private boolean slabBreaking;
    private boolean spawnTeleport;
    private boolean openTrapDoors;

    public SettingsImpl() {
        this(builder());
    }

    private SettingsImpl(Builder builder) {
        this.navigatorType = builder.navigatorType;
        this.designColor = builder.designColor;
        this.worldDisplay = builder.worldDisplay;
        this.clearInventory = builder.clearInventory;
        this.disableInteract = builder.disableInteract;
        this.hidePlayers = builder.hidePlayers;
        this.instantPlaceSigns = builder.instantPlaceSigns;
        this.keepNavigator = builder.keepNavigator;
        this.nightVision = builder.nightVision;
        this.noClip = builder.noClip;
        this.placePlants = builder.placePlants;
        this.scoreboard = builder.scoreboard;
        this.slabBreaking = builder.slabBreaking;
        this.spawnTeleport = builder.spawnTeleport;
        this.openTrapDoors = builder.openTrapDoors;
    }

    /**
     * Creates a builder pre-seeded with the defaults applied to a player who has never changed their settings. Both the
     * {@link #SettingsImpl() default settings} and storage deserialization go through it, so those defaults live in
     * exactly one place.
     *
     * @return A fresh builder seeded with the default settings
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public NavigatorType getNavigatorType() {
        return navigatorType;
    }

    @Override
    public void setNavigatorType(NavigatorType navigatorType) {
        this.navigatorType = navigatorType;
    }

    @Override
    public DesignColor getDesignColor() {
        return designColor;
    }

    @Override
    public void setDesignColor(DesignColor designColor) {
        this.designColor = designColor;
    }

    @Override
    public WorldDisplay getWorldDisplay() {
        return worldDisplay;
    }

    @Override
    public boolean isClearInventory() {
        return clearInventory;
    }

    @Override
    public void setClearInventory(boolean clearInventory) {
        this.clearInventory = clearInventory;
    }

    @Override
    public boolean isDisableInteract() {
        return disableInteract;
    }

    @Override
    public void setDisableInteract(boolean disableInteract) {
        this.disableInteract = disableInteract;
    }

    @Override
    public boolean isHidePlayers() {
        return hidePlayers;
    }

    @Override
    public void setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
    }

    @Override
    public boolean isInstantPlaceSigns() {
        return instantPlaceSigns;
    }

    @Override
    public void setInstantPlaceSigns(boolean instantPlaceSigns) {
        this.instantPlaceSigns = instantPlaceSigns;
    }

    @Override
    public boolean isKeepNavigator() {
        return keepNavigator;
    }

    @Override
    public void setKeepNavigator(boolean keepNavigator) {
        this.keepNavigator = keepNavigator;
    }

    @Override
    public boolean isNightVision() {
        return nightVision;
    }

    @Override
    public void setNightVision(boolean nightVision) {
        this.nightVision = nightVision;
    }

    @Override
    public boolean isNoClip() {
        return noClip;
    }

    @Override
    public void setNoClip(boolean noClip) {
        this.noClip = noClip;
    }

    @Override
    public boolean isPlacePlants() {
        return placePlants;
    }

    @Override
    public void setPlacePlants(boolean placePlants) {
        this.placePlants = placePlants;
    }

    @Override
    public boolean isScoreboard() {
        return scoreboard;
    }

    @Override
    public void setScoreboard(boolean scoreboard) {
        this.scoreboard = scoreboard;
    }

    @Override
    public boolean isSlabBreaking() {
        return slabBreaking;
    }

    @Override
    public void setSlabBreaking(boolean slabBreaking) {
        this.slabBreaking = slabBreaking;
    }

    @Override
    public boolean isSpawnTeleport() {
        return spawnTeleport;
    }

    @Override
    public void setSpawnTeleport(boolean spawnTeleport) {
        this.spawnTeleport = spawnTeleport;
    }

    @Override
    public boolean isOpenTrapDoors() {
        return openTrapDoors;
    }

    @Override
    public void setOpenTrapDoors(boolean trapDoor) {
        this.openTrapDoors = trapDoor;
    }

    /**
     * Fluent builder for {@link SettingsImpl}. Each option is a named setter, so deserialization binds every value by
     * name instead of passing a long row of positional booleans where two could be transposed unnoticed. Options left
     * unset keep their default.
     */
    public static final class Builder {

        private NavigatorType navigatorType = NavigatorType.OLD;
        private DesignColor designColor = DesignColor.BLACK;
        private WorldDisplay worldDisplay = new WorldDisplayImpl();
        private boolean clearInventory = false;
        private boolean disableInteract = false;
        private boolean hidePlayers = false;
        private boolean instantPlaceSigns = false;
        private boolean keepNavigator = false;
        private boolean nightVision = false;
        private boolean noClip = false;
        private boolean placePlants = false;
        private boolean scoreboard = true;
        private boolean slabBreaking = false;
        private boolean spawnTeleport = true;
        private boolean openTrapDoors = false;

        private Builder() {}

        /** Sets the navigator type; {@code null} keeps the default {@link NavigatorType#OLD}. */
        public Builder navigatorType(@Nullable NavigatorType navigatorType) {
            if (navigatorType != null) {
                this.navigatorType = navigatorType;
            }
            return this;
        }

        /** Sets the design colour; {@code null} keeps the default {@link DesignColor#BLACK}. */
        public Builder designColor(@Nullable DesignColor designColor) {
            if (designColor != null) {
                this.designColor = designColor;
            }
            return this;
        }

        public Builder worldDisplay(WorldDisplay worldDisplay) {
            this.worldDisplay = worldDisplay;
            return this;
        }

        public Builder clearInventory(boolean clearInventory) {
            this.clearInventory = clearInventory;
            return this;
        }

        public Builder disableInteract(boolean disableInteract) {
            this.disableInteract = disableInteract;
            return this;
        }

        public Builder hidePlayers(boolean hidePlayers) {
            this.hidePlayers = hidePlayers;
            return this;
        }

        public Builder instantPlaceSigns(boolean instantPlaceSigns) {
            this.instantPlaceSigns = instantPlaceSigns;
            return this;
        }

        public Builder keepNavigator(boolean keepNavigator) {
            this.keepNavigator = keepNavigator;
            return this;
        }

        public Builder nightVision(boolean nightVision) {
            this.nightVision = nightVision;
            return this;
        }

        public Builder noClip(boolean noClip) {
            this.noClip = noClip;
            return this;
        }

        public Builder placePlants(boolean placePlants) {
            this.placePlants = placePlants;
            return this;
        }

        public Builder scoreboard(boolean scoreboard) {
            this.scoreboard = scoreboard;
            return this;
        }

        public Builder slabBreaking(boolean slabBreaking) {
            this.slabBreaking = slabBreaking;
            return this;
        }

        public Builder spawnTeleport(boolean spawnTeleport) {
            this.spawnTeleport = spawnTeleport;
            return this;
        }

        public Builder openTrapDoors(boolean openTrapDoors) {
            this.openTrapDoors = openTrapDoors;
            return this;
        }

        public SettingsImpl build() {
            return new SettingsImpl(this);
        }
    }
}
