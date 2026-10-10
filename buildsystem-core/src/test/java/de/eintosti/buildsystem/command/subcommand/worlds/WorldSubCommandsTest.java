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
package de.eintosti.buildsystem.command.subcommand.worlds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.PlayerChatInput.InputRunnable;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import de.eintosti.buildsystem.world.download.WorldDownloadService;
import de.eintosti.buildsystem.world.menu.WorldPrompts;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Pins how each {@code /worlds} subcommand is wired to its {@link WorldTarget}, and what they do once their world is
 * resolved: the builder lookups and the world-name completion. The order of the checks is pinned by
 * {@code WorldTargetTest}.
 */
@NullMarked
class WorldSubCommandsTest {

    private static final UUID NOTCH = UUID.randomUUID();

    private Messages messages;
    private WorldServiceImpl worldService;
    private WorldStorageImpl worldStorage;
    private PlayerLookupService lookup;
    private SoundlessPlayer player;
    private BuildWorld buildWorld;
    private final Menus menus = mock(Menus.class);
    private final Prompts prompts = mock(Prompts.class);
    private final WorldPrompts worldPrompts = mock(WorldPrompts.class);
    private final ConfigService configService = mock(ConfigService.class);
    private final SettingsService settingsService = mock(SettingsService.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final Logger logger = Logger.getLogger("test");

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        worldStorage = mock(WorldStorageImpl.class);
        worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
        when(worldService.resolveWorldName(any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        player = SoundlessPlayer.join(MockBukkit.mock(), "Alex");
        buildWorld = buildWorld(true);

        lookup = mock(PlayerLookupService.class);
        doAnswer(invocation -> {
                    if (invocation.<String>getArgument(0).equals("Notch")) {
                        invocation.<Consumer<Builder>>getArgument(1).accept(Builder.of(NOTCH, "Notch"));
                    } else {
                        invocation.<Runnable>getArgument(2).run();
                    }
                    return null;
                })
                .when(lookup)
                .resolve(anyString(), any(), any());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * One row per subcommand: how it is built, the key for a missing world, the key for too many arguments (or
     * {@code null} when extra arguments are ignored), and the most arguments it accepts.
     */
    static Stream<Arguments> subCommands() {
        return Stream.of(
                typed(t -> new BuildersSubCommand(t.messages, t.worldService, t.menus), "worlds_builders", 2),
                typed(
                        t -> new DeleteSubCommand(t.messages, t.worldService, t.configService, t.menus),
                        "worlds_delete",
                        2),
                typed(
                        t -> new DownloadSubCommand(
                                t.messages, t.worldService, mock(WorldDownloadService.class), t.scheduler, t.logger),
                        "worlds_download",
                        2),
                typed(t -> new EditSubCommand(t.messages, t.worldService, t.menus), "worlds_edit", 2),
                typed(t -> new InfoSubCommand(t.messages, t.worldService), "worlds_info", 2),
                typed(t -> new RenameSubCommand(t.messages, t.worldService, t.prompts), "worlds_rename", 2),
                typed(
                        t -> new SaveTemplateSubCommand(
                                t.messages, t.worldService, t.configService, new File("data"), t.logger, t.scheduler),
                        "worlds_savetemplate",
                        3),
                typed(
                        t -> new SetCreatorSubCommand(
                                t.messages, t.worldService, t.lookup, t.prompts, t.settingsService),
                        "worlds_setcreator",
                        2),
                typed(t -> new SetItemSubCommand(t.messages, t.worldService), "worlds_setitem", 2),
                typed(
                        t -> new SetPermissionSubCommand(t.messages, t.worldService, t.worldPrompts),
                        "worlds_setpermission",
                        2),
                typed(
                        t -> new SetProjectSubCommand(t.messages, t.worldService, t.worldPrompts),
                        "worlds_setproject",
                        2),
                typed(t -> new SetStatusSubCommand(t.messages, t.worldService, t.menus), "worlds_setstatus", 2),
                typed(t -> new UnimportSubCommand(t.messages, t.worldService), "worlds_unimport", 2),
                Arguments.of(
                        (Function<WorldSubCommandsTest, WorldSubCommand>)
                                t -> new AddBuilderSubCommand(t.messages, t.worldService, t.worldPrompts),
                        "worlds_addbuilder_unknown_world",
                        "worlds_addbuilder_usage",
                        2),
                Arguments.of(
                        (Function<WorldSubCommandsTest, WorldSubCommand>) t -> new BackupsSubCommand(
                                t.messages, t.worldService, mock(BackupServiceImpl.class), t.menus),
                        "worlds_backup_world_not_imported",
                        "worlds_backup_usage",
                        2),
                Arguments.of(
                        (Function<WorldSubCommandsTest, WorldSubCommand>)
                                t -> new RemoveBuilderSubCommand(t.messages, t.worldService, t.lookup, t.prompts),
                        "worlds_removebuilder_unknown_world",
                        "worlds_removebuilder_usage",
                        2),
                Arguments.of(
                        (Function<WorldSubCommandsTest, WorldSubCommand>)
                                t -> new RemoveSpawnSubCommand(t.messages, t.worldService),
                        "worlds_removespawn_world_not_imported",
                        null,
                        1),
                Arguments.of(
                        (Function<WorldSubCommandsTest, WorldSubCommand>)
                                t -> new SetSpawnSubCommand(t.messages, t.worldService),
                        "worlds_setspawn_world_not_imported",
                        null,
                        1));
    }

    private static Arguments typed(
            Function<WorldSubCommandsTest, WorldSubCommand> factory, String prefix, int maxArgs) {
        return Arguments.of(factory, prefix + "_unknown_world", prefix + "_usage", maxArgs);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("subCommands")
    void missingWorld_andTooManyArguments_sendTheirKeys(
            Function<WorldSubCommandsTest, WorldSubCommand> factory,
            String missingKey,
            @Nullable String usageKey,
            int maxArgs) {
        WorldSubCommand subCommand = factory.apply(this);

        subCommand.execute(player, "nowhere", arguments(maxArgs, "nowhere"));
        verify(messages).sendMessage(player, missingKey);

        if (usageKey != null) {
            when(worldStorage.getBuildWorld("world")).thenReturn(buildWorld);
            when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(buildWorld);
            subCommand.execute(player, "world", arguments(maxArgs + 1, "world"));
            verify(messages).sendMessage(player, usageKey);
        }
    }

    private static String[] arguments(int count, String worldName) {
        String[] args = new String[count];
        Arrays.fill(args, "x");
        args[0] = "label";
        if (count > 1) {
            args[1] = worldName;
        }
        return args;
    }

    @Test
    void removeBuilder_knownPlayer_isRemoved() {
        when(buildWorld.getBuilders().isBuilder(NOTCH)).thenReturn(true);

        new RemoveBuilderSubCommand(messages, worldService, lookup, mock(Prompts.class))
                .execute(player, buildWorld, new String[] {"removeBuilder", "Notch"});

        verify(buildWorld.getBuilders()).removeBuilder(NOTCH);
    }

    @Test
    void removeBuilder_unknownPlayer_sendsTheNotFoundKey_andClosesTheInventory() {
        player.openInventory(Bukkit.createInventory(null, 9));

        new RemoveBuilderSubCommand(messages, worldService, lookup, mock(Prompts.class))
                .execute(player, buildWorld, new String[] {"removeBuilder", "Nobody"});

        verify(messages).sendMessage(player, "worlds_removebuilder_player_not_found");
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
    }

    @Test
    void setCreator_storesTheResolvedPlayer() {
        Prompts prompts = mock(Prompts.class);
        Prompts.Builder prompt = mock(Prompts.Builder.class, RETURNS_SELF);
        when(prompts.prompt(player)).thenReturn(prompt);
        doAnswer(invocation -> {
                    invocation.<InputRunnable>getArgument(0).run("Notch");
                    return null;
                })
                .when(prompt)
                .request(any());

        new SetCreatorSubCommand(messages, worldService, lookup, prompts, mock(SettingsService.class))
                .execute(player, buildWorld, new String[] {"setCreator"});

        verify(buildWorld.getBuilders())
                .setCreator(argThat(creator ->
                        creator.getUniqueId().equals(NOTCH) && creator.getName().equals("Notch")));
    }

    @Test
    void completion_offersPermittedWorldsMatchingThePrefix() {
        BuildWorld allowed = buildWorld(true);
        when(allowed.getName()).thenReturn("Lobby");
        BuildWorld otherPrefix = buildWorld(true);
        when(otherPrefix.getName()).thenReturn("Arena");
        BuildWorld denied = buildWorld(false);
        when(denied.getName()).thenReturn("Lounge");
        when(worldStorage.getBuildWorlds()).thenReturn(List.of(allowed, otherPrefix, denied));
        when(worldStorage.typedNames()).thenReturn(Function.identity());

        EditSubCommand edit = new EditSubCommand(messages, worldService, mock(Menus.class));

        assertEquals(List.of("Lobby"), edit.complete(player, new String[] {"edit", "lo"}));
        assertEquals(List.of(), edit.complete(player, new String[] {"edit", "lo", "x"}));
    }

    private BuildWorld buildWorld(boolean permitted) {
        WorldPermissions permissions = mock(WorldPermissions.class);
        when(permissions.canPerformCommand(eq(player), any())).thenReturn(permitted);
        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.PERMISSION)).thenReturn("-");
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getName()).thenReturn("world");
        when(buildWorld.getPermissions()).thenReturn(permissions);
        when(buildWorld.getData()).thenReturn(data);
        when(buildWorld.getBuilders()).thenReturn(mock(Builders.class));
        return buildWorld;
    }
}
