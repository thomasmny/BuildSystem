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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * Pins the void-block placement contract of {@link BukkitWorldFactory#applyPostGenerationSettings}: the configured
 * block is placed at the spawn column of newly generated void worlds only — never over an existing block, never when
 * disabled, and never when the world is merely re-generated (import/load/rename) — while the spawn is always set.
 */
@NullMarked
class BukkitWorldFactoryTest {

    private ServerMock server;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private BukkitWorldFactory factory(PluginConfig.World.VoidBlock voidBlock, boolean initialGeneration) {
        lenient().when(configService.current().world().voidBlock()).thenReturn(voidBlock);
        return new BukkitWorldFactory(
                configService,
                Logger.getLogger("test"),
                "void-world",
                BuildWorldType.VOID,
                null,
                null,
                null,
                null,
                null,
                initialGeneration);
    }

    @Test
    void initialGeneration_placesConfiguredBlockAndSetsSpawn() {
        WorldMock world = server.addSimpleWorld("void-world");
        assertEquals(Material.AIR, world.getBlockAt(0, 64, 0).getType());

        factory(new PluginConfig.World.VoidBlock(true, Material.DIAMOND_BLOCK), true)
                .applyPostGenerationSettings(world, BuildWorldType.VOID);

        assertEquals(Material.DIAMOND_BLOCK, world.getBlockAt(0, 64, 0).getType());
        assertEquals(65, world.getSpawnLocation().getBlockY());
    }

    @Test
    void initialGeneration_neverOverwritesAnExistingBlock() {
        WorldMock world = server.addSimpleWorld("void-world");
        world.getBlockAt(0, 64, 0).setType(Material.STONE);

        factory(new PluginConfig.World.VoidBlock(true, Material.GOLD_BLOCK), true)
                .applyPostGenerationSettings(world, BuildWorldType.VOID);

        assertEquals(Material.STONE, world.getBlockAt(0, 64, 0).getType());
    }

    @Test
    void placementDisabled_stillSetsSpawn() {
        WorldMock world = server.addSimpleWorld("void-world");

        factory(new PluginConfig.World.VoidBlock(false, Material.GOLD_BLOCK), true)
                .applyPostGenerationSettings(world, BuildWorldType.VOID);

        assertEquals(Material.AIR, world.getBlockAt(0, 64, 0).getType());
        assertEquals(65, world.getSpawnLocation().getBlockY());
    }

    @Test
    void regeneration_placesNothingButStillSetsSpawn() {
        WorldMock world = server.addSimpleWorld("void-world");

        factory(new PluginConfig.World.VoidBlock(true, Material.GOLD_BLOCK), false)
                .applyPostGenerationSettings(world, BuildWorldType.VOID);

        assertEquals(Material.AIR, world.getBlockAt(0, 64, 0).getType());
        assertEquals(65, world.getSpawnLocation().getBlockY());
    }
}
