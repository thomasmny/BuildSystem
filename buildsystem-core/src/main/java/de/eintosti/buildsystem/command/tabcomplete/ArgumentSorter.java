/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.command.tabcomplete;

import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
abstract class ArgumentSorter {

    public void addArgument(String input, String argument, List<String> arrayList) {
        if (input.isEmpty() || argument.toLowerCase().startsWith(input.toLowerCase())) {
            arrayList.add(argument);
        }
    }
}