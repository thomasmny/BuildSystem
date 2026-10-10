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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryView;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;

@NullMarked
class BuilderMenuTest {

    private static final int FIRST_BUILDER_SLOT = 9;

    private ServerMock server;
    private MockedStatic<ItemBuilder> itemBuilder;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // XSkull needs authlib, which only the server provides; plain heads are enough here.
        itemBuilder = mockStatic(ItemBuilder.class, CALLS_REAL_METHODS);
        itemBuilder
                .when(() -> ItemBuilder.skull(any(Profileable.class)))
                .thenAnswer(invocation -> ItemBuilder.of(Material.PLAYER_HEAD));
    }

    @AfterEach
    void tearDown() {
        itemBuilder.close();
        MockBukkit.unmock();
    }

    @Test
    void shiftClickRemovesTheBuilderByItsStoredId() {
        // The stored name is stale: the builder has renamed since being added. Removal must not depend on it.
        Builder builder = Builder.of(UUID.randomUUID(), "OldName");
        Builders builders = mock(Builders.class);
        when(builders.getAllBuilders()).thenReturn(List.of(builder));
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getBuilders()).thenReturn(builders);

        PlayerMock admin = admin();
        admin.addAttachment(MockBukkit.createMockPlugin(), BuildSystemPlugin.ADMIN_PERMISSION, true);

        BuilderMenu menu = menu(buildWorld, admin);
        menu.populate(admin);
        menu.handleClick(click(admin, menu, FIRST_BUILDER_SLOT, ClickType.SHIFT_LEFT));

        verify(builders).removeBuilder(builder);
    }

    @Test
    void plainClickDoesNotRemove() {
        Builder builder = Builder.of(UUID.randomUUID(), "Name");
        Builders builders = mock(Builders.class);
        when(builders.getAllBuilders()).thenReturn(List.of(builder));
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getBuilders()).thenReturn(builders);
        when(buildWorld.getPermissions()).thenReturn(mock());

        PlayerMock admin = admin();
        admin.addAttachment(MockBukkit.createMockPlugin(), BuildSystemPlugin.ADMIN_PERMISSION, true);

        BuilderMenu menu = menu(buildWorld, admin);
        menu.populate(admin);
        menu.handleClick(click(admin, menu, FIRST_BUILDER_SLOT, ClickType.LEFT));

        verify(builders, never()).removeBuilder(any(Builder.class));
    }

    private PlayerMock admin() {
        // XSound plays through the seeded overload, which MockBukkit does not implement.
        PlayerMock player = new PlayerMock(server, "Admin") {
            @Override
            public void playSound(
                    Location location, Sound sound, SoundCategory category, float volume, float pitch, long seed) {}
        };
        server.addPlayer(player);
        return player;
    }

    private BuilderMenu menu(BuildWorld buildWorld, PlayerMock player) {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("text");
        when(messages.getString(anyString(), any(), any())).thenReturn("text");
        return new BuilderMenu(
                messages,
                mock(MenuItems.class),
                mock(Menus.class),
                new NamespacedKey("buildsystem", "builder_name"),
                buildWorld,
                player);
    }

    private static InventoryClickEvent click(PlayerMock player, BuilderMenu menu, int slot, ClickType type) {
        InventoryView view = player.openInventory(menu.getInventory());
        return new InventoryClickEvent(view, SlotType.CONTAINER, slot, type, InventoryAction.PICKUP_ALL);
    }
}
