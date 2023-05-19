/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.util;

/**
 * <p>Provides extra functionality for Java Number classes.</p>
 *
 * @since 2.0
 */
public class NumberUtils {

    /**
     * <p>{@code NumberUtils} instances should NOT be constructed in standard programming.
     * Instead, the class should be used as {@code NumberUtils.toInt("6");}.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
     */
    public NumberUtils() {
    }

    /**
     * <p>Convert a {@code String} to an {@code int}, returning
     * {@code zero} if the conversion fails.</p>
     *
     * <p>If the string is {@code null}, {@code zero} is returned.</p>
     *
     * <pre>
     *   NumberUtils.toInt(null) = 0
     *   NumberUtils.toInt("")   = 0
     *   NumberUtils.toInt("1")  = 1
     * </pre>
     *
     * @param str the string to convert, may be null
     * @return the int represented by the string, or {@code zero} if
     * conversion fails
     * @since 2.1
     */
    public static int toInt(final String str) {
        return toInt(str, 0);
    }

    /**
     * <p>Convert a {@code String} to an {@code int}, returning a
     * default value if the conversion fails.</p>
     *
     * <p>If the string is {@code null}, the default value is returned.</p>
     *
     * <pre>
     *   NumberUtils.toInt(null, 1) = 1
     *   NumberUtils.toInt("", 1)   = 1
     *   NumberUtils.toInt("1", 0)  = 1
     * </pre>
     *
     * @param str          the string to convert, may be null
     * @param defaultValue the default value
     * @return the int represented by the string, or the default if conversion fails
     * @since 2.1
     */
    public static int toInt(final String str, final int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (final NumberFormatException nfe) {
            return defaultValue;
        }
    }
}