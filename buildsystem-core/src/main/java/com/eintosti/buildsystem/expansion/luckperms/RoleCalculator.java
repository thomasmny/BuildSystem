/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.expansion.luckperms;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.CraftBuildWorld;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

/**
 * @author einTosti
 */
public class RoleCalculator implements ContextCalculator<Player> {

    private static final String KEY = "buildsystem:role";

    private final WorldManager worldManager;

    public RoleCalculator(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
    }

    @Override
    public void calculate(@NonNull Player player, @NonNull ContextConsumer contextConsumer) {
        CraftBuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld());
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

    private enum Role {
        CREATOR,
        BUILDER,
        GUEST;

        public static Role matchRole(Player player, CraftBuildWorld buildWorld) {
            if (buildWorld == null) {
                return GUEST;
            }

            UUID playerUuid = player.getUniqueId();

            if (buildWorld.getCreatorId().equals(playerUuid)) {
                return CREATOR;
            } else if (buildWorld.isBuilder(playerUuid)) {
                return BUILDER;
            } else {
                return GUEST;
            }
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
