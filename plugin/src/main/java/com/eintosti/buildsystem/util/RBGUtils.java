/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author CoasterFreakDE (Original Author)
 * @author einTosti (Ported to Java)
 */
public class RBGUtils {

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

        text = parseFormat1(text);
        text = parseFormat2(text);
        text = parseFormat3(text);

        return text;
    }

    //&#RRGGBB
    private static String parseFormat1(String input) {
        return input.replace("&#", "#");
    }

    //{#RRGGBB}
    private static String parseFormat2(String input) {
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
    private static String parseFormat3(String input) {
        String text = input.replace('\u00a7', '&');

        Matcher matcher = SPIGOT_HEX.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group();
            String fixed = hexCode.substring(3).replace("&", "");
            text = text.replace(hexCode, "#" + fixed);
        }

        return text;
    }
}
