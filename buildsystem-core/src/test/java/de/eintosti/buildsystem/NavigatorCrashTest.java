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
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockConstruction;

import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedConstruction;

/**
 * Drives the real plugin through a join after a crash that happened while the player had the armor-stand navigator
 * open. The server saved the player with everything the navigator applied, and the plugin no longer knows it was open.
 */
@NullMarked
class NavigatorCrashTest {

    private ServerMock server;
    private MockedConstruction<BuildSystemMetrics> metrics;
    private BuildSystemPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // bStats refuses to run unrelocated, which it only is in the shaded jar.
        metrics = mockConstruction(BuildSystemMetrics.class);
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(BuildSystemPlugin.class);
        // The scoreboard needs a real server class, so it is turned off before anyone joins.
        plugin.getConfig().set("settings.scoreboard", false);
        plugin.saveConfig();
        plugin.reloadConfigData(false);
    }

    @AfterEach
    void tearDown() {
        metrics.close();
        MockBukkit.unmock();
    }

    @Test
    void joiningWithTheNavigatorsLeftovers_clearsThem() {
        SoundlessPlayer player = new SoundlessPlayer(server, "Player");
        player.getInventory().setItem(3, barrier());
        player.setWalkSpeed(0);
        player.setFlySpeed(0);
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, false, false));
        // Versions before 3.0 used the longest duration instead of an infinite one.
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false));

        server.addPlayer(player);

        assertFalse(player.getInventory().contains(Material.BARRIER));
        ItemStack navigator = player.getInventory().getItem(3);
        assertNotNull(navigator);
        assertEquals(Material.CLOCK, navigator.getType(), "the navigator is back where the barrier was");
        assertEquals(0.2f, player.getWalkSpeed());
        assertEquals(0.1f, player.getFlySpeed());
        assertNull(player.getPotionEffect(PotionEffectType.BLINDNESS));
        assertNull(player.getPotionEffect(PotionEffectType.JUMP_BOOST));
    }

    @Test
    void enabling_clearsTheLeftoversADisableThatThrewNeverReached() {
        SoundlessPlayer player = SoundlessPlayer.join(server, "Player");
        server.getPluginManager().disablePlugin(plugin);
        player.getInventory().setItem(3, barrier());
        player.setWalkSpeed(0);
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, false, false));

        server.getPluginManager().enablePlugin(plugin);

        assertFalse(player.getInventory().contains(Material.BARRIER));
        assertEquals(0.2f, player.getWalkSpeed());
        assertNull(player.getPotionEffect(PotionEffectType.BLINDNESS));
    }

    @Test
    void joiningWithOtherEffectsAndSpeeds_keepsThem() {
        SoundlessPlayer player = new SoundlessPlayer(server, "Player");
        player.setWalkSpeed(0.5f);
        player.setFlySpeed(0.3f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0));
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 1, false, false));

        server.addPlayer(player);

        assertEquals(0.5f, player.getWalkSpeed());
        assertEquals(0.3f, player.getFlySpeed());
        assertNotNull(player.getPotionEffect(PotionEffectType.BLINDNESS));
        assertNotNull(player.getPotionEffect(PotionEffectType.JUMP_BOOST));
    }

    private ItemStack barrier() {
        return ItemBuilder.of(Material.BARRIER)
                .pdc(new NamespacedKey(plugin, "navigator_barrier"), PersistentDataType.BOOLEAN, true)
                .build();
    }
}
