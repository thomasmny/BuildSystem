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
package de.eintosti.buildsystem.util.color.patterns;

import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.util.color.ColorPattern;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RainbowPattern implements ColorPattern {

    private static final Pattern PATTERN = Pattern.compile("<RAINBOW([0-9]{1,3})>(.*?)</RAINBOW>");

    @Override
    public String process(String string) {
        Matcher matcher = PATTERN.matcher(string);
        while (matcher.find()) {
            String saturation = matcher.group(1);
            String content = matcher.group(2);
            string = string.replace(matcher.group(), ColorAPI.rainbow(content, Float.parseFloat(saturation)));
        }
        return string;
    }
}