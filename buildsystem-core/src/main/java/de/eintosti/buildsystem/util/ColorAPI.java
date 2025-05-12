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
package de.eintosti.buildsystem.util.color;

import com.google.common.collect.ImmutableMap;
import de.eintosti.buildsystem.util.color.patterns.GradientPattern;
import de.eintosti.buildsystem.util.color.patterns.HexPattern;
import de.eintosti.buildsystem.util.color.patterns.RainbowPattern;
import de.eintosti.buildsystem.util.color.patterns.SolidPattern;
import de.eintosti.buildsystem.version.util.MinecraftVersion;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

public final class ColorAPI {

    /**
     * Clients are able to display RGB colors after Minecraft 1.16
     */
    private static final boolean SUPPORTS_RGB = MinecraftVersion.getCurrent()
            .isEqualOrHigherThan(MinecraftVersion.NETHER_16);

    private static final Map<Color, ChatColor> COLORS = ImmutableMap.<Color, ChatColor>builder()
            .put(new Color(0), ChatColor.getByChar('0'))
            .put(new Color(170), ChatColor.getByChar('1'))
            .put(new Color(43520), ChatColor.getByChar('2'))
            .put(new Color(43690), ChatColor.getByChar('3'))
            .put(new Color(11141120), ChatColor.getByChar('4'))
            .put(new Color(11141290), ChatColor.getByChar('5'))
            .put(new Color(16755200), ChatColor.getByChar('6'))
            .put(new Color(11184810), ChatColor.getByChar('7'))
            .put(new Color(5592405), ChatColor.getByChar('8'))
            .put(new Color(5592575), ChatColor.getByChar('9'))
            .put(new Color(5635925), ChatColor.getByChar('a'))
            .put(new Color(5636095), ChatColor.getByChar('b'))
            .put(new Color(16733525), ChatColor.getByChar('c'))
            .put(new Color(16733695), ChatColor.getByChar('d'))
            .put(new Color(16777045), ChatColor.getByChar('e'))
            .put(new Color(16777215), ChatColor.getByChar('f'))
            .build();

    private static final java.util.List<String> SPECIAL_COLORS = Arrays.asList(
            "&l", "&n", "&o", "&k", "&m",
            "§l", "§n", "§o", "§k", "§m"
    );

    private static final java.util.List<ColorPattern> PATTERNS = Arrays.asList(
            new GradientPattern(),
            new HexPattern(),
            new SolidPattern(),
            new RainbowPattern()
    );

    /**
     * Processes a string to add color to it. Thanks to Distressing for helping with the regex <3
     *
     * @param string The string we want to process
     */
    @NotNull
    public static String process(@NotNull String string) {
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
    @NotNull
    public static List<String> process(@NotNull Collection<String> strings) {
        return strings.stream().map(ColorAPI::process).collect(Collectors.toList());
    }

    /**
     * Colors a String with a gradiant.
     *
     * @param string The string we want to color
     * @param start  The starting gradiant
     * @param end    The ending gradiant
     */
    @NotNull
    public static String color(@NotNull String string, @NotNull Color start, @NotNull Color end) {
        ChatColor[] colors = createGradient(start, end, withoutSpecialChar(string).length());
        return apply(string, colors);
    }

    /**
     * Colors a String with rainbow colors.
     *
     * @param string     The string which should have rainbow colors
     * @param saturation The saturation of the rainbow colors
     */
    @NotNull
    public static String rainbow(@NotNull String string, float saturation) {
        ChatColor[] colors = createRainbow(withoutSpecialChar(string).length(), saturation);
        return apply(string, colors);
    }

    /**
     * Gets a color from hex code.
     *
     * @param string The hex code of the color
     */
    @NotNull
    public static ChatColor getColor(@NotNull String string) {
        return SUPPORTS_RGB
                ? ChatColor.of(new Color(Integer.parseInt(string, 16)))
                : getClosestColor(new Color(Integer.parseInt(string, 16)));
    }

    @NotNull
    private static String apply(@NotNull String source, ChatColor[] colors) {
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
                    stringBuilder.append(colors[outIndex++]).append(specialColors).append(characters[i]);
                }
            } else {
                stringBuilder.append(colors[outIndex++]).append(specialColors).append(characters[i]);
            }
        }

        return stringBuilder.toString();
    }

    @NotNull
    private static String withoutSpecialChar(@NotNull String source) {
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
    @NotNull
    private static ChatColor[] createRainbow(int step, float saturation) {
        ChatColor[] colors = new ChatColor[step];
        double colorStep = (1.00 / step);

        for (int i = 0; i < step; i++) {
            Color color = Color.getHSBColor((float) (colorStep * i), saturation, saturation);
            if (SUPPORTS_RGB) {
                colors[i] = ChatColor.of(color);
            } else {
                colors[i] = getClosestColor(color);
            }
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
    @NotNull
    private static ChatColor[] createGradient(@NotNull Color start, @NotNull Color end, int step) {
        ChatColor[] colors = new ChatColor[step];
        int stepR = Math.abs(start.getRed() - end.getRed()) / (step - 1);
        int stepG = Math.abs(start.getGreen() - end.getGreen()) / (step - 1);
        int stepB = Math.abs(start.getBlue() - end.getBlue()) / (step - 1);
        int[] direction = new int[]{
                start.getRed() < end.getRed() ? +1 : -1,
                start.getGreen() < end.getGreen() ? +1 : -1,
                start.getBlue() < end.getBlue() ? +1 : -1
        };

        for (int i = 0; i < step; i++) {
            Color color = new Color(
                    start.getRed() + ((stepR * i) * direction[0]),
                    start.getGreen() + ((stepG * i) * direction[1]),
                    start.getBlue() + ((stepB * i) * direction[2])
            );
            colors[i] = SUPPORTS_RGB ? ChatColor.of(color) : getClosestColor(color);
        }

        return colors;
    }

    /**
     * Returns the closest legacy color from an RGB color
     *
     * @param color The color we want to transform
     */
    @NotNull
    private static ChatColor getClosestColor(Color color) {
        Color nearestColor = null;
        double nearestDistance = Integer.MAX_VALUE;

        for (Color constantColor : COLORS.keySet()) {
            double distance = Math.pow(color.getRed() - constantColor.getRed(), 2)
                    + Math.pow(color.getGreen() - constantColor.getGreen(), 2)
                    + Math.pow(color.getBlue() - constantColor.getBlue(), 2);
            if (nearestDistance > distance) {
                nearestColor = constantColor;
                nearestDistance = distance;
            }
        }

        return COLORS.get(nearestColor);
    }
}
