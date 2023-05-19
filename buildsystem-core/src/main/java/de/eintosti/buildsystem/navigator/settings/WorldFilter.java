/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.navigator.settings;

import de.eintosti.buildsystem.world.BuildWorld;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class WorldFilter implements ConfigurationSerializable {

    private Mode mode;
    private String text;

    public WorldFilter() {
        this.mode = Mode.NONE;
        this.text = "";
    }

    public WorldFilter(Mode mode, String text) {
        this.mode = mode;
        this.text = text;
    }

    public enum Mode {
        NONE("world_filter_mode_none"),
        STARTS_WITH("world_filter_mode_starts_with"),
        CONTAINS("world_filter_mode_contains"),
        MATCHES("world_filter_mode_matches");

        private final String loreKey;

        Mode(String loreKey) {
            this.loreKey = loreKey;
        }

        public String getLoreKey() {
            return loreKey;
        }

        public Mode getNext() {
            switch (this) {
                default: // NONE
                    return STARTS_WITH;
                case STARTS_WITH:
                    return CONTAINS;
                case CONTAINS:
                    return MATCHES;
                case MATCHES:
                    return NONE;
            }
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Predicate<BuildWorld> apply() {
        switch (mode) {
            case STARTS_WITH:
                return buildWorld -> buildWorld.getName().startsWith(text);
            case CONTAINS:
                return buildWorld -> buildWorld.getName().contains(text);
            case MATCHES:
                return buildWorld -> buildWorld.getName().matches(text);
            default:
                return buildWorld -> true;
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> display = new HashMap<>();
        display.put("mode", mode.toString());
        display.put("text", text);
        return display;
    }
}