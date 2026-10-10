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
package de.eintosti.buildsystem.world.creation;

import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.world.WorldClock;
import de.eintosti.buildsystem.world.menu.GameRuleEntry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The settings a world gets once, when it is created or imported: the configured difficulty, time, border and game
 * rules, and a spawn for void and flat worlds. A world that is only loaded again keeps whatever it has, so changes made
 * since it was created survive an unload.
 */
@NullMarked
final class WorldDefaults {

    private static final int VOID_BLOCK_Y = 64;

    private final Logger logger;
    private final ConfigService configService;
    private final @Nullable Difficulty difficulty;
    private final @Nullable Integer time;
    private final @Nullable Integer worldBorderSize;

    WorldDefaults(
            Logger logger,
            ConfigService configService,
            @Nullable Difficulty difficulty,
            @Nullable Integer time,
            @Nullable Integer worldBorderSize) {
        this.logger = logger;
        this.configService = configService;
        this.difficulty = difficulty;
        this.time = time;
        this.worldBorderSize = worldBorderSize;
    }

    /**
     * Applies the defaults to a world that was just generated. A default that fails is logged and the rest still apply,
     * since the world is already registered and loaded by now.
     *
     * @param generatedType The type the world was generated as
     * @param newlyGenerated Whether the world was generated from scratch rather than imported. Only then is the
     *     configured block placed under a void world's spawn.
     */
    void apply(World world, BuildWorldType generatedType, boolean newlyGenerated) {
        if (difficulty != null) {
            attempt(world, "difficulty", () -> world.setDifficulty(difficulty));
        }
        if (time != null) {
            attempt(world, "time", () -> WorldClock.trySetTime(world, time));
        }
        if (worldBorderSize != null) {
            attempt(world, "world border", () -> world.getWorldBorder().setSize(worldBorderSize));
        }
        for (GameRuleEntry<?> gameRule :
                configService.current().world().defaults().gameRules()) {
            attempt(world, "game rule " + gameRule.rule(), () -> applyGameRule(world, gameRule));
        }
        attempt(world, "spawn", () -> applySpawn(world, generatedType, newlyGenerated));
    }

    private void attempt(World world, String setting, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Could not apply the default " + setting + " to world " + world.getName(), e);
        }
    }

    private static <T> void applyGameRule(World world, GameRuleEntry<T> entry) {
        entry.rule().setValue(world, entry.value());
    }

    void applySpawn(World world, BuildWorldType generatedType, boolean newlyGenerated) {
        switch (generatedType) {
            case VOID -> {
                if (newlyGenerated) {
                    placeVoidBlock(world);
                }
                world.setSpawnLocation(0, VOID_BLOCK_Y + 1, 0);
            }
            case FLAT -> world.setSpawnLocation(0, -60, 0);
            default -> {
                // Other types keep the spawn the generator chose.
            }
        }
    }

    /**
     * Places the configured void block at the spawn column. Only fills air: a block already there, from a world folder
     * that existed before, is never overwritten.
     */
    private void placeVoidBlock(World world) {
        PluginConfig.World.VoidBlock voidBlock = configService.current().world().voidBlock();
        if (!voidBlock.enabled()) {
            return;
        }

        Block block = world.getBlockAt(0, VOID_BLOCK_Y, 0);
        if (block.getType().isAir()) {
            block.setType(voidBlock.material());
        }
    }
}
