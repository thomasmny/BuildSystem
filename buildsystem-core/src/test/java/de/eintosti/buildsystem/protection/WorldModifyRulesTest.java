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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The truth table for "may this player modify this world": every combination of the inputs, for a plain modification
 * and for each {@link WorldSetting}, runs through {@link WorldProtectionPolicy#mayModify} and
 * {@link WorldPermissionsImpl#canModify} and is checked against {@link Row#expected()}, a separate written-out rule set.
 */
@NullMarked
class WorldModifyRulesTest {

    private static final int INPUTS = 10;

    private final WorldContext context = TestData.worldContext();

    static Stream<Arguments> settings() {
        return Stream.concat(
                Stream.of(Arguments.of((WorldSetting) null)),
                Arrays.stream(WorldSetting.values()).map(Arguments::of));
    }

    @ParameterizedTest(name = "setting: {0}")
    @MethodSource("settings")
    void policyAndPermissionsFollowTheRules(@Nullable WorldSetting setting) {
        List<String> mismatches = new ArrayList<>();
        for (int bits = 0; bits < 1 << INPUTS; bits++) {
            Row row = new Row(bits, setting);
            BuildWorld world = world(row);
            Player player = row.player;

            Denial expected = row.expected();
            Denial denial = setting == null
                    ? new WorldProtectionPolicy().mayModify(player, world)
                    : new WorldProtectionPolicy().mayModify(player, world, setting);
            boolean byPermissions = setting == null
                    ? world.getPermissions().canModify(player)
                    : world.getPermissions().canModify(player, setting);

            if (denial != expected || byPermissions != (expected == Denial.NONE)) {
                mismatches.add(
                        "%s expected=%s policy=%s permissions=%s".formatted(row, expected, denial, byPermissions));
            }
        }
        assertEquals(List.of(), mismatches);
    }

    @ParameterizedTest(name = "setting: {0}")
    @MethodSource("settings")
    void anAdmin_passesALockedWorldWithTheSettingOffAsANonBuilder(@Nullable WorldSetting setting) {
        // Admin, a locked status and builders on; the setting is off and the player is no builder.
        Row row = new Row(2 | 8 | 32, setting);
        BuildWorld world = world(row);

        Denial denial = setting == null
                ? new WorldProtectionPolicy().mayModify(row.player, world)
                : new WorldProtectionPolicy().mayModify(row.player, world, setting);

        assertEquals(Denial.NONE, denial);
    }

    private static WorldDataKey<Boolean> keyOf(WorldSetting setting) {
        return switch (setting) {
            case BLOCK_BREAKING -> WorldDataKey.BLOCK_BREAKING;
            case BLOCK_PLACEMENT -> WorldDataKey.BLOCK_PLACEMENT;
            case BLOCK_INTERACTIONS -> WorldDataKey.BLOCK_INTERACTIONS;
        };
    }

    private BuildWorld world(Row row) {
        when(context.playerService().isInBuildMode(row.player)).thenReturn(row.buildMode);

        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.STATUS)).thenReturn(row.locked ? TestData.ARCHIVE_STATUS : TestData.NOT_STARTED);
        when(data.get(WorldDataKey.BUILDERS_ENABLED)).thenReturn(row.buildersEnabled);
        if (row.setting != null) {
            when(data.get(keyOf(row.setting))).thenReturn(row.settingEnabled);
        }

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
                settingEnabled,
                bypassSettings;
        final @Nullable WorldSetting setting;
        final Player player = mock(Player.class);

        Row(int bits, @Nullable WorldSetting setting) {
            this.setting = setting;
            buildMode = (bits & 1) != 0;
            admin = (bits & 2) != 0;
            bypassArchive = (bits & 4) != 0;
            locked = (bits & 8) != 0;
            bypassBuilders = (bits & 16) != 0;
            buildersEnabled = (bits & 32) != 0;
            creator = (bits & 64) != 0;
            builder = (bits & 128) != 0;
            settingEnabled = (bits & 256) != 0;
            bypassSettings = (bits & 512) != 0;

            when(player.hasPermission(Permissions.ADMIN)).thenReturn(admin);
            when(player.hasPermission(Permissions.BYPASS_ARCHIVE)).thenReturn(bypassArchive);
            when(player.hasPermission(Permissions.BYPASS_BUILDERS)).thenReturn(bypassBuilders);
            when(player.hasPermission("buildsystem.bypass.settings")).thenReturn(bypassSettings);
        }

        /**
         * The rules: build mode and admin allow everything; a locked status needs the archive bypass; a setting-gated
         * action needs the setting on or the settings bypass, which also excuses the builder check; and with the
         * builders feature on, only the creator, a builder or a builders-bypass holder may build.
         */
        Denial expected() {
            if (buildMode || admin) {
                return Denial.NONE;
            }
            if (locked && !bypassArchive) {
                return Denial.STATUS_LOCKED;
            }
            if (setting != null && bypassSettings) {
                return Denial.NONE;
            }
            if (setting != null && !settingEnabled) {
                return Denial.SETTING_DISABLED;
            }
            return creator || builder || bypassBuilders || !buildersEnabled ? Denial.NONE : Denial.NOT_A_BUILDER;
        }

        @Override
        public String toString() {
            return "buildMode=%s admin=%s bypassArchive=%s locked=%s bypassBuilders=%s buildersEnabled=%s creator=%s"
                            .formatted(
                                    buildMode, admin, bypassArchive, locked, bypassBuilders, buildersEnabled, creator)
                    + " builder=%s settingEnabled=%s bypassSettings=%s"
                            .formatted(builder, settingEnabled, bypassSettings);
        }
    }
}
