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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.i18n.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@NullMarked
class HelpPagesTest {

    private static final int COMMANDS = 10;

    @ParameterizedTest
    @CsvSource({"1, 1, 7", "2, 2, 3", "0, 1, 7", "-3, 1, 7", "99, 2, 3"})
    void send_clampsThePageIntoRange(int requested, int shown, int commandLines) {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any(CommandSender.class))).thenReturn("%page%/%max%");
        List<String> lines = new ArrayList<>();
        Player player = mock(Player.class);
        Player.Spigot spigot = mock(Player.Spigot.class);
        when(player.spigot()).thenReturn(spigot);
        doAnswer(invocation -> {
                    lines.add(((BaseComponent) invocation.getArgument(0)).toPlainText());
                    return null;
                })
                .when(spigot)
                .sendMessage(any(BaseComponent.class));

        List<TextComponent> commands = IntStream.range(0, COMMANDS)
                .mapToObj(i -> new TextComponent("cmd" + i))
                .toList();
        new HelpPages(messages, "title", "permission").send(player, requested, commands);

        assertEquals(shown + "/2\n", lines.get(1));
        assertEquals(commandLines + 3, lines.size());
    }
}
