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
package de.eintosti.buildsystem.storage.yaml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.settings.SettingsImpl;
import java.io.File;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlPlayerStorageRoundTripTest {

    @TempDir
    File dataFolder;

    private BuildSystemPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
    }

    private BuildPlayerImpl samplePlayer(UUID uuid) {
        SettingsImpl settings = new SettingsImpl();
        settings.setNavigatorType(NavigatorType.NEW);
        settings.setNoClip(true);
        settings.setNightVision(true);
        settings.setKeepNavigator(true);
        settings.setSlabBreaking(true);
        settings.setClearInventory(true);

        BuildPlayerImpl player = new BuildPlayerImpl(uuid, settings);
        player.setLogoutLocation(new LogoutLocation("lobby", 1.5, 64.0, -3.25, 90.0f, -10.0f));
        return player;
    }

    @Test
    void saveAndLoad_roundTripsSettingsAndLogoutLocation() throws Exception {
        UUID uuid = UUID.randomUUID();
        BuildPlayerImpl original = samplePlayer(uuid);

        new YamlPlayerStorage(plugin).save(original).join();

        Collection<BuildPlayer> loaded = new YamlPlayerStorage(plugin).load().get();
        assertEquals(1, loaded.size());

        BuildPlayerImpl reloaded = BuildPlayerImpl.of(loaded.iterator().next());
        assertEquals(uuid, reloaded.getUniqueId());
        assertEquals(NavigatorType.NEW, reloaded.getSettings().getNavigatorType());
        assertTrue(reloaded.getSettings().isNoClip());
        assertTrue(reloaded.getSettings().isNightVision());
        assertTrue(reloaded.getSettings().isKeepNavigator());
        assertTrue(reloaded.getSettings().isSlabBreaking());
        assertTrue(reloaded.getSettings().isClearInventory());
        assertFalse(reloaded.getSettings().isHidePlayers());

        LogoutLocation logoutLocation = reloaded.getLogoutLocation();
        assertNotNull(logoutLocation);
        assertEquals(original.getLogoutLocation().toString(), logoutLocation.toString());
    }

    @Test
    void load_missingFile_returnsEmptyCollection() throws Exception {
        Collection<BuildPlayer> loaded = new YamlPlayerStorage(plugin).load().get();

        assertTrue(loaded.isEmpty());
    }
}
