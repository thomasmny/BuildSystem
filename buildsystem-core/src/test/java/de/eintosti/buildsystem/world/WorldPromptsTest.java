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
package de.eintosti.buildsystem.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.PlayerChatInput.InputRunnable;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;

class WorldPromptsTest {

    private static final UUID NOTCH = UUID.randomUUID();

    private ServerMock server;
    private SoundlessPlayer player;
    private Prompts.Builder prompt;
    private WorldPrompts worldPrompts;
    private BuildWorld buildWorld;
    private Messages messages;
    private Builders builders;
    private ConfigService configService;
    private final List<String> thens = new ArrayList<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = SoundlessPlayer.join(server, "Alex");
        prompt = mock(Prompts.Builder.class, RETURNS_SELF);
        Prompts prompts = mock(Prompts.class);
        when(prompts.prompt(player)).thenReturn(prompt);
        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.PROJECT)).thenReturn("Alpha");
        buildWorld = mock(BuildWorld.class);
        when(buildWorld.getName()).thenReturn("world");
        when(buildWorld.getData()).thenReturn(data);
        builders = mock(Builders.class);
        when(buildWorld.getBuilders()).thenReturn(builders);
        messages = mock(Messages.class);
        configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);

        PlayerLookupService lookup = mock(PlayerLookupService.class);
        doAnswer(invocation -> {
                    String name = invocation.getArgument(0);
                    if (name.equals("Notch")) {
                        invocation.<Consumer<Builder>>getArgument(1).accept(Builder.of(NOTCH, "Notch"));
                    } else if (name.equals("Alex")) {
                        invocation.<Consumer<Builder>>getArgument(1).accept(Builder.of(player));
                    } else {
                        invocation.<Runnable>getArgument(2).run();
                    }
                    return null;
                })
                .when(lookup)
                .resolve(anyString(), any(), any());

        worldPrompts = new WorldPrompts(messages, prompts, mock(SettingsService.class), configService, lookup);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void cancellingAPrompt_returnsToTheCaller() {
        worldPrompts.promptProject(player, buildWorld, () -> thens.add("back"));

        ArgumentCaptor<Runnable> onCancel = ArgumentCaptor.forClass(Runnable.class);
        verify(prompt).onCancel(onCancel.capture());
        onCancel.getValue().run();

        assertEquals(List.of("back"), thens);
    }

    @Test
    void answeringAPrompt_setsTheValueAndReturns() {
        worldPrompts.promptProject(player, buildWorld, () -> thens.add("back"));

        ArgumentCaptor<InputRunnable> request = ArgumentCaptor.forClass(InputRunnable.class);
        verify(prompt).request(request.capture());
        request.getValue().run(" Alpha ");

        verify(buildWorld.getData()).set(WorldDataKey.PROJECT, "Alpha");
        assertEquals(List.of("back"), thens);
    }

    @Test
    void addBuilder_newBuilder_isAddedAndRunsThen() {
        worldPrompts.addBuilder(player, buildWorld, "Notch", () -> thens.add("back"));

        verify(builders).addBuilder(any(Builder.class));
        verify(messages).sendMessage(eq(player), eq("worlds_addbuilder_added"), any(Placeholders.class));
        assertEquals(List.of("back"), thens);
    }

    @Test
    void addBuilder_theCreatorThemselves_isRefusedAndCloses() {
        when(builders.isCreator(player)).thenReturn(true);

        assertRefused("Alex", "worlds_addbuilder_already_creator");
    }

    @Test
    void addBuilder_anExistingBuilder_isRefusedAndCloses() {
        when(builders.isBuilder(NOTCH)).thenReturn(true);

        assertRefused("Notch", "worlds_addbuilder_already_added");
    }

    @Test
    void addBuilder_anUnknownName_isRefusedAndCloses() {
        assertRefused("Nobody", "worlds_addbuilder_player_not_found");
    }

    private void assertRefused(String name, String key) {
        player.openInventory(Bukkit.createInventory(null, 9));

        worldPrompts.addBuilder(player, buildWorld, name, () -> thens.add("back"));

        verify(messages).sendMessage(player, key);
        verify(builders, never()).addBuilder(any(Builder.class));
        assertEquals(InventoryType.CRAFTING, player.getOpenInventory().getType());
        assertEquals(List.of(), thens);
    }

    @Test
    void permissionOutsideTheWhitelist_isRefusedAndRunsThen() {
        when(configService.current().settings().worldPermissionWhitelist()).thenReturn(List.of("worlds.lobby"));
        worldPrompts.promptPermission(player, buildWorld, () -> thens.add("back"));

        ArgumentCaptor<InputRunnable> request = ArgumentCaptor.forClass(InputRunnable.class);
        verify(prompt).request(request.capture());
        request.getValue().run("worlds.secret");

        verify(buildWorld.getData(), never()).set(eq(WorldDataKey.PERMISSION), anyString());
        verify(messages).sendMessage(player, "worlds_setpermission_not_allowed");
        assertEquals(List.of("back"), thens);
    }

    @Test
    void isPermissionAllowed_emptyWhitelist_allowsAnything() {
        assertTrue(WorldPrompts.isPermissionAllowed("worlds.lobby", List.of()));
        assertTrue(WorldPrompts.isPermissionAllowed("anything.at.all", List.of()));
        assertTrue(WorldPrompts.isPermissionAllowed("-", List.of()));
    }

    @Test
    void isPermissionAllowed_nonEmptyWhitelist_allowsOnlyListed() {
        List<String> whitelist = List.of("worlds.lobby", "worlds.spawn");

        assertTrue(WorldPrompts.isPermissionAllowed("worlds.lobby", whitelist));
        assertTrue(WorldPrompts.isPermissionAllowed("worlds.spawn", whitelist));
        assertFalse(WorldPrompts.isPermissionAllowed("worlds.secret", whitelist));
    }

    @Test
    void isPermissionAllowed_nonEmptyWhitelist_alwaysAllowsClearSentinel() {
        List<String> whitelist = List.of("worlds.lobby");

        assertTrue(WorldPrompts.isPermissionAllowed("-", whitelist));
    }
}
