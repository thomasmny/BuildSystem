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
package de.eintosti.buildsystem.world.backup.storage.s3;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.NullMarked;

/**
 * RFC 3986 percent-encoding, as SigV4 requires it.
 *
 * <p>{@code URLEncoder} cannot be used: it encodes a space as {@code +} and leaves {@code *} alone, and either
 * difference makes the signature disagree with the request.
 */
@NullMarked
final class PercentEncoding {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private PercentEncoding() {}

    /**
     * {@return the value percent-encoded, with only the RFC 3986 unreserved characters left as-is}
     *
     * @param value The value to encode
     */
    static String encode(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int unsigned = b & 0xFF;
            if (isUnreserved((char) unsigned)) {
                encoded.append((char) unsigned);
            } else {
                encoded.append('%').append(HEX[unsigned >> 4]).append(HEX[unsigned & 0xF]);
            }
        }
        return encoded.toString();
    }

    /**
     * {@return the key encoded for use as a path} Slashes survive, since they separate the key's own segments rather
     * than being part of one.
     *
     * @param key The object key
     */
    static String encodePath(String key) {
        StringBuilder encoded = new StringBuilder(key.length());
        for (String segment : key.split("/", -1)) {
            if (!encoded.isEmpty()) {
                encoded.append('/');
            }
            encoded.append(encode(segment));
        }
        return encoded.toString();
    }

    /**
     * {@return the parameters as a query string, sorted and encoded the way SigV4 canonicalises them} The same string
     * is both signed and sent, so a request can never disagree with its own signature.
     *
     * @param parameters The query parameters
     */
    static String query(Map<String, String> parameters) {
        StringBuilder query = new StringBuilder();
        new TreeMap<>(parameters).forEach((name, value) -> {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(encode(name)).append('=').append(encode(value));
        });
        return query.toString();
    }

    private static boolean isUnreserved(char c) {
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '-'
                || c == '_'
                || c == '.'
                || c == '~';
    }
}
