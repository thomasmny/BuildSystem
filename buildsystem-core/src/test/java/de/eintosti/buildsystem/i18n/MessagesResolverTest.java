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

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the external-placeholder seam: a resolver is applied only when both a resolver is registered and the audience is a player. The player-present application path is covered by
 * {@code PlaceholderApiExpansionTest} plus the compile gate; here we guard the pass-through (cleared / non-player) branches that the refactor introduced.
 */
@NullMarked
class MessagesResolverTest {

    @Test
    void noResolverLeavesTextUnchanged() {
        assertEquals("hello %player%", Messages.applyResolver(null, null, "hello %player%"));
    }

    @Test
    void nullPlayerSkipsResolverEvenWhenRegistered() {
        TextResolver shouting = (player, text) -> text.toUpperCase();
        assertEquals("hello", Messages.applyResolver(shouting, null, "hello"));
    }
}
