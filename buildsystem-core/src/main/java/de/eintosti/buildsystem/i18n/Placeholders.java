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
package de.eintosti.buildsystem.i18n;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * The substitutions applied to a message, as an immutable {@code %token%} &rarr; value set.
 *
 * <p>Substitution is a literal replace, so a token the message declares but this set omits reaches the player as
 * literal text. Every token a message uses must be supplied.
 */
@NullMarked
public final class Placeholders {

    private static final Placeholders NONE = new Placeholders(Map.of());

    private final Map<String, Object> values;

    private Placeholders(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * {@return an empty set, for a message with no substitutions}
     */
    public static Placeholders none() {
        return NONE;
    }

    /**
     * {@return a builder for assembling a set}
     */
    public static Builder of() {
        return new Builder();
    }

    /**
     * {@return a set holding just the given substitution} Shorthand for the common single-token message.
     *
     * @param token The token to replace, including its {@code %} delimiters
     * @param value The value to substitute, rendered with {@link String#valueOf}
     */
    public static Placeholders of(String token, Object value) {
        return new Placeholders(Map.of(token, value));
    }

    /**
     * Replaces every token in this set that occurs in the given text.
     *
     * @param text The text to substitute into
     * @return The substituted text
     */
    public String applyTo(String text) {
        String result = text;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            result = result.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * Assembles a {@link Placeholders} set. Tokens keep insertion order, and adding one twice keeps the later value.
     */
    public static final class Builder {

        private final Map<String, Object> values = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Adds a substitution.
         *
         * @param token The token to replace, including its {@code %} delimiters
         * @param value The value to substitute, rendered with {@link String#valueOf}
         * @return This builder
         */
        public Builder add(String token, Object value) {
            values.put(token, value);
            return this;
        }

        /**
         * {@return the assembled, immutable set}
         */
        public Placeholders build() {
            return values.isEmpty() ? NONE : new Placeholders(new LinkedHashMap<>(values));
        }
    }
}
