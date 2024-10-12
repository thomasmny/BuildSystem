package de.eintosti.buildsystem.util.color.patterns;

import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.util.color.ColorPattern;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SolidPattern implements ColorPattern {

    private static final Pattern PATTERN = Pattern.compile("<SOLID:([0-9A-Fa-f]{6})>|#\\{([0-9A-Fa-f]{6})}");

    @Override
    public String process(String string) {
        Matcher matcher = PATTERN.matcher(string);
        while (matcher.find()) {
            String color = matcher.group(1);
            if (color == null) {
                color = matcher.group(2);
            }

            string = string.replace(matcher.group(), ColorAPI.getColor(color) + "");
        }
        return string;
    }
}