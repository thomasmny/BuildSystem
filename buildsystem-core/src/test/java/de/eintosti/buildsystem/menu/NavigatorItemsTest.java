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
package de.eintosti.buildsystem.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@NullMarked
class NavigatorItemsTest {

    private ServerMock server;
    private PlayerMock player;
    private NavigatorItems items;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().settings().navigator().item()).thenReturn(XMaterial.CLOCK);
        Messages messages = mock(Messages.class);
        when(messages.getString(any(), any())).thenReturn("Navigator");
        items = new NavigatorItems(MockBukkit.createMockPlugin(), configService, messages);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void itemsAreRecognisedByTagNotByName() {
        assertTrue(items.is(items.create(player)));
        assertTrue(items.isBarrier(items.createBarrier(player)));

        ItemStack namedBarrier =
                ItemBuilder.of(Material.BARRIER).name("Navigator").build();
        assertFalse(items.isBarrier(namedBarrier));
        assertFalse(items.is(new ItemStack(Material.CLOCK)));
        assertFalse(items.isBarrier(items.create(player)));
    }

    @Test
    void replace_swapsTheNavigatorForTheBarrierAndBack() {
        player.getInventory().setItem(3, items.create(player));

        items.replace(player, items::is, items.createBarrier(player));
        assertTrue(items.isBarrier(player.getInventory().getItem(3)));

        items.replace(player, items::isBarrier, items.create(player));
        assertTrue(items.is(player.getInventory().getItem(3)));
        assertEquals(1, items.slots(player).size());
    }
}
