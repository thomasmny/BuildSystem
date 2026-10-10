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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.util.StringUtil;
import org.jspecify.annotations.NullMarked;

/**
 * Tab completion helpers shared by the commands and the {@code /worlds} subcommands.
 */
@NullMarked
public final class Completions {

    private Completions() {}

    /**
     * {@return whether {@code candidate} starts with {@code input}, ignoring case}
     */
    public static boolean matches(String input, String candidate) {
        return StringUtil.startsWithIgnoreCase(candidate, input);
    }

    /**
     * Adds {@code candidate} to {@code result} if it starts with {@code input}, ignoring case.
     */
    public static void addMatching(String input, String candidate, List<String> result) {
        if (matches(input, candidate)) {
            result.add(candidate);
        }
    }

    /**
     * Adds the worlds {@code allowed} accepts whose name, as the player would type it, starts with {@code input}.
     */
    public static void addWorldNames(
            String input, WorldStorageImpl worldStorage, Predicate<BuildWorld> allowed, List<String> result) {
        Function<String, String> typedName = worldStorage.typedNames();
        for (BuildWorld world : worldStorage.getBuildWorlds()) {
            if (allowed.test(world)) {
                addMatching(input, typedName.apply(world.getName()), result);
            }
        }
    }
}
