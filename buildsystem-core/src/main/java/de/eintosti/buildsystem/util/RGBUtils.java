/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CoasterFreakDE (Original Author)
 * @author einTosti (Ported to Java)
 */
public class RGBUtils {

    private static final Pattern DEFAULT_HEX = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern BRACKET_HEX = Pattern.compile("\\{#[0-9a-fA-F]{6}}");
    private static final Pattern SPIGOT_HEX = Pattern.compile("&x[&0-9a-fA-F]{12}");

    private static String toChatColor(String hexCode) {
        StringBuilder magic = new StringBuilder("§x");
        char[] colorChars = hexCode.substring(1).toCharArray();

        for (char c : colorChars) {
            magic.append('§').append(c);
        }

        return magic.toString();
    }

    public static String color(String input) {
        String text = applyFormats(input);

        Matcher matcher = DEFAULT_HEX.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group();
            text = text.replace(hexCode, toChatColor(hexCode));
        }

        return text;
    }

    private static String applyFormats(String input) {
        String text = input;

        text = parseDefaultFormat(text);
        text = parseBracketFormat(text);
        text = parseSpigotFormat(text);

        return text;
    }

    //&#RRGGBB
    private static String parseDefaultFormat(String input) {
        return input.replace("&#", "#");
    }

    //{#RRGGBB}
    private static String parseBracketFormat(String input) {
        String text = input;
        Matcher matcher = BRACKET_HEX.matcher(text);

        while (matcher.find()) {
            String hexCode = matcher.group();
            String fixed = hexCode.substring(2, 8);
            text = text.replace(hexCode, "#" + fixed);
        }

        return text;
    }

    //&x&R&R&G&G&B&B
    private static String parseSpigotFormat(String input) {
        String text = input.replace('§', '&');

        Matcher matcher = SPIGOT_HEX.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group();
            String fixed = hexCode.substring(3).replace("&", "");
            text = text.replace(hexCode, "#" + fixed);
        }

        return text;
    }
}