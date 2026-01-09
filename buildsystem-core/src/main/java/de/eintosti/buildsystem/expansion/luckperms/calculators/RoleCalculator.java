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
package de.eintosti.buildsystem.expansion.luckperms.calculators;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.Locale;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class RoleCalculator implements ContextCalculator<Player> {

    private static final String KEY = "buildsystem:role";

    private final WorldStorageImpl worldStorage;

    public RoleCalculator(BuildSystemPlugin plugin) {
        this.worldStorage = plugin.getWorldService().getWorldStorage();
    }

    @Override
    public void calculate(Player player, ContextConsumer contextConsumer) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        contextConsumer.accept(KEY, Role.matchRole(player, buildWorld).toString());
    }

    @Override
    public ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        for (Role role : Role.values()) {
            builder.add(KEY, role.toString());
        }
        return builder.build();
    }

    @NullMarked
    private enum Role {

        /**
         * The creator of a {@link BuildWorld}.
         */
        CREATOR,

        /**
         * A player which has been added to the list of trusted players and is therefore allowed to build in a {@link BuildWorld}.
         */
        BUILDER,

        /**
         * A player which is neither the {@link #CREATOR} nor a {@link #BUILDER} in a {@link BuildWorld}.
         */
        GUEST;

        public static Role matchRole(Player player, @Nullable BuildWorld buildWorld) {
            if (buildWorld == null) {
                return GUEST;
            }

            Builders builders = buildWorld.getBuilders();
            if (builders.isCreator(player)) {
                return CREATOR;
            } else if (builders.isBuilder(player.getUniqueId())) {
                return BUILDER;
            } else {
                return GUEST;
            }
        }

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}