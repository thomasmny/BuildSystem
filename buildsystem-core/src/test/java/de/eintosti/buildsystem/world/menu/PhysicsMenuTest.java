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
package de.eintosti.buildsystem.world.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Golden test pinning the {@link PhysicsMenu} slot &rarr; {@link WorldDataKey} contract: the master physics switch and
 * one toggle per {@link PhysicsCategory}.
 */
@NullMarked
class PhysicsMenuTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PhysicsMenu menu() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        BuildWorld buildWorld = mock(BuildWorld.class);
        Player player = server.addPlayer();
        return new PhysicsMenu(messages, mock(MenuItems.class), mock(Menus.class), buildWorld, player);
    }

    @Test
    void keyBySlot_mapsMasterAndEveryCategory() {
        Map<Integer, WorldDataKey<Boolean>> keys = menu().keyBySlot();

        assertEquals(WorldDataKey.PHYSICS, keys.get(4));

        assertEquals(PhysicsCategory.BLOCK_UPDATES.key(), keys.get(12));
        assertEquals(PhysicsCategory.CONNECTIONS.key(), keys.get(13));
        assertEquals(PhysicsCategory.FALLING_BLOCKS.key(), keys.get(14));
        assertEquals(PhysicsCategory.FLUID_FLOW.key(), keys.get(21));
        assertEquals(PhysicsCategory.LEAF_DECAY.key(), keys.get(22));
        assertEquals(PhysicsCategory.GROWTH.key(), keys.get(23));
        assertEquals(PhysicsCategory.SPREADING.key(), keys.get(30));
        assertEquals(PhysicsCategory.BLOCK_FORMING.key(), keys.get(31));
        assertEquals(PhysicsCategory.BLOCK_FADING.key(), keys.get(32));

        // Master + one slot per category; a new PhysicsCategory constant must be given a slot here.
        assertEquals(1 + PhysicsCategory.values().length, keys.size());
    }
}
