/*
 * Copyright (c) 2018-2024, Thomas Meaney
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
package de.eintosti.buildsystem.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class ArgumentParser {

    private final List<String> args;
    private final Map<String, List<String>> map;
    private final Set<String> flags;

    public ArgumentParser(String[] arguments) {
        this.args = Arrays.asList(arguments);
        this.map = new HashMap<>();
        this.flags = new HashSet<>();
        map();
    }

    /**
     * Gets whether the provided string is used as an argument.<br> Useful for figuring out if an argument has a value.
     *
     * @param name The name of the argument
     * @return {@code true} if the argument is present, otherwise {@code false}
     */
    public boolean isArgument(String name) {
        return args.stream()
                .map(arg -> arg.replace("-", ""))
                .anyMatch(name::equalsIgnoreCase);
    }

    /**
     * Gets whether the flag is present in the array of arguments. A flag is a true or false argument with no value.
     * <p>
     * For example: {@code -nogui}
     *
     * @param name The name of the flag
     * @return {@code true} if the flag is present, otherwise {@code false}
     */
    public boolean getFlag(String name) {
        return flags.contains(name);
    }

    /**
     * Gets the value of an argument.
     * <p>
     * For example: {@code -name XYZ}
     *
     * @param name The name of the argument
     * @return The argument value if present, otherwise {@code null}.
     */
    @Nullable
    public String getValue(String name) {
        List<String> value = map.get(name);
        if (value != null) {
            return String.join(" ", value);
        }
        return null;
    }

    /**
     * Maps the arguments passed to the ArgumentParser. Arguments starting with "-" are considered flags, and their
     * presence is added to the 'flags' set. Arguments without "-" are considered argument names, and their values are
     * stored in the 'map' HashMap.
     */
    public void map() {
        for (int index = 0; index < args.size(); index++) {
            String arg = args.get(index);
            if (!arg.startsWith("-")) {
                continue;
            }
            if (isFlagArgument(arg, index)) {
                flags.add(arg.replace("-", ""));
            } else {
                map.put(arg.replace("-", ""), storeArgumentValues(arg, index));
            }
        }
    }

    /**
     * Determines whether the provided argument is a flag argument. A flag argument is an argument that is either the
     * last argument in the list or is followed by another argument that starts with a dash (-).
     *
     * @param arg   The argument to check
     * @param index The index of the argument in the argument list
     * @return {@code true} if the argument is a flag argument, otherwise {@code false}
     */
    private boolean isFlagArgument(String arg, int index) {
        return index == (args.size() - 1) || args.get(index + 1).startsWith("-");
    }

    /**
     * Stores the argument values for a given argument name.
     *
     * @param arg   The argument name
     * @param index The index of the argument in the list of arguments
     * @return The argument values as a list, or an empty list if no values are found
     */
    private List<String> storeArgumentValues(String arg, int index) {
        List<String> argumentValues = new ArrayList<>();
        int i = index + 1;
        while (i != args.size() && !args.get(i).startsWith("-")) {
            argumentValues.add(args.get(i));
            i++;
        }
        return argumentValues;
    }
}