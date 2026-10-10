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
package de.eintosti.buildsystem.listener.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.NavigatorItems;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.util.List;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * Fires real Bukkit events at the listeners that guard a world, so the wiring from event to permission check to
 * cancellation is covered, not only the decision itself.
 */
@NullMarked
class WorldProtectionListenersTest {

    private ServerMock server;
    private Plugin plugin;
    private WorldContext context;
    private WorldMock world;
    private BuildWorldImpl buildWorld;
    private PlayerMock builder;
    private PlayerMock stranger;
    private Messages messages;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        context = TestData.worldContext();
        world = server.addSimpleWorld("lobby");
        builder = player("Builder");
        stranger = player("Stranger");

        WorldDataImpl data = WorldDataSchema.create("lobby", TestData.NOT_STARTED);
        data.set(WorldDataKey.DIFFICULTY, Difficulty.NORMAL);
        data.set(WorldDataKey.BUILDERS_ENABLED, true);
        buildWorld = new BuildWorldImpl(
                context,
                UUID.randomUUID(),
                "lobby",
                BuildWorldType.NORMAL,
                data,
                Builder.of(UUID.randomUUID(), "Creator"),
                List.of(Builder.of(builder.getUniqueId(), builder.getName())),
                System.currentTimeMillis(),
                null,
                null);

        WorldStorageImpl worldStorage = mock(WorldStorageImpl.class);
        when(worldStorage.getBuildWorld(world)).thenReturn(buildWorld);
        SettingsService settingsService = mock(SettingsService.class);
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().settings().builder().blockWorldEditNonBuilder())
                .thenReturn(true);
        messages = mock(Messages.class);

        server.getPluginManager()
                .registerEvents(
                        new WorldManipulateListener(
                                worldStorage, configService, context.statusRegistry(), settingsService),
                        plugin);
        server.getPluginManager()
                .registerEvents(
                        new PlayerCommandPreprocessListener(
                                settingsService,
                                worldStorage,
                                mock(NavigatorItems.class),
                                configService,
                                messages,
                                mock(TaskScheduler.class)),
                        plugin);
    }

    @AfterEach
    void tearDown() {
        context.scheduler().shutdown();
        MockBukkit.unmock();
    }

    private PlayerMock player(String name) {
        PlayerMock player = server.addPlayer(name);
        player.teleport(new Location(world, 0, 65, 0));
        return player;
    }

    private Block block() {
        return world.getBlockAt(0, 64, 0);
    }

    private boolean breakCancelled(PlayerMock player) {
        BlockBreakEvent event = new BlockBreakEvent(block(), player);
        server.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    private boolean placeCancelled(PlayerMock player) {
        Block placed = block().getRelative(BlockFace.UP);
        BlockPlaceEvent event = new BlockPlaceEvent(
                placed, placed.getState(), block(), new ItemStack(Material.STONE), player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    private boolean interactDenied(PlayerMock player) {
        PlayerInteractEvent event =
                new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block(), BlockFace.UP);
        server.getPluginManager().callEvent(event);
        return event.useInteractedBlock() == Event.Result.DENY;
    }

    private boolean commandCancelled(PlayerMock player, String command) {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, command);
        server.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    @Test
    void onlyBuildersMayBreakAndPlace() {
        assertFalse(breakCancelled(builder));
        assertFalse(placeCancelled(builder));
        assertTrue(breakCancelled(stranger));
        assertTrue(placeCancelled(stranger));
    }

    @Test
    void archivedWorld_refusesABuilder_butNotAnAdminOrBuildMode() {
        buildWorld.getData().set(WorldDataKey.STATUS, TestData.ARCHIVE_STATUS);
        PlayerMock admin = player("Admin");
        admin.addAttachment(plugin, "buildsystem.admin", true);

        assertTrue(breakCancelled(builder));
        assertFalse(breakCancelled(admin));

        when(context.playerService().isInBuildMode(builder)).thenReturn(true);
        assertFalse(breakCancelled(builder));
    }

    @Test
    void eachEventIsCheckedAgainstItsOwnSetting() {
        buildWorld.getData().set(WorldDataKey.BLOCK_BREAKING, false);

        assertTrue(breakCancelled(builder));
        assertFalse(placeCancelled(builder));
        assertFalse(interactDenied(builder));

        buildWorld.getData().set(WorldDataKey.BLOCK_BREAKING, true);
        buildWorld.getData().set(WorldDataKey.BLOCK_INTERACTIONS, false);

        assertFalse(breakCancelled(builder));
        assertTrue(interactDenied(builder));
    }

    @Test
    void disabledSetting_isBypassedWithTheSettingsNode() {
        buildWorld.getData().set(WorldDataKey.BLOCK_PLACEMENT, false);
        builder.addAttachment(plugin, "buildsystem.bypass.settings", true);

        assertFalse(placeCancelled(builder));
    }

    @Test
    void allowedEdit_recordsTheEditAndStartsTheWorld() {
        assertFalse(placeCancelled(builder));

        assertTrue(buildWorld.getData().get(WorldDataKey.LAST_EDITED) > 0);
        assertEquals(TestData.IN_PROGRESS, buildWorld.getData().get(WorldDataKey.STATUS));
    }

    @Test
    void refusedEdit_changesNothing() {
        long lastEdited = buildWorld.getData().get(WorldDataKey.LAST_EDITED);

        assertTrue(placeCancelled(stranger));

        assertEquals(lastEdited, buildWorld.getData().get(WorldDataKey.LAST_EDITED));
        assertEquals(TestData.NOT_STARTED, buildWorld.getData().get(WorldDataKey.STATUS));
    }

    @Test
    void worldEditCommand_isRefusedToANonBuilder() {
        assertTrue(commandCancelled(stranger, "//set stone"));
        verify(messages).sendMessage(stranger, "command_not_builder");

        assertFalse(commandCancelled(builder, "//set stone"));
    }

    @Test
    void worldEditCommand_isRefusedInAnArchivedWorld() {
        buildWorld.getData().set(WorldDataKey.STATUS, TestData.ARCHIVE_STATUS);

        assertTrue(commandCancelled(builder, "//set stone"));
        verify(messages).sendMessage(builder, "command_archive_world");
    }

    @Test
    void otherCommands_areLeftAlone() {
        assertFalse(commandCancelled(stranger, "/spawn"));
    }
}
