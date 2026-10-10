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
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.lifecycle.WorldPermissionsImpl;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Runs every combination of the inputs that decide "may this player modify this world" through both
 * {@link WorldPermissionsImpl#canModify} and {@link WorldProtectionPolicy#mayModify}, and checks them against one
 * written-out rule set. The block listeners use the first and the setting listeners and WorldEdit the second, so they
 * must never disagree.
 */
@NullMarked
class WorldModifyParityTest {

    private static final int INPUTS = 11;

    private final WorldContext context = TestData.worldContext();

    @Test
    void permissionsAndPolicyAgreeWithTheRules() {
        List<String> mismatches = new ArrayList<>();
        for (int bits = 0; bits < 1 << INPUTS; bits++) {
            Row row = new Row(bits);
            BuildWorld world = world(row);
            Player player = row.player;
            @Nullable WorldSetting setting = row.withSetting ? WorldSetting.BLOCK_PLACEMENT : null;

            boolean expected = row.expected();
            boolean byPermissions = setting == null
                    ? world.getPermissions().canModify(player)
                    : world.getPermissions().canModify(player, setting);
            Denial denial = setting == null
                    ? new WorldProtectionPolicy().mayModify(player, world)
                    : new WorldProtectionPolicy().mayModify(player, world, setting);

            if (byPermissions != expected || (denial == Denial.NONE) != expected) {
                mismatches.add(
                        "%s expected=%s permissions=%s policy=%s".formatted(row, expected, byPermissions, denial));
            }
        }
        assertEquals(List.of(), mismatches);
    }

    private BuildWorld world(Row row) {
        when(context.playerService().isInBuildMode(row.player)).thenReturn(row.buildMode);

        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS)).thenReturn(row.locked ? TestData.ARCHIVE_STATUS : TestData.NOT_STARTED);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(row.buildersEnabled);
        when(data.get(WorldDataKey.BLOCK_PLACEMENT)).thenReturn(row.settingEnabled);

        Builders builders = mock(Builders.class);
        when(builders.isCreator(row.player)).thenReturn(row.creator);
        when(builders.isBuilder(row.player)).thenReturn(row.builder);

        BuildWorld world = mock(BuildWorld.class);
        when(world.getData()).thenReturn(data);
        when(world.getBuilders()).thenReturn(builders);
        WorldPermissionsImpl permissions = WorldPermissionsImpl.of(context, world);
        when(world.getPermissions()).thenReturn(permissions);
        return world;
    }

    private static final class Row {

        final boolean buildMode,
                admin,
                bypassArchive,
                locked,
                bypassBuilders,
                buildersEnabled,
                creator,
                builder,
                withSetting,
                settingEnabled,
                bypassSettings;
        final Player player = mock(Player.class);

        Row(int bits) {
            buildMode = (bits & 1) != 0;
            admin = (bits & 2) != 0;
            bypassArchive = (bits & 4) != 0;
            locked = (bits & 8) != 0;
            bypassBuilders = (bits & 16) != 0;
            buildersEnabled = (bits & 32) != 0;
            creator = (bits & 64) != 0;
            builder = (bits & 128) != 0;
            withSetting = (bits & 256) != 0;
            settingEnabled = (bits & 512) != 0;
            bypassSettings = (bits & 1024) != 0;

            when(player.hasPermission(Permissions.ADMIN)).thenReturn(admin);
            when(player.hasPermission(Permissions.BYPASS_ARCHIVE)).thenReturn(bypassArchive);
            when(player.hasPermission(Permissions.BYPASS_BUILDERS)).thenReturn(bypassBuilders);
            when(player.hasPermission(WorldSetting.BLOCK_PLACEMENT.getBypassPermission()))
                    .thenReturn(bypassSettings);
        }

        /**
         * The rules: build mode and admin allow everything; a locked status needs the archive bypass; a setting-gated
         * action needs the setting on or the settings bypass, which also excuses the builder check; and with the
         * builders feature on, only the creator, a builder or a builders-bypass holder may build.
         */
        boolean expected() {
            if (buildMode || admin) {
                return true;
            }
            if (locked && !bypassArchive) {
                return false;
            }
            if (withSetting && (bypassSettings || !settingEnabled)) {
                return bypassSettings;
            }
            return creator || builder || bypassBuilders || !buildersEnabled;
        }

        @Override
        public String toString() {
            return "buildMode=%s admin=%s bypassArchive=%s locked=%s bypassBuilders=%s buildersEnabled=%s creator=%s"
                            .formatted(
                                    buildMode, admin, bypassArchive, locked, bypassBuilders, buildersEnabled, creator)
                    + " builder=%s withSetting=%s settingEnabled=%s bypassSettings=%s"
                            .formatted(builder, withSetting, settingEnabled, bypassSettings);
        }
    }
}
