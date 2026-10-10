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
package de.eintosti.buildsystem.player.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.util.Permissions;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;

/**
 * Pins the speed each {@link SpeedMenu} slot sets, by clicking it. Built through the real production constructor under
 * a {@link MockBukkit} server.
 */
@NullMarked
class SpeedMenuTest {

    private static final float DEFAULT_WALK_SPEED = 0.2f;

    private Messages messages;
    private SpeedPlayer player;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        player = new SpeedPlayer(server);
        server.addPlayer(player);
        messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest(name = "slot {0} sets speed {2}")
    @CsvSource({"11, 0.2, 1", "12, 0.4, 2", "13, 0.6, 3", "14, 0.8, 4", "15, 1.0, 5"})
    void slot_setsItsWalkSpeed(int slot, float speed, int shown) {
        player.setWalkSpeed(0.1f);
        player.addAttachment(MockBukkit.createMockPlugin(), Permissions.SPEED, true);

        click(slot);

        assertEquals(speed, player.getWalkSpeed());
        ArgumentCaptor<Placeholders> placeholders = ArgumentCaptor.forClass(Placeholders.class);
        verify(messages).sendMessage(eq(player), eq("speed_set_walking"), placeholders.capture());
        assertEquals(String.valueOf(shown), placeholders.getValue().applyTo("%speed%"));
    }

    @Test
    void flying_setsTheFlySpeed() {
        player.setOp(true);
        player.setAllowFlight(true);
        player.setFlying(true);

        click(15);

        assertEquals(0.9f, player.getFlySpeed(), 0.0001f);
        ArgumentCaptor<Placeholders> placeholders = ArgumentCaptor.forClass(Placeholders.class);
        verify(messages).sendMessage(eq(player), eq("speed_set_flying"), placeholders.capture());
        assertEquals("5", placeholders.getValue().applyTo("%speed%"));
    }

    @Test
    void withoutPermission_nothingChanges() {
        click(15);

        assertEquals(DEFAULT_WALK_SPEED, player.getWalkSpeed());
    }

    @Test
    void otherSlots_doNothingEvenForAnOperator() {
        player.setOp(true);

        for (int slot = 0; slot < 27; slot++) {
            if (slot < 11 || slot > 15) {
                click(slot);
            }
        }

        assertEquals(DEFAULT_WALK_SPEED, player.getWalkSpeed());
        verify(messages, never()).sendMessage(eq(player), anyString(), any(Placeholders.class));
    }

    /**
     * PlayerMock refuses a walk speed of 1.0, which Bukkit allows, so this player stores it unchecked.
     */
    private static final class SpeedPlayer extends SoundlessPlayer {

        private float walkSpeed = DEFAULT_WALK_SPEED;

        SpeedPlayer(ServerMock server) {
            super(server, "Alex");
        }

        @Override
        public void setWalkSpeed(float walkSpeed) {
            this.walkSpeed = walkSpeed;
        }

        @Override
        public float getWalkSpeed() {
            return walkSpeed;
        }
    }

    private void click(int slot) {
        SpeedMenu menu = new SpeedMenu(
                new MenuContext(messages, mock(MenuItems.class), mock(Menus.class)),
                mock(SettingsService.class),
                player);
        menu.handleClick(new InventoryClickEvent(
                player.openInventory(menu.getInventory()),
                SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL));
    }
}
