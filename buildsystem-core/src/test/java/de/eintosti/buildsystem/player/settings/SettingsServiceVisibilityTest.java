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
package de.eintosti.buildsystem.player.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.test.VisibilityFixture;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Who sees whom after {@link SettingsService#updateVisibility}, and what is left after
 * {@link SettingsService#showAllPlayers} on disable.
 */
@NullMarked
class SettingsServiceVisibilityTest {

    private ServerMock server;
    private VisibilityFixture fixture;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        fixture = new VisibilityFixture(MockBukkit.createMockPlugin());
        settingsService = fixture.settingsService;
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    static Stream<Arguments> combinations() {
        List<Arguments> rows = new ArrayList<>();
        for (int bits = 0; bits < 32; bits++) {
            rows.add(
                    Arguments.of((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0, (bits & 16) != 0));
        }
        return rows.stream();
    }

    @ParameterizedTest(name = "player hides {0}, archived {1}; other hides {2}, archived {3}; vanish {4}")
    @MethodSource("combinations")
    void eachSideSeesTheOtherUnlessHidingOrVanished(
            boolean playerHides, boolean playerArchived, boolean otherHides, boolean otherArchived, boolean vanish) {
        fixture.archiveVanish(vanish);
        PlayerMock player = join("Player", playerHides, playerArchived);
        PlayerMock other = join("Other", otherHides, otherArchived);
        boolean otherSeesPlayer = !otherHides && !(vanish && playerArchived);
        boolean playerSeesOther = !playerHides && !(vanish && otherArchived);
        // Start from the opposite of what is expected, so a missing show or hide is noticed.
        setVisible(other, player, !otherSeesPlayer);
        setVisible(player, other, !playerSeesOther);

        settingsService.updateVisibility(player);

        assertEquals(otherSeesPlayer, other.canSee(player));
        assertEquals(playerSeesOther, player.canSee(other));
    }

    @Test
    void playerInvisibleByDefault_isNeverShown() {
        PlayerMock viewer = join("Viewer", false, false);
        PlayerMock target = join("Target", false, false);
        target.setVisibleByDefault(false);
        // MockBukkit's canSee ignores visible-by-default, so a hide stands in for it: a show would lift it.
        viewer.hidePlayer(fixture.plugin, target);

        settingsService.updateVisibility(target);
        settingsService.showAllPlayers();

        assertFalse(viewer.canSee(target));
    }

    @Test
    void anotherPluginsHide_survivesUpdateAndDisable() {
        PlayerMock viewer = join("Viewer", false, false);
        PlayerMock target = join("Target", false, false);
        // Plugins are equal by name, so the other plugin needs its own.
        viewer.hidePlayer(MockBukkit.createMockPlugin("OtherPlugin"), target);

        settingsService.updateVisibility(target);
        assertFalse(viewer.canSee(target));

        settingsService.showAllPlayers();
        assertFalse(viewer.canSee(target));
    }

    @Test
    void reload_disableLiftsTheOldHides_andEnableAppliesTheRuleAgain() {
        PlayerMock viewer = join("Viewer", true, false);
        PlayerMock target = join("Target", false, false);
        settingsService.updateVisibility(viewer);
        assertFalse(viewer.canSee(target));

        settingsService.showAllPlayers();
        assertTrue(viewer.canSee(target), "a new instance could not lift the old instance's hide");

        Plugin reloaded = MockBukkit.createMockPlugin("Reloaded");
        VisibilityFixture next = new VisibilityFixture(reloaded);
        next.hidePlayers(viewer, true);
        Bukkit.getOnlinePlayers().forEach(next.settingsService::updateVisibility);
        assertFalse(viewer.canSee(target));
    }

    private PlayerMock join(String name, boolean hidesOthers, boolean archived) {
        PlayerMock player = SoundlessPlayer.join(server, name);
        fixture.hidePlayers(player, hidesOthers);
        if (archived) {
            fixture.enterArchive(player);
        }
        return player;
    }

    private void setVisible(PlayerMock viewer, PlayerMock target, boolean visible) {
        if (visible) {
            viewer.showPlayer(fixture.plugin, target);
        } else {
            viewer.hidePlayer(fixture.plugin, target);
        }
    }
}
