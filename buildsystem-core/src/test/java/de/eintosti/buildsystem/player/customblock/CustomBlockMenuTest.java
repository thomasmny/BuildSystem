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
package de.eintosti.buildsystem.player.customblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.customblock.CustomBlockMenu.BlockEntry;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Golden test pinning the {@link CustomBlockMenu} slot &rarr; block selection grid. Built through the real production
 * constructor under a {@link MockBukkit} server.
 */
@NullMarked
class CustomBlockMenuTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private CustomBlockMenu menu() {
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any())).thenReturn("Title");
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class);
        when(plugin.getMessages()).thenReturn(messages);
        Player player = server.addPlayer();
        return new CustomBlockMenu(plugin, player);
    }

    @Test
    void blockBySlot_hasAllTwentySixEntries() {
        assertEquals(26, menu().blockBySlot().size());
    }

    @Test
    void blockBySlot_mapsSpotCheckedSlots() {
        Map<Integer, BlockEntry> blocks = menu().blockBySlot();

        assertEquals(CustomBlock.FULL_OAK_BARCH, blocks.get(1).block());
        assertEquals(CustomBlock.MUSHROOM_BLOCK, blocks.get(14).block());
        assertEquals(CustomBlock.SMOOTH_STONE, blocks.get(19).block());
        assertEquals(CustomBlock.COMMAND_BLOCK, blocks.get(31).block());
        assertEquals(CustomBlock.DRAGON_EGG, blocks.get(40).block());
    }

    @Test
    void blockBySlot_defaultGiveMaterialIsPlayerHead() {
        assertEquals(XMaterial.PLAYER_HEAD, menu().blockBySlot().get(1).giveMaterial());
    }

    @Test
    void blockBySlot_specialSlotsCarryTheirNonSkullGiveMaterial() {
        Map<Integer, BlockEntry> blocks = menu().blockBySlot();

        assertEquals(CustomBlock.BARRIER, blocks.get(32).block());
        assertEquals(XMaterial.BARRIER, blocks.get(32).giveMaterial());

        assertEquals(CustomBlock.INVISIBLE_ITEM_FRAME, blocks.get(33).block());
        assertEquals(XMaterial.ITEM_FRAME, blocks.get(33).giveMaterial());

        assertEquals(CustomBlock.DEBUG_STICK, blocks.get(41).block());
        assertEquals(XMaterial.DEBUG_STICK, blocks.get(41).giveMaterial());
    }

    @Test
    void blockBySlot_glassAndEmptySlotsAreAbsent() {
        Map<Integer, BlockEntry> blocks = menu().blockBySlot();
        assertFalse(blocks.containsKey(0)); // glass
        assertFalse(blocks.containsKey(7)); // empty
        assertFalse(blocks.containsKey(15)); // empty
    }
}
