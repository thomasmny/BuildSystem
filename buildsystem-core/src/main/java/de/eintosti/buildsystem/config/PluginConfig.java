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
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.world.menu.GameRuleEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PluginConfig(Settings settings, Storage storage, World world, Folder folder) {

    /**
     * Credentials for the external services the plugin can talk to, defined once at the root rather than inside the
     * feature that happened to need them first. Backups and world downloads both select a backend by name and read it
     * from here, so a bucket configured for one is the same bucket for the other.
     *
     * @param s3 The S3 or S3-compatible service
     * @param sftp The SFTP server
     */
    public record Storage(S3 s3, Sftp sftp) {

        /**
         * Which backend a feature stores its files on.
         */
        public enum Type {

            /**
             * The server's own disk, under the plugin folder.
             */
            LOCAL,

            /**
             * The bucket configured in {@code storage.s3}.
             */
            S3,

            /**
             * The server configured in {@code storage.sftp}.
             */
            SFTP
        }

        public record Sftp(
                @Nullable String host,
                int port,
                @Nullable String username,
                @Nullable String password) {

            /**
             * {@return the password, preferring {@code BUILDSYSTEM_SFTP_PASSWORD}} Lets operators keep the secret out
             * of config.yml.
             */
            public @Nullable String resolvedPassword() {
                return envOrConfig("BUILDSYSTEM_SFTP_PASSWORD", password);
            }

            /**
             * Overridden so the password never appears in a log or pasted support output.
             */
            @Override
            public String toString() {
                return "Sftp[host=%s, port=%d, username=%s, password=%s]"
                        .formatted(host, port, username, password == null ? null : "***");
            }
        }

        public record S3(
                @Nullable String url,
                @Nullable String accessKey,
                @Nullable String secretKey,
                @Nullable String region,
                @Nullable String bucket) {

            /**
             * {@return the access key, preferring {@code AWS_ACCESS_KEY_ID}} Lets operators keep the secret out of
             * config.yml.
             */
            public @Nullable String resolvedAccessKey() {
                return envOrConfig("AWS_ACCESS_KEY_ID", accessKey);
            }

            /**
             * {@return the secret key, preferring {@code AWS_SECRET_ACCESS_KEY}}
             */
            public @Nullable String resolvedSecretKey() {
                return envOrConfig("AWS_SECRET_ACCESS_KEY", secretKey);
            }

            /**
             * Overridden so the access key and secret key never appear in a log or pasted support output.
             */
            @Override
            public String toString() {
                return "S3[url=%s, accessKey=%s, secretKey=%s, region=%s, bucket=%s]"
                        .formatted(
                                url,
                                accessKey == null ? null : "***",
                                secretKey == null ? null : "***",
                                region,
                                bucket);
            }
        }

        private static @Nullable String envOrConfig(String envKey, @Nullable String configValue) {
            String env = System.getenv(envKey);
            return env == null || env.isBlank() ? configValue : env;
        }
    }

    public record Settings(
            boolean updateChecker,
            boolean scoreboard,
            List<String> worldPermissionWhitelist,
            boolean spawnTeleportMessage,
            boolean joinQuitMessages,
            String dateFormat,
            Archive archive,
            SaveFromDeath saveFromDeath,
            BuildMode buildMode,
            Builder builder,
            Navigator navigator) {

        public record Archive(boolean vanish, boolean changeGamemode, GameMode worldGameMode) {}

        public record SaveFromDeath(boolean enabled, boolean teleportToMapSpawn) {}

        public record BuildMode(boolean dropItems, boolean moveItems) {}

        public record Builder(boolean blockWorldEditNonBuilder, XMaterial worldEditWand) {}

        public record Navigator(XMaterial item, boolean giveItemOnJoin) {}
    }

    public record World(
            boolean lockWeather,
            String invalidCharacters,
            int importAllDelay,
            Set<String> deletionBlacklist,
            VoidBlock voidBlock,
            Limits limits,
            Defaults defaults,
            Unload unload,
            Backup backup,
            Download download) {

        public World {
            deletionBlacklist = Set.copyOf(deletionBlacklist);
        }

        /**
         * The block placed at the spawn of newly generated void worlds, so players do not fall on first join. The
         * material is resolved and validated at parse time, so it is always a placeable block.
         */
        public record VoidBlock(boolean enabled, Material material) {}

        /**
         * Fallback world-creation limits, applied only to players holding no
         * {@code buildsystem.create.<visibility>.<amount>} permission node, so a permission grant always wins. Counted
         * per player and per visibility; {@code -1} means unlimited.
         *
         * @param publicWorlds Default maximum number of public worlds one player may create
         * @param privateWorlds Default maximum number of private worlds one player may create
         */
        public record Limits(int publicWorlds, int privateWorlds) {}

        public record Defaults(
                int worldBorderSize,
                Difficulty difficulty,
                List<GameRuleEntry<?>> gameRules,
                Permission permission,
                Time time,
                boolean physics,
                Map<PhysicsCategory, Boolean> physicsExceptions,
                boolean explosions,
                boolean mobAi,
                boolean blockBreaking,
                boolean blockPlacement,
                boolean blockInteractions,
                BuildersEnabled buildersEnabled) {

            public Defaults {
                physicsExceptions = Map.copyOf(physicsExceptions);
            }

            /**
             * The default for a {@link PhysicsCategory} on worlds that have never stored their own value:
             * {@code true} means the behavior still runs while the world's physics are disabled. Unlisted categories
             * default to blocked, matching vanilla physics-off behavior.
             */
            public boolean physicsException(PhysicsCategory category) {
                return physicsExceptions.getOrDefault(category, false);
            }

            public record Permission(String publicPermission, String privatePermission) {}

            public record Time(int sunrise, int noon, int night) {}

            public record BuildersEnabled(boolean publicBuilders, boolean privateBuilders) {}
        }

        public record Unload(boolean enabled, String timeUntilUnload, Set<String> blacklistedWorlds) {

            public Unload {
                blacklistedWorlds = Set.copyOf(blacklistedWorlds);
            }
        }

        /**
         * @param maxBackupsPerWorld How many backups are kept per world
         * @param storage Which backend backups are written to, configured under the root {@code storage} section
         * @param path Where backups live within that backend
         * @param autoBackup The scheduled backup settings
         */
        public record Backup(int maxBackupsPerWorld, Storage.Type storage, String path, AutoBackup autoBackup) {

            public record AutoBackup(boolean enabled, boolean onlyActiveWorlds, int interval) {}
        }

        /**
         * The built-in HTTP server behind {@code /worlds download}, disabled by default: enabling it opens a port and
         * hands out world archives, so it stays an explicit decision by the operator.
         *
         * @param enabled Whether the download server runs at all
         * @param storage Where a prepared archive is served from
         * @param port The port the server listens on
         * @param url The base URL players are sent, for servers reached through a proxy or a domain
         * @param behindProxy Whether to take the client's address from {@code X-Forwarded-For} rather than the socket
         * @param expirationMinutes How long a download link stays valid before the archive is deleted
         */
        public record Download(
                boolean enabled,
                Storage.Type storage,
                int port,
                String url,
                boolean behindProxy,
                int expirationMinutes,
                int maxSizeMb,
                int maxStorageMb,
                int maxConcurrentDownloads) {}
    }

    public record Folder(boolean overridePermissions, boolean overrideProjects) {}
}
