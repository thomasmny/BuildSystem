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