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
package de.eintosti.buildsystem.world.download;

import org.jspecify.annotations.NullMarked;

/**
 * Renders the animated bar shown while a world is being packed.
 *
 * <p>The animation is carried by a highlight sweeping through the filled part of the bar and a spinner beside it, so
 * an export that is briefly stuck on one large region file still looks alive. Both are driven by the caller's frame
 * counter rather than wall-clock time, keeping the output a pure function of {@code (fraction, frame)}.
 */
@NullMarked
public final class ExportProgressBar {

    private static final int SEGMENTS = 20;
    private static final char FILLED = '▬';
    private static final char EMPTY = '▬';
    private static final String FILLED_COLOR = "&a";
    private static final String HIGHLIGHT_COLOR = "&f";
    private static final String EMPTY_COLOR = "&8";

    /**
     * Frames of the spinner. Plain ASCII, because the client's default font renders it everywhere, which is not true
     * of the braille and circle glyphs usually used for spinners.
     *
     * <p>Every frame must be the same width. The default font is variable-width and the action bar is centered, so a
     * narrower frame shifts the whole line sideways for as long as it shows: these three all advance six pixels, while
     * the {@code |} that would complete the rotation advances two and made the message jitter.
     */
    private static final char[] SPINNER = {'/', '-', '\\'};

    private ExportProgressBar() {}

    /**
     * @param fraction How much of the export is done, from {@code 0} to {@code 1}
     * @param frame The animation frame, incremented once per update
     * @return The legacy-colored bar
     */
    public static String bar(double fraction, int frame) {
        int filled = (int) Math.round(clamp(fraction) * SEGMENTS);
        int highlight = filled == 0 ? -1 : Math.floorMod(frame, filled);

        StringBuilder bar = new StringBuilder(SEGMENTS * 3);
        for (int segment = 0; segment < SEGMENTS; segment++) {
            if (segment >= filled) {
                bar.append(EMPTY_COLOR).append(EMPTY);
            } else if (segment == highlight) {
                bar.append(HIGHLIGHT_COLOR).append(FILLED);
            } else {
                bar.append(FILLED_COLOR).append(FILLED);
            }
        }
        return bar.toString();
    }

    public static char spinner(int frame) {
        return SPINNER[Math.floorMod(frame, SPINNER.length)];
    }

    public static int percent(double fraction) {
        return (int) Math.round(clamp(fraction) * 100);
    }

    /**
     * {@return the percentage padded to a fixed three characters} Keeps the line from jumping as the number grows a
     * digit. A space is narrower than a digit, so this shrinks the shift rather than removing it.
     */
    public static String percentText(double fraction) {
        return "%3d".formatted(percent(fraction));
    }

    private static double clamp(double fraction) {
        if (Double.isNaN(fraction)) {
            return 0;
        }
        return Math.clamp(fraction, 0, 1);
    }
}
