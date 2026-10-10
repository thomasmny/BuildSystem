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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Zombie;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Pins what each {@link EditMenu} slot needs and does: a player holding only that slot's permission is let through and
 * sees the click's effect, and a player without it is refused. The menu is built through its real production
 * constructor under a {@link MockBukkit} server.
 */
@NullMarked
class EditMenuTest {

    private ServerMock server;
    private Messages messages;
    private Menus menus;
    private PlayerServiceImpl playerService;
    private BuildWorld buildWorld;
    private WorldDataImpl data;
    private SoundlessPlayer player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        menus = mock(Menus.class);
        playerService = mock(PlayerServiceImpl.class);
        data = WorldDataSchema.create("world", TestData.NOT_STARTED);
        buildWorld = mock(BuildWorld.class);
        when(buildWorld.getName()).thenReturn("world");
        when(buildWorld.getData()).thenReturn(data);
        when(buildWorld.getBuilders()).thenReturn(mock(Builders.class));
        when(buildWorld.cycleDifficulty()).thenReturn(Difficulty.EASY);
        player = SoundlessPlayer.join(server, "Alex");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    static Stream<Arguments> slots() {
        return Stream.of(
                slot(3, Permissions.EDIT_ICON, t -> verify(t.menus).openMaterialPicker(eq(t.player), any(), any())),
                slot(5, Permissions.EDIT_PIN, t -> assertEquals(true, t.data.get(WorldDataKey.PINNED))),
                slot(20, Permissions.EDIT_BREAKING, t -> assertEquals(false, t.data.get(WorldDataKey.BLOCK_BREAKING))),
                slot(
                        21,
                        Permissions.EDIT_PLACEMENT,
                        t -> assertEquals(false, t.data.get(WorldDataKey.BLOCK_PLACEMENT))),
                slot(22, Permissions.EDIT_PHYSICS, t -> assertEquals(false, t.data.get(WorldDataKey.PHYSICS))),
                slot(23, Permissions.EDIT_TIME, t -> verify(t.menus).reopenEdit(t.buildWorld, t.player)),
                slot(24, Permissions.EDIT_EXPLOSIONS, t -> assertEquals(false, t.data.get(WorldDataKey.EXPLOSIONS))),
                slot(
                        29,
                        Permissions.EDIT_ENTITIES,
                        t -> verify(t.messages)
                                .sendMessage(eq(t.player), eq("worldeditor_butcher_removed"), any(Placeholders.class))),
                slot(30, Permissions.EDIT_BUILDERS, t -> assertEquals(true, t.data.get(WorldDataKey.BUILDERS_ENABLED))),
                slot(31, Permissions.EDIT_MOBAI, t -> assertEquals(false, t.data.get(WorldDataKey.MOB_AI))),
                slot(
                        32,
                        Permissions.EDIT_VISIBILITY,
                        t -> assertEquals(Visibility.ADDED_PLAYERS, t.data.get(WorldDataKey.VISIBILITY))),
                slot(
                        33,
                        Permissions.EDIT_INTERACTIONS,
                        t -> assertEquals(false, t.data.get(WorldDataKey.BLOCK_INTERACTIONS))),
                slot(38, Permissions.EDIT_GAMERULES, t -> verify(t.menus).openGameRules(t.buildWorld, t.player)),
                slot(39, Permissions.EDIT_DIFFICULTY, t -> verify(t.buildWorld).cycleDifficulty()),
                slot(40, Permissions.EDIT_STATUS, t -> verify(t.menus).openStatus(t.buildWorld, t.player)),
                slot(41, Permissions.EDIT_PROJECT, t -> verify(t.menus).promptWorldProject(t.buildWorld, t.player)),
                slot(
                        42,
                        Permissions.EDIT_PERMISSION,
                        t -> verify(t.menus).promptWorldPermission(t.buildWorld, t.player)));
    }

    @ParameterizedTest(name = "slot {0} needs {1}")
    @MethodSource("slots")
    void slot_needsItsPermission_andActs(int slot, String permission, Consumer<EditMenuTest> effect) {
        server.addSimpleWorld("world");
        when(buildWorld.getBuilders().isCreator(player)).thenReturn(true);
        when(playerService.canCreateWorld(eq(player), any())).thenReturn(true);

        click(slot);
        verify(messages).sendPermissionError(player);

        player.addAttachment(MockBukkit.createMockPlugin(), permission, true);
        click(slot);
        effect.accept(this);
    }

    @Test
    void otherSlots_doNothingEvenForAnOperator() {
        Set<Integer> buttons =
                slots().map(arguments -> (Integer) arguments.get()[0]).collect(Collectors.toSet());
        player.setOp(true);

        IntStream.range(0, 54).filter(slot -> !buttons.contains(slot)).forEach(this::click);

        verifyNoInteractions(menus);
        verify(messages, never()).sendPermissionError(player);
    }

    @Test
    void butcher_removesEntitiesButKeepsDecorationAndPlayers() {
        player.setOp(true);
        World world = Objects.requireNonNull(server.getWorld("world"));
        Location location = world.getSpawnLocation();
        Zombie zombie = world.spawn(location, Zombie.class);
        ArmorStand stand = world.spawn(location, ArmorStand.class);

        click(29);

        assertTrue(zombie.isDead());
        assertFalse(stand.isDead());
    }

    private static Arguments slot(int slot, String permission, Consumer<EditMenuTest> effect) {
        return Arguments.of(slot, permission, effect);
    }

    private void click(int slot) {
        EditMenu menu = new EditMenu(
                new MenuContext(messages, mock(MenuItems.class), menus),
                playerService,
                mock(ConfigService.class, RETURNS_DEEP_STUBS),
                mock(Prompts.class),
                buildWorld,
                player);
        menu.getInventory()
                .setItem(slot, ItemBuilder.of(Material.STONE).name("x").build());
        menu.handleClick(new InventoryClickEvent(
                player.openInventory(menu.getInventory()),
                SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL));
    }
}
