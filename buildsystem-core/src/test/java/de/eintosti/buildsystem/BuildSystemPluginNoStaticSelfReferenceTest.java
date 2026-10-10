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
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Guards against bringing back a static singleton on {@link BuildSystemPlugin}: services get the plugin passed in, so
 * no static field may hold it and no static method may hand it out.
 */
@NullMarked
class BuildSystemPluginNoStaticSelfReferenceTest {

    @Test
    void noStaticFieldHoldsThePlugin() {
        for (Field field : BuildSystemPlugin.class.getDeclaredFields()) {
            assertFalse(
                    Modifier.isStatic(field.getModifiers()) && field.getType() == BuildSystemPlugin.class,
                    field.toString());
        }
    }

    @Test
    void noStaticMethodReturnsThePlugin() {
        for (Method method : BuildSystemPlugin.class.getDeclaredMethods()) {
            assertFalse(
                    Modifier.isStatic(method.getModifiers()) && method.getReturnType() == BuildSystemPlugin.class,
                    method.toString());
        }
    }
}
