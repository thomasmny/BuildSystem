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
package de.eintosti.buildsystem.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@NullMarked
class BuildModeTest {

    @TempDir
    File dataFolder;

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void endBuildSession_givesBackWhatStartSaved() {
        ServerMock server = MockBukkit.mock();
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().addItem(new ItemStack(Material.DIRT));

        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        PlayerServiceImpl service = new PlayerServiceImpl(
                plugin, mock(ConfigService.class), () -> mock(WorldServiceImpl.class), mock(TaskScheduler.class));

        assertTrue(service.startBuildSession(player));
        assertFalse(service.startBuildSession(player), "a second start keeps the first snapshot");
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().clear();

        assertTrue(service.endBuildSession(player));
        assertFalse(service.isInBuildMode(player));
        assertEquals(GameMode.SURVIVAL, player.getGameMode());
        assertTrue(player.getInventory().contains(Material.DIRT));
        assertFalse(service.endBuildSession(player), "a second end is a no-op");
    }
}
