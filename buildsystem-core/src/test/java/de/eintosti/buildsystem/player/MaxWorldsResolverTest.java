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
package de.eintosti.buildsystem.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.Visibility;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
class MaxWorldsResolverTest {

    private MaxWorldsResolver service;

    @BeforeEach
    void setUp() {
        service = new MaxWorldsResolver(Logger.getLogger("test"));
    }

    private Player playerWith(boolean isAdmin, String... permissionStrings) {
        Player player = mock(Player.class);
        when(player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)).thenReturn(isAdmin);
        Set<PermissionAttachmentInfo> perms = Arrays.stream(permissionStrings)
                .map(p -> {
                    PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
                    when(pai.getPermission()).thenReturn(p);
                    return pai;
                })
                .collect(Collectors.toSet());
        when(player.getEffectivePermissions()).thenReturn(perms);
        return player;
    }

    @Test
    void adminPermissionIsUnlimited() {
        Player admin = playerWith(true);
        assertEquals(-1, service.getMaxWorlds(admin, Visibility.PUBLIC));
    }

    @Test
    void wildcardPublicPermissionIsUnlimited() {
        Player player = playerWith(false, "buildsystem.create.public.*");
        assertEquals(-1, service.getMaxWorlds(player, Visibility.PUBLIC));
    }

    @Test
    void numericPublicPermissionReturnsLimit() {
        Player player = playerWith(false, "buildsystem.create.public.5");
        assertEquals(5, service.getMaxWorlds(player, Visibility.PUBLIC));
    }

    @Test
    void noMatchingPermissionReturnsNegativeOne() {
        Player player = playerWith(false, "buildsystem.worlds.list");
        assertEquals(-1, service.getMaxWorlds(player, Visibility.PUBLIC));
    }

    @Test
    void publicPermissionDoesNotApplyToPrivate() {
        Player player = playerWith(false, "buildsystem.create.public.5");
        assertEquals(-1, service.getMaxWorlds(player, Visibility.PRIVATE));
    }

    @Test
    void highestNumericPermissionWins() {
        Player player = playerWith(
                false, "buildsystem.create.public.3", "buildsystem.create.public.7", "buildsystem.create.public.5");
        assertEquals(7, service.getMaxWorlds(player, Visibility.PUBLIC));
    }
}
