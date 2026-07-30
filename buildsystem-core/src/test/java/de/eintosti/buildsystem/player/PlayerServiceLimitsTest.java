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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Pins the world-creation limit ladder: a {@code buildsystem.create.<visibility>.<amount>} node wins, the
 * {@code world.limits.*} config value is the fallback for players holding no such node, and creation is unlimited when
 * neither is set. Counting is per player and per visibility, so a limit on one visibility never blocks the other.
 */
@NullMarked
class PlayerServiceLimitsTest {

    private static final int UNLIMITED = -1;

    private PlayerServiceImpl service(int configPublic, int configPrivate, int ownedOfVisibility) {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        PluginConfig.World.Limits limits = new PluginConfig.World.Limits(configPublic, configPrivate);
        PluginConfig.World world = mock(PluginConfig.World.class);
        when(world.limits()).thenReturn(limits);
        PluginConfig config = mock(PluginConfig.class);
        when(config.world()).thenReturn(world);
        ConfigService configService = mock(ConfigService.class);
        when(configService.current()).thenReturn(config);

        List<BuildWorld> owned = Collections.nCopies(ownedOfVisibility, mock(BuildWorld.class));
        WorldStorageImpl worldStorage = mock(WorldStorageImpl.class);
        when(worldStorage.getBuildWorldsCreatedByPlayer(any(), any())).thenReturn(owned);
        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);

        return new PlayerServiceImpl(plugin, configService, () -> worldService, mock(TaskScheduler.class));
    }

    private Player playerWith(String... permissionStrings) {
        Player player = mock(Player.class);
        when(player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)).thenReturn(false);
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
    void permissionNodeOverridesTheConfiguredDefault() {
        // Config allows 1, the node allows 5, the player owns 3.
        PlayerServiceImpl service = service(1, 1, 3);
        assertTrue(service.canCreateWorld(playerWith("buildsystem.create.public.5"), Visibility.EVERYONE));
    }

    @Test
    void configuredDefaultAppliesWhenThePlayerHoldsNoNode() {
        PlayerServiceImpl service = service(2, 2, 2);
        assertFalse(service.canCreateWorld(playerWith(), Visibility.EVERYONE));
    }

    @Test
    void configuredDefaultStillAllowsCreationBelowTheLimit() {
        PlayerServiceImpl service = service(3, 3, 2);
        assertTrue(service.canCreateWorld(playerWith(), Visibility.EVERYONE));
    }

    @Test
    void unlimitedWhenNeitherNodeNorConfigSetsALimit() {
        PlayerServiceImpl service = service(UNLIMITED, UNLIMITED, 500);
        assertTrue(service.canCreateWorld(playerWith(), Visibility.EVERYONE));
    }

    @Test
    void aPrivateLimitDoesNotBlockPublicCreation() {
        // Private capped at 1 and already met; public is unlimited, so a public world is still allowed. Counting used
        // to ignore visibility, which made either limit block both.
        PlayerServiceImpl service = service(UNLIMITED, 1, 1);
        assertTrue(service.canCreateWorld(playerWith(), Visibility.EVERYONE));
        assertFalse(service.canCreateWorld(playerWith(), Visibility.ADDED_PLAYERS));
    }

    @Test
    void adminHasNoLimit() {
        PlayerServiceImpl service = service(1, 1, 99);
        Player admin = playerWith();
        when(admin.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)).thenReturn(true);
        assertTrue(service.canCreateWorld(admin, Visibility.EVERYONE));
    }
}
