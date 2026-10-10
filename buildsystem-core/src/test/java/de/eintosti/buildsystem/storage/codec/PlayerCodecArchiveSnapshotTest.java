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
package de.eintosti.buildsystem.storage.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The archive snapshot is written with the player, so the inventory an archive world took survives a crash.
 */
@NullMarked
class PlayerCodecArchiveSnapshotTest {

    private final PlayerCodec codec = new PlayerCodec(Logger.getLogger("PlayerCodecArchiveSnapshotTest"));

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void archiveSnapshot_survivesTheRoundTrip_withItemMeta() {
        PlayerMock archived = new PlayerMock(server, "Archived");
        archived.setGameMode(GameMode.CREATIVE);
        archived.getInventory().setItem(0, sword());
        archived.getInventory().setItem(8, new ItemStack(Material.DIRT, 3));
        archived.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        BuildPlayerImpl stored = new BuildPlayerImpl(CodecSamples.PLAYER_ID, new SettingsImpl());
        stored.getCachedValues().saveArchiveState(archived);

        BuildPlayerImpl loaded = reload(stored);
        PlayerMock rejoined = new PlayerMock(server, "Archived");
        rejoined.setGameMode(GameMode.ADVENTURE);
        rejoined.getInventory().setItem(4, new ItemStack(Material.STICK));

        assertTrue(loaded.getCachedValues().resetArchiveStateIfPresent(rejoined));
        PlayerInventory inventory = rejoined.getInventory();
        assertEquals(GameMode.CREATIVE, rejoined.getGameMode());
        ItemStack sword = Objects.requireNonNull(inventory.getItem(0));
        assertEquals(Material.DIAMOND_SWORD, sword.getType());
        assertEquals("Excalibur", Objects.requireNonNull(sword.getItemMeta()).getDisplayName());
        assertEquals(5, sword.getEnchantmentLevel(Enchantment.SHARPNESS));
        assertEquals(Material.DIRT, Objects.requireNonNull(inventory.getItem(8)).getType());
        assertEquals(3, Objects.requireNonNull(inventory.getItem(8)).getAmount());
        assertNull(inventory.getItem(4), "the archive inventory is replaced, not merged");
        assertEquals(
                Material.DIAMOND_HELMET,
                Objects.requireNonNull(inventory.getHelmet()).getType());
    }

    @Test
    void archiveSnapshot_isTakenWhenSerialized() {
        PlayerMock archived = new PlayerMock(server, "Archived");
        archived.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        BuildPlayerImpl stored = new BuildPlayerImpl(CodecSamples.PLAYER_ID, new SettingsImpl());
        stored.getCachedValues().saveArchiveState(archived);
        ItemStack held = Objects.requireNonNull(stored.getCachedValues().getArchiveState())
                .inventory()[0];

        Map<String, Object> serialized = codec.serialize(stored);
        // The file is written later, off the main thread.
        Objects.requireNonNull(held).setAmount(1);

        BuildPlayerImpl loaded = codec.deserialize(
                CodecSamples.PLAYER_ID.toString(),
                CodecSamples.section(
                        CodecSamples.toYaml(Map.of(CodecSamples.PLAYER_ID.toString(), serialized)),
                        CodecSamples.PLAYER_ID.toString()));
        PlayerMock rejoined = new PlayerMock(server, "Archived");
        loaded.getCachedValues().resetArchiveStateIfPresent(rejoined);
        assertEquals(
                5, Objects.requireNonNull(rejoined.getInventory().getItem(0)).getAmount());
    }

    @Test
    void playerWithoutSnapshot_writesNoSnapshot() {
        BuildPlayerImpl player = new BuildPlayerImpl(CodecSamples.PLAYER_ID, new SettingsImpl());

        assertFalse(codec.serialize(player).containsKey("archive-snapshot"));
        assertFalse(reload(player).getCachedValues().hasArchiveState());
    }

    private BuildPlayerImpl reload(BuildPlayerImpl player) {
        String key = codec.key(player);
        String yaml = CodecSamples.toYaml(Map.of(key, codec.serialize(player)));
        return codec.deserialize(key, CodecSamples.section(yaml, key));
    }

    private static ItemStack sword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = Objects.requireNonNull(sword.getItemMeta());
        meta.setDisplayName("Excalibur");
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        sword.setItemMeta(meta);
        return sword;
    }
}
