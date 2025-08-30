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
package de.eintosti.buildsystem.expansion.luckperms.calculators;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildModeCalculator implements ContextCalculator<Player> {

    private static final String KEY = "buildsystem:build-mode";

    private final PlayerServiceImpl playerManager;

    public BuildModeCalculator(BuildSystemPlugin plugin) {
        this.playerManager = plugin.getPlayerService();
    }

    @Override
    public void calculate(Player player, ContextConsumer contextConsumer) {
        boolean isInBuildMode = playerManager.isInBuildMode(player);
        contextConsumer.accept(KEY, String.valueOf(isInBuildMode));
    }

    @Override
    public ContextSet estimatePotentialContexts() {
        return ImmutableContextSet.builder()
                .add(KEY, "true")
                .add(KEY, "false")
                .build();
    }
}