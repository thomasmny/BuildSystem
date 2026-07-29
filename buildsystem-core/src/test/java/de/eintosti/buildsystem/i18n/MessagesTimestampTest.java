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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.config.ConfigService;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Timestamp rendering: the formatters are cached per configured pattern, so this pins that a reload picks a new pattern
 * up and that an unusable one degrades instead of throwing on every render.
 */
@NullMarked
class MessagesTimestampTest {

    private static final long TIMESTAMP = 1_700_000_000_000L;

    private ConfigService configService;
    private Messages messages;

    @BeforeEach
    void setUp() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        usePattern("dd/MM/yyyy");
        messages = new Messages(plugin, configService);
    }

    private void usePattern(String pattern) {
        when(configService.current().settings().dateFormat()).thenReturn(pattern);
    }

    @Test
    void formatDateUsesTheConfiguredPattern() {
        assertEquals(10, messages.formatDate(TIMESTAMP).length(), "dd/MM/yyyy renders as 10 characters");
    }

    @Test
    void formatDateTimeAppendsTheTimeOfDay() {
        String date = messages.formatDate(TIMESTAMP);
        String dateTime = messages.formatDateTime(TIMESTAMP);

        assertTrue(dateTime.startsWith(date), dateTime + " should start with " + date);
        assertEquals(date.length() + " HH:mm:ss".length(), dateTime.length());
    }

    @Test
    void noTimestampRendersAsDash() {
        assertEquals("-", messages.formatDate(0));
        assertEquals("-", messages.formatDate(-1));
        assertEquals("-", messages.formatDateTime(0));
    }

    @Test
    void aChangedPatternIsPickedUpWithoutARestart() {
        assertEquals(10, messages.formatDate(TIMESTAMP).length());

        usePattern("yyyy");

        assertEquals(4, messages.formatDate(TIMESTAMP).length(), "the cache must not pin the old pattern");
    }

    @Test
    void anUnusablePatternFallsBackInsteadOfThrowing() {
        usePattern("not a pattern");

        assertDoesNotThrow(() -> messages.formatDate(TIMESTAMP));
        assertEquals(10, messages.formatDate(TIMESTAMP).length(), "falls back to dd/MM/yyyy");
    }
}
