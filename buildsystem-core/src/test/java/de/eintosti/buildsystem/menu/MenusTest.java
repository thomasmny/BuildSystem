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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldPrompts;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest(name = "permitted: {0}")
    @ValueSource(booleans = {false, true})
    void promptAddBuilder_needsAddBuilderInThatWorld(boolean permitted) {
        Messages messages = mock(Messages.class);
        WorldPrompts worldPrompts = mock(WorldPrompts.class);
        Services services = mock(Services.class);
        when(services.messages()).thenReturn(messages);
        when(services.worldPrompts()).thenReturn(worldPrompts);
        Player player = SoundlessPlayer.join(server, "Builder");
        WorldPermissions permissions = mock(WorldPermissions.class);
        when(permissions.canPerformCommand(player, Permissions.ADDBUILDER)).thenReturn(permitted);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getPermissions()).thenReturn(permissions);

        new Menus(mock(BuildSystemPlugin.class), services).promptAddBuilder(buildWorld, player);

        verify(messages, permitted ? never() : times(1)).sendPermissionError(player);
        verify(worldPrompts, permitted ? times(1) : never()).promptAddBuilder(eq(player), eq(buildWorld), any());
    }

    @Test
    void openDelete_confirmDeletesTheWorld() {
        DeleteFixture fixture = new DeleteFixture();

        fixture.click(11);

        verify(fixture.worldService).deleteWorld(fixture.player, fixture.buildWorld);
    }

    @Test
    void openDelete_cancelSaysSo() {
        DeleteFixture fixture = new DeleteFixture();

        fixture.click(15);

        verify(fixture.worldService, never()).deleteWorld(any(), any());
        verify(fixture.messages).sendMessage(eq(fixture.player), eq("worlds_delete_canceled"), any(Placeholders.class));
    }

    @ParameterizedTest(name = "slot {0}, op {1}, restores: {2}")
    @CsvSource({"11, true, true", "15, true, false", "11, false, false"})
    void openBackupsConfirmation_onlyAPermittedConfirmRestores(int slot, boolean op, boolean restores) {
        Messages messages = labelledMessages();
        Services services = mock(Services.class);
        when(services.messages()).thenReturn(messages);
        SoundlessPlayer player = SoundlessPlayer.join(server, "Builder");
        player.setOp(op);
        BackupProfile owner = mock(BackupProfile.class);
        Backup backup = mock(Backup.class);
        when(backup.owner()).thenReturn(owner);

        new Menus(mock(BuildSystemPlugin.class), services).openBackupsConfirmation(backup, player);
        click(player, slot);

        verify(owner, restores ? times(1) : never()).restoreBackup(backup, player);
    }

    /**
     * The delete confirmation for one world, opened for an operator.
     */
    private final class DeleteFixture {

        private final Messages messages = labelledMessages();
        private final WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        private final BuildWorld buildWorld = mock(BuildWorld.class);
        private final SoundlessPlayer player = SoundlessPlayer.join(server, "Builder");

        DeleteFixture() {
            Services services = mock(Services.class);
            when(services.messages()).thenReturn(messages);
            when(services.world()).thenReturn(worldService);
            when(buildWorld.getName()).thenReturn("world");
            player.setOp(true);
            new Menus(mock(BuildSystemPlugin.class), services).openDelete(buildWorld, player);
        }

        void click(int slot) {
            MenusTest.click(player, slot);
        }
    }

    private static Messages labelledMessages() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any(Player.class))).thenReturn("label");
        when(messages.getString(anyString(), any(Player.class), any(Placeholders.class)))
                .thenReturn("label");
        when(messages.formatDateTime(anyLong())).thenReturn("now");
        return messages;
    }

    private static void click(Player player, int slot) {
        InventoryView view = player.getOpenInventory();
        ((Menu) view.getTopInventory().getHolder())
                .handleClick(new InventoryClickEvent(
                        view, SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL));
    }
}
