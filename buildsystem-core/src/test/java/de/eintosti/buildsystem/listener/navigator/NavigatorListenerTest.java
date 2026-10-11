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
package de.eintosti.buildsystem.listener.navigator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.Permissions;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@NullMarked
class NavigatorListenerTest {

    private ServerMock server;
    private PlayerMock player;
    private WorldPermissions permissions;
    private NavigatorListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();

        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.ARCHIVE_STATUS);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(false);
        permissions = mock(WorldPermissions.class);
        BuildWorld archive = mock(BuildWorld.class);
        when(archive.getData()).thenReturn(data);
        when(archive.getPermissions()).thenReturn(permissions);
        when(archive.getBuilders()).thenReturn(mock(Builders.class));
        WorldStorage worldStorage = mock(WorldStorage.class);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(archive);

        listener = new NavigatorListener(
                mock(NavigatorService.class),
                mock(SettingsService.class),
                worldStorage,
                mock(NavigatorItems.class),
                mock(Messages.class),
                mock(Menus.class),
                mock(PlayerService.class));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void interactingWithAnEntityInAnArchive_isCancelled() {
        assertTrue(interact().isCancelled());
    }

    @Test
    void interactingWithAnEntityInAnArchive_withTheArchiveBypass_isAllowed() {
        player.addAttachment(MockBukkit.createMockPlugin(), Permissions.BYPASS_ARCHIVE, true);

        assertFalse(interact().isCancelled());
    }

    @Test
    void interactingWithAnEntityInAnArchive_asAnAdmin_isAllowed() {
        when(permissions.hasAdminPermission(player)).thenReturn(true);

        assertFalse(interact().isCancelled());
    }

    private PlayerInteractAtEntityEvent interact() {
        ArmorStand armorStand = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
        PlayerInteractAtEntityEvent event = new PlayerInteractAtEntityEvent(player, armorStand, new Vector());
        listener.manageNewNavigatorInteraction(event);
        return event;
    }
}
