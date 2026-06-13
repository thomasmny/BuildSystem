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
package de.eintosti.buildsystem.world.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.eintosti.buildsystem.world.data.type.ConfigurableType;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ConfigurableTypeChangeListenerTest {

    @Test
    void set_changesValue_notifiesListener() {
        ConfigurableType<String> type = new ConfigurableType<>("old");
        AtomicInteger invocations = new AtomicInteger();
        String[] observed = new String[2];
        type.setChangeListener((oldValue, newValue) -> {
            invocations.incrementAndGet();
            observed[0] = oldValue;
            observed[1] = newValue;
        });

        type.set("new");

        assertEquals(1, invocations.get());
        assertEquals("old", observed[0]);
        assertEquals("new", observed[1]);
    }

    @Test
    void set_sameValue_doesNotNotifyListener() {
        ConfigurableType<String> type = new ConfigurableType<>("same");
        AtomicInteger invocations = new AtomicInteger();
        type.setChangeListener((oldValue, newValue) -> invocations.incrementAndGet());

        type.set("same");

        assertEquals(0, invocations.get());
    }

    @Test
    void set_withoutListener_doesNotThrow() {
        ConfigurableType<String> type = new ConfigurableType<>("old");
        type.set("new");
        assertEquals("new", type.get());
    }

    @Test
    void setChangeListener_null_deregistersListener() {
        ConfigurableType<String> type = new ConfigurableType<>("old");
        AtomicInteger invocations = new AtomicInteger();
        type.setChangeListener((oldValue, newValue) -> invocations.incrementAndGet());
        type.setChangeListener(null);

        type.set("new");

        assertEquals(0, invocations.get());
        assertEquals("new", type.get());
    }
}
