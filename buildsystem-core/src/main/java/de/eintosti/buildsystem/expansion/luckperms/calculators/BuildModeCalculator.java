/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.expansion.luckperms.calculators;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.player.PlayerManager;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class BuildModeCalculator implements ContextCalculator<Player> {

    private static final String KEY = "buildsystem:build-mode";

    private final PlayerManager playerManager;

    public BuildModeCalculator(BuildSystem plugin) {
        this.playerManager = plugin.getPlayerManager();
    }

    @Override
    public void calculate(@NonNull Player player, @NonNull ContextConsumer contextConsumer) {
        boolean isInBuildMode = playerManager.isInBuildMode(player);
        contextConsumer.accept(KEY, String.valueOf(isInBuildMode));
    }

    @NotNull
    @Override
    public ContextSet estimatePotentialContexts() {
        return ImmutableContextSet.builder()
                .add(KEY, "true")
                .add(KEY, "false")
                .build();
    }
}