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
package de.eintosti.buildsystem.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.command.HelpPages.Entry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.StorageType;
import de.eintosti.buildsystem.util.Permissions;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class BuildSystemCommandTest {

    private final Services services = mock(Services.class, RETURNS_DEEP_STUBS);
    private final BuildSystemCommand command =
            new BuildSystemCommand(mock(Messages.class, RETURNS_DEEP_STUBS), mock(Logger.class), services);

    @Test
    void worldsHelp_showsThePermissionItNeeds() {
        assertEquals(Permissions.HELP_WORLDS, entry("/worlds help").permission());
    }

    @Test
    void speed_withoutALevel_opensTheMenu_soTheLevelIsOptional() {
        assertEquals("/speed [1-5]", entry("/speed ").usage());
    }

    @Test
    void complete_offersTheStorageTypesOtherThanTheOneInUse() {
        when(services.storage().type()).thenReturn(StorageType.SQLITE);
        Player player = player(true);

        assertEquals(List.of("storage"), command.complete(player, "buildsystem", new String[] {"st"}));
        assertEquals(List.of("migrate"), command.complete(player, "buildsystem", new String[] {"storage", ""}));
        assertEquals(
                List.of("yaml", "mysql", "mariadb", "postgresql", "mongodb"),
                command.complete(player, "buildsystem", new String[] {"storage", "migrate", ""}));
        assertEquals(
                List.of("mysql", "mariadb", "mongodb"),
                command.complete(player, "buildsystem", new String[] {"storage", "migrate", "M"}));
    }

    @Test
    void complete_offersNothingWithoutThePermission() {
        assertEquals(List.of(), command.complete(player(false), "buildsystem", new String[] {"st"}));
    }

    private static Entry entry(String suggest) {
        return BuildSystemCommand.COMMANDS.stream()
                .filter(entry -> entry.suggest().equals(suggest))
                .findFirst()
                .orElseThrow();
    }

    private static Player player(boolean mayMigrate) {
        Player player = mock(Player.class);
        when(player.hasPermission(Permissions.STORAGE_MIGRATE)).thenReturn(mayMigrate);
        return player;
    }
}
