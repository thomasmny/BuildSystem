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
package de.eintosti.buildsystem.protection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.test.TestData;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
class WorldProtectionPolicyTest {

    private WorldProtectionPolicy policy;
    private Player player;
    private BuildWorld world;
    private WorldPermissions permissions;
    private WorldData data;
    private Builders builders;

    @BeforeEach
    void setUp() {
        policy = new WorldProtectionPolicy();
        player = mock(Player.class);
        world = mock(BuildWorld.class);
        permissions = mock(WorldPermissions.class);
        data = mock(WorldData.class);
        builders = mock(Builders.class);

        when(world.getPermissions()).thenReturn(permissions);
        when(world.getData()).thenReturn(data);
        when(world.getBuilders()).thenReturn(builders);

        // Default: no bypass, non-archived, builders disabled, not a creator/builder
        when(permissions.canBypassBuildRestriction(player)).thenReturn(false);
        when(player.hasPermission("buildsystem.bypass.archive")).thenReturn(false);
        when(player.hasPermission("buildsystem.bypass.builders")).thenReturn(false);
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.NOT_STARTED);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(false);
        when(builders.isCreator(player)).thenReturn(false);
        when(builders.isBuilder(player)).thenReturn(false);
    }

    @Test
    void bypass_shortCircuitsEverything() {
        when(permissions.canBypassBuildRestriction(player)).thenReturn(true);
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.ARCHIVE_STATUS);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(true);

        assertEquals(Denial.NONE, policy.checkStatus(player, world));
        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
        assertEquals(Denial.NONE, policy.mayModify(player, world));
    }

    @Test
    void archiveBypassPermission_shortCircuitsStatusCheck() {
        when(player.hasPermission("buildsystem.bypass.archive")).thenReturn(true);
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.ARCHIVE_STATUS);

        assertEquals(Denial.NONE, policy.checkStatus(player, world));
    }

    @Test
    void archivedWorld_noBypass_returnsStatusLocked() {
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.ARCHIVE_STATUS);

        assertEquals(Denial.STATUS_LOCKED, policy.checkStatus(player, world));
    }

    @Test
    void nonArchivedWorld_returnsNone() {
        assertEquals(Denial.NONE, policy.checkStatus(player, world));
    }

    @Test
    void buildersEnabled_nonBuilder_notCreator_returnsNotABuilder() {
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(true);

        assertEquals(Denial.NOT_A_BUILDER, policy.checkBuilders(player, world));
    }

    @Test
    void buildersEnabled_isCreator_returnsNone() {
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(true);
        when(builders.isCreator(player)).thenReturn(true);

        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
    }

    @Test
    void buildersEnabled_isBuilder_returnsNone() {
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(true);
        when(builders.isBuilder(player)).thenReturn(true);

        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
    }

    @Test
    void buildersDisabled_returnsNone() {
        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
    }

    @Test
    void buildersPermission_shortCircuitsBuildersCheck() {
        when(player.hasPermission("buildsystem.bypass.builders")).thenReturn(true);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(true);

        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
    }

    @Test
    void settingDisabled_returnsSettingDisabled() {
        when(data.get(WorldDataKey.BLOCK_PLACEMENT)).thenReturn(false);
        assertEquals(Denial.SETTING_DISABLED, policy.checkSetting(player, world, WorldSetting.BLOCK_PLACEMENT));
    }

    @Test
    void settingEnabled_returnsNone() {
        when(data.get(WorldDataKey.BLOCK_PLACEMENT)).thenReturn(true);
        assertEquals(Denial.NONE, policy.checkSetting(player, world, WorldSetting.BLOCK_PLACEMENT));
    }

    @Test
    void mayModify_archive_winsOverBuilders() {
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.ARCHIVE_STATUS);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(true);

        // Archive denial takes precedence over builder denial
        assertEquals(Denial.STATUS_LOCKED, policy.mayModify(player, world));
    }

    @Test
    void mayModify_withSetting_settingDisabled_winsOverBuilders() {
        when(data.get(WorldDataKey.BLOCK_PLACEMENT)).thenReturn(false);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(true);

        assertEquals(Denial.SETTING_DISABLED, policy.mayModify(player, world, WorldSetting.BLOCK_PLACEMENT));
    }

    @Test
    void mayModify_withSetting_archiveWinsOverSetting() {
        when(data.get(WorldDataKey.STATUS)).thenReturn(TestData.ARCHIVE_STATUS);
        when(data.get(WorldDataKey.BLOCK_PLACEMENT)).thenReturn(false);

        assertEquals(Denial.STATUS_LOCKED, policy.mayModify(player, world, WorldSetting.BLOCK_PLACEMENT));
    }

    @Test
    void mayModify_allClear_returnsNone() {
        assertEquals(Denial.NONE, policy.mayModify(player, world));
    }
}
