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
package de.eintosti.buildsystem.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptomorin.xseries.XMaterial;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the two sources of truth for every config default against each other: the value written in the bundled
 * {@code config.yml} and the fallback the parser uses when the key is absent.
 *
 * <p>They can only disagree on a server whose config predates a key, which is the hardest case to notice — the shipped
 * file says one thing and such a server silently gets the other. Rather than scrape the parser for its literals, this
 * parses an empty config and the bundled file through the same parser and compares the resulting trees, so any default
 * reachable through {@link ConfigService#parse} is covered without listing it here.
 */
@NullMarked
class ConfigDefaultsDriftTest {

    private static final Logger LOGGER = Logger.getLogger("ConfigDefaultsDriftTest");

    /**
     * Paths whose shipped value is deliberately not the parser's fallback. Each is a list that ships populated but
     * defaults to empty, so a server that removed the key opts out rather than silently inheriting entries it never
     * asked for. Anything else appearing here is drift, not a decision.
     */
    private static final Set<String> INTENTIONAL = Set.of(
            "world.deletionBlacklist", "world.unload.blacklistedWorlds", "settings.worldPermissionWhitelist");

    @Test
    @DisplayName("Every parser default matches the value shipped in config.yml")
    void parserDefaults_matchBundledConfigYml() {
        PluginConfig fromEmptyConfig = parse(new YamlConfiguration());
        PluginConfig fromBundledFile = parse(loadBundledConfig());

        List<String> drifted = new ArrayList<>();
        compare("", fromBundledFile, fromEmptyConfig, drifted);

        assertTrue(
                drifted.isEmpty(),
                """
                config.yml and the parser's fallbacks disagree. A server missing the key gets the second value:
                  %s
                Fix the parser default (or config.yml), or add the path to INTENTIONAL with a reason."""
                        .formatted(String.join("\n  ", drifted)));
    }

    /**
     * Walks two config trees in parallel, recording the dotted path of every component whose values differ. Records are
     * descended into so the failure names the leaf that drifted rather than the whole subtree.
     */
    private static void compare(String path, @Nullable Object shipped, @Nullable Object fallback, List<String> drifted) {
        if (INTENTIONAL.contains(path)) {
            return;
        }
        if (Objects.equals(shipped, fallback)) {
            return;
        }
        if (shipped != null && fallback != null && shipped.getClass() == fallback.getClass()) {
            RecordComponent[] components = shipped.getClass().getRecordComponents();
            if (components != null) {
                for (RecordComponent component : components) {
                    compare(
                            path.isEmpty() ? component.getName() : path + "." + component.getName(),
                            read(component, shipped),
                            read(component, fallback),
                            drifted);
                }
                return;
            }
        }
        drifted.add("%s: config.yml=%s, parser default=%s".formatted(path, shipped, fallback));
    }

    private static @Nullable Object read(RecordComponent component, Object owner) {
        try {
            return component.getAccessor().invoke(owner);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not read " + component.getName(), e);
        }
    }

    private static PluginConfig parse(YamlConfiguration config) {
        return ConfigService.parse(config, LOGGER, XMaterial.WOODEN_AXE);
    }

    private static YamlConfiguration loadBundledConfig() {
        try (InputStream stream = ConfigService.class.getClassLoader().getResourceAsStream("config.yml")) {
            Objects.requireNonNull(stream, "config.yml is missing from the resources");
            YamlConfiguration config = new YamlConfiguration();
            config.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return config;
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException("Could not read the bundled config.yml", e);
        }
    }
}
