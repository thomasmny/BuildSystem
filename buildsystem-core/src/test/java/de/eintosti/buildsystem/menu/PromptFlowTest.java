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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Drives a {@link Prompts.PromptFlow} through the real {@link PlayerChatInput} machinery under MockBukkit, pinning the
 * routing contract: a step that rejects its input is re-prompted, an accepting step advances, and the completion
 * callback fires only after the final step accepts.
 */
class PromptFlowTest {

    private ServerMock server;
    private TaskScheduler scheduler;
    private Prompts prompts;
    private Player player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        Plugin plugin = MockBukkit.createMockPlugin();
        scheduler = new TaskScheduler(plugin);

        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        prompts = new Prompts(messages, mock(ConfigService.class), scheduler);

        server.getPluginManager().registerEvents(new PlayerChatInput.ChatInputListener(), plugin);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
        MockBukkit.unmock();
    }

    /** Sends a chat line, then ticks the scheduler so the queued completion callback runs. */
    private void chat(String message) {
        server.getPluginManager().callEvent(new AsyncPlayerChatEvent(false, player, message, new HashSet<>()));
        server.getScheduler().performOneTick();
    }

    @Test
    void rejectedStepIsRepromptedAndFlowCompletesAfterLastStepAccepts() {
        List<String> seen = new ArrayList<>();
        boolean[] completed = {false};

        prompts.flow(player)
                .step("first", input -> {
                    seen.add(input);
                    return input.equals("ok"); // anything else re-prompts this step
                })
                .step("second", input -> {
                    seen.add(input);
                    return true;
                })
                .start(() -> completed[0] = true);

        chat("bad");
        assertEquals(List.of("bad"), seen, "first step saw the input");
        assertFalse(completed[0], "rejected input must not advance the flow");

        chat("ok");
        assertEquals(List.of("bad", "ok"), seen, "still on the first step until it accepts");
        assertFalse(completed[0], "advancing past the first step is not completion");

        chat("seed");
        assertEquals(List.of("bad", "ok", "seed"), seen, "second step saw the input");
        assertTrue(completed[0], "flow completes once the last step accepts");
    }
}
