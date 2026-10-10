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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.bukkit.Sound;
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
 * Pins what each {@link SettingsMenu} slot needs and does: a player holding only that toggle's node flips it and the
 * menu reopens, and a player without it is refused. Built through the real production constructor under a
 * {@link MockBukkit} server.
 */
@NullMarked
class SettingsMenuTest {

    private ServerMock server;
    private Messages messages;
    private SettingsService settingsService;
    private Menus menus;
    private ConfigService configService;
    private SoundlessPlayer player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        settingsService = mock(SettingsService.class);
        menus = mock(Menus.class);
        configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        player = SoundlessPlayer.join(server, "Alex");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private SettingsMenu menu() {
        return new SettingsMenu(
                messages,
                settingsService,
                configService,
                mock(MenuItems.class),
                mock(NavigatorService.class),
                mock(NoClipService.class),
                menus,
                player);
    }

    static Stream<Arguments> toggles() {
        return Stream.of(
                toggle(12, "clear-inventory", Settings::isClearInventory),
                toggle(13, "disable-interact", Settings::isDisableInteract),
                toggle(14, "hide-players", Settings::isHidePlayers),
                toggle(15, "instant-place-signs", Settings::isInstantPlaceSigns),
                toggle(20, "keep-navigator", Settings::isKeepNavigator),
                toggle(21, "navigator-type", Settings::getNavigatorType),
                toggle(22, "night-vision", Settings::isNightVision),
                toggle(23, "no-clip", Settings::isNoClip),
                toggle(24, "open-trapdoors", Settings::isOpenTrapDoors),
                toggle(29, "place-plants", Settings::isPlacePlants),
                toggle(30, "scoreboard", Settings::isScoreboard),
                toggle(31, "slab-breaking", Settings::isSlabBreaking),
                toggle(32, "spawn-teleport", Settings::isSpawnTeleport));
    }

    private static Arguments toggle(int slot, String setting, Function<Settings, Object> value) {
        return Arguments.of(slot, "buildsystem.setting." + setting, value);
    }

    @ParameterizedTest(name = "slot {0} needs {1}")
    @MethodSource("toggles")
    void toggle_needsItsPermission_flipsItsSetting_andReopens(
            int slot, String permission, Function<Settings, Object> value) {
        Settings settings = new SettingsImpl();
        when(settingsService.getSettings(player)).thenReturn(settings);
        when(configService.current().settings().scoreboard()).thenReturn(true);
        Object before = value.apply(settings);

        click(menu(), slot);
        verify(messages).sendPermissionError(player);
        verify(menus, never()).openSettings(player);
        assertEquals(before, value.apply(settings));

        player.addAttachment(MockBukkit.createMockPlugin(), permission, true);
        click(menu(), slot);
        verify(menus).openSettings(player);
        assertNotEquals(before, value.apply(settings));
    }

    @Test
    void scoreboard_disabledInConfig_isRefusedWithoutReopening() {
        player.setOp(true);
        Settings settings = new SettingsImpl();
        when(settingsService.getSettings(player)).thenReturn(settings);
        when(configService.current().settings().scoreboard()).thenReturn(false);
        boolean before = settings.isScoreboard();

        click(menu(), 30);

        assertEquals(before, settings.isScoreboard());
        verify(menus, never()).openSettings(player);
        assertEquals(List.of(Sound.ENTITY_ITEM_BREAK), player.sounds());
    }

    @Test
    void otherSlots_doNothingEvenForAnOperator() {
        player.setOp(true);
        SettingsMenu menu = menu();
        Set<Integer> buttons = Set.of(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32);

        IntStream.range(0, 45).filter(slot -> !buttons.contains(slot)).forEach(slot -> click(menu, slot));

        verifyNoInteractions(menus);
    }

    @Test
    void design_opensTheDesignMenu() {
        click(menu(), 11);

        verify(menus).openDesign(player);
    }

    @Test
    void toggle_flipsTheSettingAndReopens() {
        player.setOp(true);
        Settings settings = mock(Settings.class);
        when(settingsService.getSettings(player)).thenReturn(settings);

        click(menu(), 12);

        verify(settings).setClearInventory(true);
        verify(menus).openSettings(player);
    }

    @Test
    void deniedToggle_keepsTheMenuOpenAndChangesNothing() {
        Settings settings = mock(Settings.class);
        when(settingsService.getSettings(player)).thenReturn(settings);
        SettingsMenu menu = menu();

        click(menu, 12);

        verify(messages).sendPermissionError(player);
        verify(settings, never()).setClearInventory(true);
        assertEquals(menu.getInventory(), player.getOpenInventory().getTopInventory());
    }

    private void click(SettingsMenu menu, int slot) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.openInventory(menu.getInventory()),
                SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL);
        menu.handleClick(event);
    }
}
