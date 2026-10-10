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
package de.eintosti.buildsystem.world.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.util.List;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Pins who may enter a world and run {@code /worlds} commands on it, using a real player's permissions and a real
 * world.
 */
@NullMarked
class WorldPermissionsImplTest {

    private static final String WORLD_PERMISSION = "maps.lobby";
    private static final String COMMAND = "buildsystem.edit";

    private enum Relation {
        NONE,
        CREATOR,
        BUILDER
    }

    private ServerMock server;
    private Plugin plugin;
    private WorldContext context;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        context = TestData.worldContext();
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        context.scheduler().shutdown();
        MockBukkit.unmock();
    }

    private BuildWorldImpl world(Relation relation) {
        Builder creator = relation == Relation.CREATOR
                ? Builder.of(player.getUniqueId(), player.getName())
                : Builder.of(UUID.randomUUID(), "Creator");
        List<Builder> builders =
                relation == Relation.BUILDER ? List.of(Builder.of(player.getUniqueId(), player.getName())) : List.of();
        WorldDataImpl data = WorldDataSchema.create("lobby", TestData.NOT_STARTED);
        data.set(WorldDataKey.DIFFICULTY, Difficulty.NORMAL);
        data.set(WorldDataKey.PERMISSION, WORLD_PERMISSION);
        return new BuildWorldImpl(
                context,
                UUID.randomUUID(),
                "lobby",
                BuildWorldType.NORMAL,
                data,
                creator,
                builders,
                System.currentTimeMillis(),
                null,
                null);
    }

    private void grant(@Nullable String nodes) {
        if (nodes == null) {
            return;
        }
        for (String node : nodes.split(";")) {
            player.addAttachment(plugin, node, true);
        }
    }

    @ParameterizedTest(name = "{0} holding [{1}], world permission {2}: {3}")
    @CsvSource({
        "NONE,    ,                                    maps.lobby, false",
        "NONE,    maps.lobby,                          maps.lobby, true",
        "NONE,    ,                                    -,          true",
        "NONE,    buildsystem.admin,                   maps.lobby, true",
        "NONE,    buildsystem.bypass.permission.public, maps.lobby, true",
        "CREATOR, ,                                    maps.lobby, true",
        "BUILDER, ,                                    maps.lobby, true",
    })
    void canEnter(Relation relation, @Nullable String nodes, String worldPermission, boolean expected) {
        BuildWorldImpl world = world(relation);
        world.getData().set(WorldDataKey.PERMISSION, worldPermission);
        grant(nodes);

        assertEquals(expected, world.getPermissions().canEnter(player));
    }

    @ParameterizedTest(name = "archived={0}, private={1}, holding {2}: {3}")
    @CsvSource({
        "false, false, buildsystem.bypass.permission.public,  true",
        "false, false, buildsystem.bypass.permission.private, false",
        "false, true,  buildsystem.bypass.permission.private, true",
        "false, true,  buildsystem.bypass.permission.public,  false",
        "true,  false, buildsystem.bypass.permission.archive, true",
        "true,  false, buildsystem.bypass.permission.public,  false",
        "true,  true,  buildsystem.bypass.permission.private, false",
    })
    void canBypassViewPermission(boolean archived, boolean privateWorld, String node, boolean expected) {
        BuildWorldImpl world = world(Relation.NONE);
        if (archived) {
            world.getData().set(WorldDataKey.STATUS, TestData.ARCHIVE_STATUS);
        }
        world.getData().set(WorldDataKey.VISIBILITY, Visibility.matchVisibility(privateWorld));
        grant(node);

        assertEquals(expected, world.getPermissions().canBypassViewPermission(player));
    }

    /**
     * The creator needs the command's node or its {@code .self} node; everyone else, builders included, needs
     * {@code .other}. A creator holding only {@code .other} is refused on their own world.
     */
    @ParameterizedTest(name = "{0} holding [{1}]: {2}")
    @CsvSource({
        "NONE,    ,                       false",
        "NONE,    buildsystem.edit,       false",
        "NONE,    buildsystem.edit.self,  false",
        "NONE,    buildsystem.edit.other, true",
        "NONE,    buildsystem.admin,      true",
        "BUILDER, buildsystem.edit.self,  false",
        "BUILDER, buildsystem.edit.other, true",
        "CREATOR, ,                       false",
        "CREATOR, buildsystem.edit,       true",
        "CREATOR, buildsystem.edit.self,  true",
        "CREATOR, buildsystem.edit.other, false",
    })
    void canPerformCommand(Relation relation, @Nullable String nodes, boolean expected) {
        BuildWorldImpl world = world(relation);
        grant(nodes);

        assertEquals(expected, world.getPermissions().canPerformCommand(player, COMMAND));
    }

    @Test
    void canPerformCommand_withoutAPermission_isAllowed() {
        BuildWorldImpl world = world(Relation.NONE);

        assertTrue(world.getPermissions().canPerformCommand(player, null));
        assertTrue(world.getPermissions().canPerformCommand(player, ""));
    }

    @Test
    void withoutAWorld_commandsPassButEntryIsRefused() {
        WorldPermissionsImpl permissions = WorldPermissionsImpl.of(context, null);
        grant("buildsystem.bypass.permission.public");

        // Commands go through so the caller can report the missing world.
        assertTrue(permissions.canPerformCommand(player, COMMAND));
        assertFalse(permissions.canEnter(player));
        assertFalse(permissions.canBypassViewPermission(player));
    }
}
