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
package de.eintosti.buildsystem.api.data;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Capability} that marks a {@link Type} as being overridable by an external source.
 *
 * @param <T>       The type of the value being overridden
 * @param isEnabled A supplier that returns {@code true} if the override is active
 * @param provider  A supplier that returns the override value or {@code null} if no override is set
 * @since 3.0.1
 */
@NullMarked
public record Overridable<T>(
        BooleanSupplier isEnabled,
        Supplier<@Nullable T> provider
) implements Capability {

}