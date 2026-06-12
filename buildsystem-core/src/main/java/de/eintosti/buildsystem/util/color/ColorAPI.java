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

import de.eintosti.buildsystem.util.color.patterns.GradientPattern;
import de.eintosti.buildsystem.util.color.patterns.HexPattern;
import de.eintosti.buildsystem.util.color.patterns.RainbowPattern;
import de.eintosti.buildsystem.util.color.patterns.SolidPattern;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ColorAPI {

    private static final List<String> SPECIAL_COLORS =
            List.of("&l", "&n", "&o", "&k", "&m", "§l", "§n", "§o", "§k", "§m");

    private static final List<ColorPattern> PATTERNS =
            List.of(new GradientPattern(), new HexPattern(), new SolidPattern(), new RainbowPattern());

    /**
     * Processes a string to add color to it. Thanks to Distressing for helping with the regex <3
     *
     * @param string The string we want to process
     */
    public static String process(String string) {
        for (ColorPattern pattern : PATTERNS) {
            string = pattern.process(string);
        }
        string = ChatColor.translateAlternateColorCodes('&', string);
        return string;
    }

    /**
     * Processes multiple strings in a collection.
     *
     * @param strings The collection of the strings we are processing
     * @return The list of processed strings
     */
    @Unmodifiable
    public static List<String> process(Collection<String> strings) {
        return strings.stream().map(ColorAPI::process).toList();
    }

    /**
     * Colors a String with a gradiant.
     *
     * @param string The string we want to color
     * @param start  The starting gradiant
     * @param end    The ending gradiant
     */
    public static String color(String string, Color start, Color end) {
        ChatColor[] colors =
                createGradient(start, end, withoutSpecialChar(string).length());
        return apply(string, colors);
    }

    /**
     * Colors a String with rainbow colors.
     *
     * @param string     The string which should have rainbow colors
     * @param saturation The saturation of the rainbow colors
     */
    public static String rainbow(String string, float saturation) {
        ChatColor[] colors = createRainbow(withoutSpecialChar(string).length(), saturation);
        return apply(string, colors);
    }

    /**
     * Gets a color from hex code.
     *
     * @param string The hex code of the color
     */
    public static ChatColor getColor(String string) {
        return ChatColor.of(new Color(Integer.parseInt(string, 16)));
    }

    private static String apply(String source, ChatColor[] colors) {
        StringBuilder specialColors = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();

        String[] characters = source.split("");
        int outIndex = 0;

        for (int i = 0; i < characters.length; i++) {
            if (characters[i].equals("&") || characters[i].equals("§")) {
                if (i + 1 < characters.length) {
                    if (characters[i + 1].equals("r")) {
                        specialColors.setLength(0);
                    } else {
                        specialColors.append(characters[i]);
                        specialColors.append(characters[i + 1]);
                    }
                    i++;
                } else {
                    stringBuilder
                            .append(colors[outIndex++])
                            .append(specialColors)
                            .append(characters[i]);
                }
            } else {
                stringBuilder.append(colors[outIndex++]).append(specialColors).append(characters[i]);
            }
        }

        return stringBuilder.toString();
    }

    private static String withoutSpecialChar(String source) {
        String workingString = source;
        for (String color : SPECIAL_COLORS) {
            if (workingString.contains(color)) {
                workingString = workingString.replace(color, "");
            }
        }
        return workingString;
    }

    /**
     * Returns a rainbow array of chat colors.
     *
     * @param step       How many colors we return
     * @param saturation The saturation of the rainbow
     * @return The array of colors
     */
    private static ChatColor[] createRainbow(int step, float saturation) {
        ChatColor[] colors = new ChatColor[step];
        double colorStep = (1.00 / step);

        for (int i = 0; i < step; i++) {
            Color color = Color.getHSBColor((float) (colorStep * i), saturation, saturation);
            colors[i] = ChatColor.of(color);
        }

        return colors;
    }

    /**
     * Returns a gradient array of chat colors.
     *
     * @param start The starting color.
     * @param end   The ending color.
     * @param step  How many colors we return.
     * @author TheViperShow
     */
    private static ChatColor[] createGradient(Color start, Color end, int step) {
        ChatColor[] colors = new ChatColor[step];
        int stepR = Math.abs(start.getRed() - end.getRed()) / (step - 1);
        int stepG = Math.abs(start.getGreen() - end.getGreen()) / (step - 1);
        int stepB = Math.abs(start.getBlue() - end.getBlue()) / (step - 1);
        int[] direction = new int[] {
            start.getRed() < end.getRed() ? +1 : -1,
            start.getGreen() < end.getGreen() ? +1 : -1,
            start.getBlue() < end.getBlue() ? +1 : -1
        };

        for (int i = 0; i < step; i++) {
            Color color = new Color(
                    start.getRed() + ((stepR * i) * direction[0]),
                    start.getGreen() + ((stepG * i) * direction[1]),
                    start.getBlue() + ((stepB * i) * direction[2]));
            colors[i] = ChatColor.of(color);
        }

        return colors;
    }
}
