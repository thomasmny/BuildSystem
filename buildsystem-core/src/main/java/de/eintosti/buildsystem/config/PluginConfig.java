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
package de.eintosti.buildsystem.config;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.world.menu.GameRuleEntry;
import java.util.List;
import java.util.Set;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PluginConfig(
        Messages messages,
        Settings settings,
        World world,
        Folder folder
) {

    public record Messages(
            boolean spawnTeleportMessage,
            boolean joinQuitMessages,
            String dateFormat
    ) {

    }

    public record Settings(
            boolean updateChecker,
            boolean scoreboard,
            Archive archive,
            DisabledPhysics disabledPhysics,
            SaveFromDeath saveFromDeath,
            BuildMode buildMode,
            Builder builder,
            Navigator navigator
    ) {

        public record Archive(boolean vanish, boolean changeGamemode, GameMode worldGameMode) {

        }

        public record DisabledPhysics(boolean preventConnections, boolean preventFluidFlow, boolean preventFallingBlocks) {

        }

        public record SaveFromDeath(boolean enabled, boolean teleportToMapSpawn) {

        }

        public record BuildMode(boolean dropItems, boolean moveItems) {

        }

        public record Builder(boolean blockWorldEditNonBuilder, XMaterial worldEditWand) {

        }

        public record Navigator(XMaterial item, boolean giveItemOnJoin) {

        }
    }

    public record World(
            boolean lockWeather,
            String invalidCharacters,
            int importAllDelay,
            Set<String> deletionBlacklist,
            Default defaults,
            Limits limits,
            Unload unload,
            Backup backup
    ) {

        public World {
            deletionBlacklist = Set.copyOf(deletionBlacklist);
        }

        public record Default(
                int worldBorderSize,
                Difficulty difficulty,
                List<GameRuleEntry<?>> gameRules,
                Permission permission,
                Time time,
                DefaultSettings settings
        ) {

            public record Permission(String publicPermission, String privatePermission) {

            }

            public record Time(int sunrise, int noon, int night) {

            }

            public record DefaultSettings(
                    boolean physics,
                    boolean explosions,
                    boolean mobAi,
                    boolean blockBreaking,
                    boolean blockPlacement,
                    boolean blockInteractions,
                    BuildersEnabled buildersEnabled
            ) {

                public record BuildersEnabled(boolean publicBuilders, boolean privateBuilders) {

                }
            }
        }

        public record Limits(int publicWorlds, int privateWorlds) {

        }

        public record Unload(boolean enabled, String timeUntilUnload, Set<String> blacklistedWorlds) {

            public Unload {
                blacklistedWorlds = Set.copyOf(blacklistedWorlds);
            }
        }

        public record Backup(
                int maxBackupsPerWorld,
                StorageSettings storage,
                AutoBackup autoBackup
        ) {

            public sealed interface StorageSettings permits Local, Sftp, S3 {

            }

            public record Local() implements StorageSettings {

            }

            public record Sftp(@Nullable String host, int port, @Nullable String username, @Nullable String password, @Nullable String path) implements StorageSettings {

            }

            public record S3(@Nullable String url, @Nullable String accessKey, @Nullable String secretKey, @Nullable String region, @Nullable String bucket,
                             @Nullable String path) implements StorageSettings {

            }

            public record AutoBackup(boolean enabled, boolean onlyActiveWorlds, int interval) {

            }
        }
    }

    public record Folder(boolean overridePermissions, boolean overrideProjects) {

    }
}
