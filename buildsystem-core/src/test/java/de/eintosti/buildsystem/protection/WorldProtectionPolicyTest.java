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

import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        Type<BuildWorldStatus> status = mockType(BuildWorldStatus.CITY);
        when(data.status()).thenReturn(status);
        Type<Boolean> buildersEnabled = mockType(false);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);
        when(builders.isCreator(player)).thenReturn(false);
        when(builders.isBuilder(player)).thenReturn(false);
    }

    @Test
    void bypass_shortCircuitsEverything() {
        when(permissions.canBypassBuildRestriction(player)).thenReturn(true);
        Type<BuildWorldStatus> archived = mockType(BuildWorldStatus.ARCHIVE);
        when(data.status()).thenReturn(archived);
        Type<Boolean> buildersEnabled = mockType(true);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);

        assertEquals(Denial.NONE, policy.checkArchive(player, world));
        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
        assertEquals(Denial.NONE, policy.mayModify(player, world));
    }

    @Test
    void archiveBypassPermission_shortCircuitsArchiveCheck() {
        when(player.hasPermission("buildsystem.bypass.archive")).thenReturn(true);
        Type<BuildWorldStatus> archived = mockType(BuildWorldStatus.ARCHIVE);
        when(data.status()).thenReturn(archived);

        assertEquals(Denial.NONE, policy.checkArchive(player, world));
    }

    @Test
    void archivedWorld_noBypass_returnsArchived() {
        Type<BuildWorldStatus> archived = mockType(BuildWorldStatus.ARCHIVE);
        when(data.status()).thenReturn(archived);

        assertEquals(Denial.ARCHIVED, policy.checkArchive(player, world));
    }

    @Test
    void nonArchivedWorld_returnsNone() {
        assertEquals(Denial.NONE, policy.checkArchive(player, world));
    }

    @Test
    void buildersEnabled_nonBuilder_notCreator_returnsNotABuilder() {
        Type<Boolean> buildersEnabled = mockType(true);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);

        assertEquals(Denial.NOT_A_BUILDER, policy.checkBuilders(player, world));
    }

    @Test
    void buildersEnabled_isCreator_returnsNone() {
        Type<Boolean> buildersEnabled = mockType(true);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);
        when(builders.isCreator(player)).thenReturn(true);

        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
    }

    @Test
    void buildersEnabled_isBuilder_returnsNone() {
        Type<Boolean> buildersEnabled = mockType(true);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);
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
        Type<Boolean> buildersEnabled = mockType(true);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);

        assertEquals(Denial.NONE, policy.checkBuilders(player, world));
    }

    @Test
    void settingDisabled_returnsSettingDisabled() {
        Type<Boolean> setting = mockType(false);
        assertEquals(Denial.SETTING_DISABLED, policy.checkSetting(player, world, setting));
    }

    @Test
    void settingEnabled_returnsNone() {
        Type<Boolean> setting = mockType(true);
        assertEquals(Denial.NONE, policy.checkSetting(player, world, setting));
    }

    @Test
    void mayModify_archive_winsOverBuilders() {
        Type<BuildWorldStatus> archived = mockType(BuildWorldStatus.ARCHIVE);
        when(data.status()).thenReturn(archived);
        Type<Boolean> buildersEnabled = mockType(true);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);

        // Archive denial takes precedence over builder denial
        assertEquals(Denial.ARCHIVED, policy.mayModify(player, world));
    }

    @Test
    void mayModify_withSetting_settingDisabled_winsOverBuilders() {
        Type<Boolean> setting = mockType(false);
        Type<Boolean> buildersEnabled = mockType(true);
        when(data.buildersEnabled()).thenReturn(buildersEnabled);

        assertEquals(Denial.SETTING_DISABLED, policy.mayModify(player, world, setting));
    }

    @Test
    void mayModify_withSetting_archiveWinsOverSetting() {
        Type<BuildWorldStatus> archived = mockType(BuildWorldStatus.ARCHIVE);
        when(data.status()).thenReturn(archived);
        Type<Boolean> setting = mockType(false);

        assertEquals(Denial.ARCHIVED, policy.mayModify(player, world, setting));
    }

    @Test
    void mayModify_allClear_returnsNone() {
        assertEquals(Denial.NONE, policy.mayModify(player, world));
    }

    @SuppressWarnings("unchecked")
    private static <T> Type<T> mockType(T value) {
        Type<T> t = mock(Type.class);
        when(t.get()).thenReturn(value);
        return t;
    }
}
