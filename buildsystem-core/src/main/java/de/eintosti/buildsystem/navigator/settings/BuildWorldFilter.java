/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.navigator.settings;

import de.eintosti.buildsystem.api.settings.WorldFilter;
import de.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class BuildWorldFilter implements WorldFilter, ConfigurationSerializable {

    private Mode mode;
    private String text;

    public BuildWorldFilter() {
        this.mode = Mode.NONE;
        this.text = "";
    }

    public BuildWorldFilter(Mode mode, String text) {
        this.mode = mode;
        this.text = text;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
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