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
package de.eintosti.buildsystem.util.color;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import net.md_5.bungee.api.ChatColor;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ColorAPITest {

    @Test
    void gradient_oneCharacter_usesTheStartColour() {
        String result = ColorAPI.process("<GRADIENT:ff0000>a</GRADIENT:0000ff>");

        assertEquals(ChatColor.of(new Color(0xff0000)) + "a", result);
    }

    @Test
    void gradient_runsFromStartToEnd() {
        String result = ColorAPI.color("abc", new Color(0x000000), new Color(0x0000c8));

        assertEquals(
                ChatColor.of(new Color(0x000000)) + "a"
                        + ChatColor.of(new Color(0x000064)) + "b"
                        + ChatColor.of(new Color(0x0000c8)) + "c",
                result);
    }
}
