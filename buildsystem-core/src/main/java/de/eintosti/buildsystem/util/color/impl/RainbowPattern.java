package de.eintosti.buildsystem.util.color.impl;

import de.eintosti.buildsystem.util.color.IlleniumColorAPI;
import de.eintosti.buildsystem.util.color.ColorPattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RainbowPattern implements ColorPattern {

    Pattern pattern = Pattern.compile("<RAINBOW([0-9]{1,3})>(.*?)</RAINBOW>");

    /**
     * Applies a rainbow pattern to the provided String.
     * Output might me the same as the input if this pattern is not present.
     *
     * @param string The String to which this pattern should be applied to
     * @return The new String with applied pattern
     */
    public String process(String string) {
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            String saturation = matcher.group(1);
            String content = matcher.group(2);
            string = string.replace(matcher.group(), IlleniumColorAPI.rainbow(content, Float.parseFloat(saturation)));
        }
        return string;
    }
}