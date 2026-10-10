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

import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.storage.StorageTls;
import de.eintosti.buildsystem.storage.StorageType;
import de.eintosti.buildsystem.world.menu.GameRuleEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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
     * @param database Where worlds, folders and players are kept
     */
    public record Storage(S3 s3, Sftp sftp, Database database) {

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
                @Nullable String bucket,
                @Nullable String path) {

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
                return "S3[url=%s, accessKey=%s, secretKey=%s, region=%s, bucket=%s, path=%s]"
                        .formatted(
                                url,
                                accessKey == null ? null : "***",
                                secretKey == null ? null : "***",
                                region,
                                bucket,
                                path);
            }
        }

        /**
         * Where worlds, folders and players are kept. The values are checked here, so a config that could reach a
         * table, file or server it should not fails to load instead.
         *
         * @param type The backend in use
         * @param tablePrefix Put in front of every table and collection name
         * @param servers The connection block of every type that has one
         */
        public record Database(StorageType type, String tablePrefix, Map<StorageType, Server> servers) {

            private static final Pattern TABLE_PREFIX = Pattern.compile("[a-z_][a-z0-9_]{0,31}|");

            public Database {
                if (!TABLE_PREFIX.matcher(tablePrefix).matches()) {
                    throw new IllegalArgumentException("storage.table-prefix may only hold lowercase letters, digits"
                            + " and underscores, at most 32 of them, and may not start with a digit");
                }
                servers = Map.copyOf(servers);
            }

            /**
             * {@return the connection block of {@code type}}
             *
             * @throws IllegalArgumentException If the type has no server to connect to
             */
            public Server server(StorageType type) {
                Server server = servers.get(type);
                if (server == null) {
                    throw new IllegalArgumentException(type.displayName() + " has no server to connect to");
                }
                return server;
            }

            /**
             * One {@code storage.<type>} block. Each value ends up in a connection address, so none may carry
             * anything that would change what that address points to.
             */
            public record Server(
                    StorageType type,
                    String host,
                    int port,
                    String database,
                    String username,
                    @Nullable String password,
                    StorageTls tls) {

                // Only names and addresses: drivers read options and further hosts from characters like ( = , @. The _
                // is for Docker service names.
                private static final Pattern HOST = Pattern.compile("[A-Za-z0-9._-]{1,253}|\\[[0-9A-Fa-f:.]{2,45}]");
                private static final Pattern DATABASE = Pattern.compile("[A-Za-z0-9_-]{1,64}");
                // The PostgreSQL driver sends the login as NUL-separated startup parameters, so a NUL would add one.
                private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

                public Server {
                    String block = "storage." + type.key() + ".";
                    if (!HOST.matcher(host).matches()) {
                        throw new IllegalArgumentException(
                                block + "host must be a host name, an IP address or an IPv6 address in brackets");
                    }
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException(block + "port must be between 1 and 65535");
                    }
                    if (!DATABASE.matcher(database).matches()) {
                        throw new IllegalArgumentException(
                                block + "database may only hold letters, digits, - and _, at most 64 of them");
                    }
                    if (CONTROL.matcher(username).find()) {
                        throw new IllegalArgumentException(block + "username may not hold control characters");
                    }
                    if (password != null && CONTROL.matcher(password).find()) {
                        throw new IllegalArgumentException(block + "password may not hold control characters");
                    }
                }

                /**
                 * {@return the password, preferring {@code BUILDSYSTEM_<TYPE>_PASSWORD}} Lets operators keep the
                 * secret out of config.yml.
                 */
                public @Nullable String resolvedPassword() {
                    return envOrConfig("BUILDSYSTEM_" + type.name() + "_PASSWORD", password);
                }

                /**
                 * Overridden so the password never appears in a log or pasted support output.
                 */
                @Override
                public String toString() {
                    return "Server[type=%s, host=%s, port=%d, database=%s, username=%s, password=%s, tls=%s]"
                            .formatted(type, host, port, database, username, password == null ? null : "***", tls);
                }
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

        public record Builder(boolean blockWorldEditNonBuilder, Material worldEditWand) {}

        public record Navigator(Material item, boolean giveItemOnJoin) {}
    }

    /**
     * @param defaultNamespace The namespace a new world's name typed without one is placed in, and the preferred one when
     *     a typed name matches several existing worlds. Always valid, and always {@code minecraft} on a server that
     *     cannot create namespaced worlds.
     */
    public record World(
            boolean lockWeather,
            String defaultNamespace,
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
