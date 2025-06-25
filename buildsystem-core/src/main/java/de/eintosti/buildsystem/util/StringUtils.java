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
package de.eintosti.buildsystem.util;

/**
 * <p>Operations on {@link java.lang.String} that are
 * {@code null} safe.</p>
 *
 * <ul>
 *  <li><b>Difference</b>
 *      - compares Strings and reports on their differences</li>
 * </ul>
 *
 * <p>The {@code StringUtils} class defines certain words related to
 * String handling.</p>
 *
 * <ul>
 *  <li>null - {@code null}</li>
 *  <li>empty - a zero-length string ({@code ""})</li>
 *  <li>space - the space character ({@code ' '}, char 32)</li>
 *  <li>whitespace - the characters defined by {@link Character#isWhitespace(char)}</li>
 *  <li>trim - the characters &lt;= 32 as in {@link String#trim()}</li>
 * </ul>
 *
 * <p>{@code StringUtils} handles {@code null} input Strings quietly.
 * That is to say that a {@code null} input will return {@code null}.
 * Where a {@code boolean} or {@code int} is being returned
 * details vary by method.</p>
 *
 * <p>A side effect of the {@code null} handling is that a
 * {@code NullPointerException} should be considered a bug in
 * {@code StringUtils}.</p>
 *
 * <p>Methods in this class include sample code in their Javadoc comments to explain their operation.
 * The symbol {@code *} is used to indicate any input including {@code null}.</p>
 *
 * <p>#ThreadSafe#</p>
 *
 * @see java.lang.String
 * @since 1.0
 */
//@Immutable
public class StringUtils {

    /**
     * <p>Case-insensitive check if a CharSequence starts with a specified prefix.</p>
     *
     * <p>{@code null}s are handled without exceptions. Two {@code null}
     * references are considered to be equal. The comparison is case-insensitive.</p>
     *
     * <pre>
     * StringUtils.startsWithIgnoreCase(null, null)      = true
     * StringUtils.startsWithIgnoreCase(null, "abc")     = false
     * StringUtils.startsWithIgnoreCase("abcdef", null)  = false
     * StringUtils.startsWithIgnoreCase("abcdef", "abc") = true
     * StringUtils.startsWithIgnoreCase("ABCDEF", "abc") = true
     * </pre>
     *
     * @param str    the CharSequence to check, may be null
     * @param prefix the prefix to find, may be null
     * @return {@code true} if the CharSequence starts with the prefix, case-insensitive, or both {@code null}
     * @see java.lang.String#startsWith(String)
     * @since 2.4
     * @since 3.0 Changed signature from startsWithIgnoreCase(String, String) to startsWithIgnoreCase(CharSequence, CharSequence)
     */
    public static boolean startsWithIgnoreCase(final CharSequence str, final CharSequence prefix) {
        if (str == null || prefix == null) {
            return str == prefix;
        }
        // Get length once instead of twice in the unlikely case that it changes.
        final int preLen = prefix.length();
        if (preLen > str.length()) {
            return false;
        }
        return regionMatches(str, prefix, preLen);
    }

    /**
     * Determines if a region of a CharSequence matches another CharSequence.
     *
     * @param cs        the CharSequence to be processed
     * @param substring the CharSequence to be compared
     * @param length    the length of the region to be compared
     * @return {@code true} if the region matches the given substring, otherwise {@code false}
     */
    static boolean regionMatches(final CharSequence cs, final CharSequence substring, final int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(true, 0, (String) substring, 0, length);
        }

        return customRegionMatch(cs, substring, length);
    }

    /**
     * Checks if a region of a CharSequence matches another CharSequence, with case sensitivity.
     *
     * @param cs        the CharSequence to be checked
     * @param substring the CharSequence to compare against
     * @param length    the length of the region to check
     * @return true if the region matches, false otherwise
     */
    private static boolean customRegionMatch(final CharSequence cs, final CharSequence substring, final int length) {
        int index1 = 0;
        int index2 = 0;
        int tmpLen = length;
        // Extract these first so we detect NPEs the same as the java.lang.String version
        final int srcLen = cs.length();
        final int otherLen = substring.length();
        // Check for invalid parameters
        if (length < 0) {
            return false;
        }
        // Check that the regions are long enough
        if (srcLen < length || otherLen < length) {
            return false;
        }
        while (tmpLen-- > 0) {
            final char c1 = cs.charAt(index1++);
            final char c2 = substring.charAt(index2++);
            if (c1 != c2) {
                if (Character.toUpperCase(c1) != Character.toUpperCase(c2)
                        && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                    return false;
                }
            }
        }
        return true;
    }
}