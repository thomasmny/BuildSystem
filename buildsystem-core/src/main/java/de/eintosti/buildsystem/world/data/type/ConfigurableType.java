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
package de.eintosti.buildsystem.world.data.type;

import de.eintosti.buildsystem.api.data.Capability;
import de.eintosti.buildsystem.api.data.Overridable;
import de.eintosti.buildsystem.api.data.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * A single, concrete implementation of {@link Type} that uses a composition-based {@link Capability} model.
 *
 * @param <T> The type of the value held.
 */
@NullMarked
public class ConfigurableType<T> implements Type<T> {

    private T value;
    private Function<T, Object> configFormatter = (value) -> (Object) value;

    private final Map<Class<? extends Capability>, Capability> capabilities = new HashMap<>();

    /**
     * Creates a simple type.
     */
    public ConfigurableType(T defaultValue) {
        this.value = defaultValue;
    }

    /**
     * Sets a custom config formatter for this type.
     *
     * @param configFormatter A function that formats the value for config storage
     * @return This object, for fluent chaining
     */
    public ConfigurableType<T> withConfigFormatter(Function<T, Object> configFormatter) {
        this.configFormatter = configFormatter;
        return this;
    }

    /**
     * Attaches a new capability to this type.
     *
     * @param capabilityType     The class of the capability
     * @param capabilityInstance The instance of the capability
     * @return This object, for fluent chaining
     */
    public <C extends Capability> ConfigurableType<T> withCapability(Class<C> capabilityType, C capabilityInstance) {
        this.capabilities.put(capabilityType, capabilityInstance);
        return this;
    }

    /**
     * Checks if this type has a specific capability.
     *
     * @param capability The class of the capability
     * @return {@code true} if the capability is present, {@code false} otherwise
     */
    public boolean hasCapability(Class<? extends Capability> capability) {
        return this.capabilities.containsKey(capability);
    }

    /**
     * Gets the capability instance if it exists.
     *
     * @param clazz The class of the capability
     * @return An optional containing the capability, or empty
     */
    public <C extends Capability> Optional<C> getCapability(Class<C> clazz) {
        return Optional.ofNullable(this.capabilities.get(clazz)).map(clazz::cast);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public T get() {
        Optional<T> override = getCapability(Overridable.class)
                .map(raw -> (Overridable<T>) raw)
                .filter(overridable -> overridable.isEnabled().getAsBoolean())
                .map(overridable -> overridable.provider().get());

        return override.orElse(this.value);
    }

    /**
     * Sets the base value for this type.
     * <p>
     * Note: This sets the underlying value. If an {@link Overridable} capability is active, {@link #get()} will still return the overridden value.
     *
     * @param value The new base value
     */
    @Override
    public void set(T value) {
        this.value = value;
    }

    @Override
    public Object getConfigFormat() {
        return this.configFormatter.apply(this.get());
    }
}