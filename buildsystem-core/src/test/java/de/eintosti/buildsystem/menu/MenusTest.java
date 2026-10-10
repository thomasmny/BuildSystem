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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

@NullMarked
class MenusTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void openEdit_unloadedWorld_showsTitleInsteadOfTheMenu() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any(Player.class))).thenReturn("not loaded");
        Services services = mock(Services.class);
        when(services.messages()).thenReturn(messages);

        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getWorld()).thenReturn(Optional.empty());
        Player player = spy(SoundlessPlayer.join(server, "Builder"));

        new Menus(plugin, services).openEdit(buildWorld, player);

        verify(messages).getString("world_not_loaded", player);
        verify(player).sendTitle(" ", "not loaded", 5, 70, 20);
        verify(player, never()).openInventory(any(Inventory.class));
    }
}
