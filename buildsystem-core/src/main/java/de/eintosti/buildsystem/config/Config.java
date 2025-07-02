/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.config.Config.Settings.Archive;
import de.eintosti.buildsystem.config.Config.Settings.BuildMode;
import de.eintosti.buildsystem.config.Config.Settings.Builder;
import de.eintosti.buildsystem.config.Config.Settings.Navigator;
import de.eintosti.buildsystem.config.Config.Settings.SaveFromDeath;
import de.eintosti.buildsystem.config.Config.World.Backup;
import de.eintosti.buildsystem.config.Config.World.Backup.AutoBackup;
import de.eintosti.buildsystem.config.Config.World.Default;
import de.eintosti.buildsystem.config.Config.World.Default.Permission;
import de.eintosti.buildsystem.config.Config.World.Default.Settings.BuildersEnabled;
import de.eintosti.buildsystem.config.Config.World.Default.Time;
import de.eintosti.buildsystem.config.Config.World.Limits;
import de.eintosti.buildsystem.config.Config.World.Unload;
import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import de.eintosti.buildsystem.world.backup.storage.LocalBackupStorage;
import de.eintosti.buildsystem.world.backup.storage.S3BackupStorage;
import de.eintosti.buildsystem.world.backup.storage.SftpBackupStorage;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Manages the plugin's configuration, loading and providing access to various settings.
 */
@NullMarked
public class Config {

    private static final BuildSystemPlugin PLUGIN = JavaPlugin.getPlugin(BuildSystemPlugin.class);
    private static final FileConfiguration CONFIG = PLUGIN.getConfig();

    /**
     * Gets the plugin's configuration.
     *
     * @return The plugin's configuration
     */
    public static FileConfiguration getConfig() {
        return CONFIG;
    }

    /**
     * Gets the current version of the plugin's configuration.
     *
     * @return The version number of the configuration
     * @see ConfigMigrationManager#LATEST_VERSION
     */
    public static int getVersion() {
        return CONFIG.getInt("version", 1);
    }

    /**
     * Sets the version of the plugin's configuration.
     *
     * @param version The version number to set
     */
    public static void setVersion(int version) {
        CONFIG.set("version", version);
        CONFIG.setComments("version", List.of("Internal, do not change manually!"));
        PLUGIN.saveConfig();
    }

    /**
     * Stores messages related configurations.
     */
    public static class Messages {

        /**
         * Whether a message should be sent to the player when teleporting to a world's spawn.
         */
        public static boolean spawnTeleportMessage = false;
        /**
         * Whether join and quit messages should be enabled.
         */
        public static boolean joinQuitMessages = true;
        /**
         * The date format used for various date displays.
         */
        public static String dateFormat = "dd/MM/yyyy";
    }

    /**
     * Stores general settings for the plugin.
     */
    public static class Settings {

        /**
         * Whether the update checker should be enabled.
         */
        public static boolean updateChecker = true;
        /**
         * Whether the scoreboard should be enabled.
         */
        public static boolean scoreboard = true;

        /**
         * Stores archive-related settings.
         */
        public static class Archive {

            /**
             * Whether players should be vanished when entering an archived world.
             */
            public static boolean vanish = true;
            /**
             * Whether the gamemode should be changed when entering an archived world.
             */
            public static boolean changeGamemode = true;
            /**
             * The gamemode players are set to when entering an archived world.
             */
            public static GameMode worldGameMode = GameMode.ADVENTURE;
        }

        /**
         * Stores settings related to saving players from death.
         */
        public static class SaveFromDeath {

            /**
             * Whether saving players from death should be enabled.
             */
            public static boolean enabled = true;
            /**
             * Whether players should be teleported to the map spawn after being saved from death.
             */
            public static boolean teleportToMapSpawn = true;
        }

        /**
         * Stores build mode related settings.
         */
        public static class BuildMode {

            /**
             * Whether items should be dropped when a player dies in build mode.
             */
            public static boolean dropItems = true;
            /**
             * Whether players can move items in their inventory in build mode.
             */
            public static boolean moveItems = true;
        }

        /**
         * Stores builder-related settings.
         */
        public static class Builder {

            /**
             * Whether WorldEdit usage should be blocked for non-builders.
             */
            public static boolean blockWorldEditNonBuilder = true;
            /**
             * The {@link XMaterial} of the WorldEdit wand.
             */
            public static XMaterial worldEditWand = XMaterial.WOODEN_AXE;
        }

        /**
         * Stores navigator-related settings.
         */
        public static class Navigator {

            /**
             * The {@link XMaterial} of the navigator item.
             */
            public static XMaterial item = XMaterial.CLOCK;
            /**
             * Whether the navigator item should be given to players on join.
             */
            public static boolean giveItemOnJoin = true;
        }
    }

    /**
     * Stores world-related configurations.
     */
    public static class World {

        /**
         * Whether the weather should be locked in worlds.
         */
        public static boolean lockWeather = true;
        /**
         * A regex string of invalid characters for world names.
         */
        public static String invalidCharacters = "^\b$";
        /**
         * The delay in seconds for the /build import all command.
         */
        public static int importAllDelay = 30;
        /**
         * A blacklist of worlds that cannot be deleted. This is used to prevent deletion of important worlds.
         */
        public static Set<String> deletionBlacklist = Set.of("world", "world_nether", "worth_the_end");
        ;

        /**
         * Stores default world settings.
         */
        public static class Default {

            /**
             * The default world border size.
             */
            public static int worldBoarderSize = 6000000;
            /**
             * The default difficulty for new worlds.
             */
            public static Difficulty difficulty = Difficulty.PEACEFUL;
            /**
             * Default game rules for new worlds.
             */
            public static Map<GameRule<?>, Object> gameRules = Map.of(
                    GameRule.DO_DAYLIGHT_CYCLE, false,
                    GameRule.DO_MOB_SPAWNING, false,
                    GameRule.DO_FIRE_TICK, false
            );

            /**
             * Stores permission-related settings for default worlds.
             */
            public static class Permission {

                /**
                 * The permission required to access public worlds by default.
                 */
                public static String publicPermission = "-";
                /**
                 * The permission required to access private worlds by default. The `%world%` placeholder will be replaced with the world name.
                 */
                public static String privatePermission = "worlds.%world%";
            }

            /**
             * Stores time-related settings for default worlds.
             */
            public static class Time {

                /**
                 * The time of sunrise in ticks.
                 */
                public static int sunrise = 0;
                /**
                 * The time of noon in ticks.
                 */
                public static int noon = 6000;
                /**
                 * The time of night in ticks.
                 */
                public static int night = 18000;
            }

            /**
             * Stores various settings for default worlds.
             */
            public static class Settings {

                /**
                 * Whether physics should be enabled by default.
                 */
                public static boolean physics = true;
                /**
                 * Whether explosions should be enabled by default.
                 */
                public static boolean explosions = true;
                /**
                 * Whether mob AI should be enabled by default.
                 */
                public static boolean mobAi = true;
                /**
                 * Whether block breaking should be enabled by default.
                 */
                public static boolean blockBreaking = true;
                /**
                 * Whether block placement should be enabled by default.
                 */
                public static boolean blockPlacement = true;
                /**
                 * Whether block interactions should be enabled by default.
                 */
                public static boolean blockInteractions = true;

                /**
                 * Stores settings for builders being enabled by default.
                 */
                public static class BuildersEnabled {

                    /**
                     * Whether builders are enabled by default for public worlds.
                     */
                    public static boolean publicBuilders = false;
                    /**
                     * Whether builders are enabled by default for private worlds.
                     */
                    public static boolean privateBuilders = true;
                }
            }
        }

        /**
         * Stores world limit settings.
         */
        public static class Limits {

            /**
             * The maximum number of public worlds a player can have. -1 for unlimited.
             */
            public static int publicWorlds = -1;
            /**
             * The maximum number of private worlds a player can have. -1 for unlimited.
             */
            public static int privateWorlds = -1;
        }

        /**
         * Stores world unload settings.
         */
        public static class Unload {

            /**
             * Whether world unloading is enabled.
             */
            public static boolean enabled = true;
            /**
             * The time until a world is unloaded after all players have left (HH:mm:ss).
             */
            public static String timeUntilUnload = "01:00:00";
            /**
             * A set of worlds that should not be unloaded.
             */
            public static Set<String> blacklistedWorlds = Set.of("world", "world_nether", "worth_the_end");
        }

        /**
         * Stores backup-related settings.
         */
        public static class Backup {

            /**
             * The maximum number of backups to keep per world.
             */
            public static int maxBackupsPerWorld = 5;
            /**
             * The storage for backups.
             */
            public static BackupStorage storage = createBackupStorage();

            public static class AutoBackup {

                /**
                 * Whether automatic backups are enabled.
                 */
                public static boolean enabled = true;
                /**
                 * Whether only active worlds should be backed up.
                 */
                public static boolean onlyActiveWorlds;
                /**
                 * The interval in seconds between automatic backups.
                 */
                public static int interval = 900;
            }
        }
    }

    /**
     * Stores folder-related settings.
     */
    public static class Folder {

        /**
         * Whether permissions should be overridden for folders.
         */
        public static boolean overridePermissions = true;
        /**
         * Whether projects should be overridden for folders.
         */
        public static boolean overrideProjects = false;
    }

    /**
     * Loads the configuration values from the plugin's config.yml into the static fields.
     */
    public static void load() {
        // Messages
        Messages.spawnTeleportMessage = CONFIG.getBoolean("messages.spawn-teleport-message", false);
        Messages.joinQuitMessages = CONFIG.getBoolean("messages.join-quit-messages", true);
        Messages.dateFormat = CONFIG.getString("messages.date-format", "dd/MM/yyyy");

        // Settings
        Settings.updateChecker = CONFIG.getBoolean("settings.update-checker", true);
        Settings.scoreboard = CONFIG.getBoolean("settings.scoreboard", true);
        // Settings - Archive
        Archive.vanish = CONFIG.getBoolean("settings.archive.vanish", true);
        Archive.changeGamemode = CONFIG.getBoolean("settings.archive.change-gamemode", true);
        Archive.worldGameMode = parseGameMode(CONFIG.getString("settings.archive.world-gamemode"));
        // Settings - Save from death
        SaveFromDeath.enabled = CONFIG.getBoolean("settings.save-from-death.enable", true);
        SaveFromDeath.teleportToMapSpawn = CONFIG.getBoolean("settings.save-from-death.teleport-to-map-spawn", true);
        // Settings - Build mode
        BuildMode.dropItems = CONFIG.getBoolean("settings.build-mode.drop-items", true);
        BuildMode.moveItems = CONFIG.getBoolean("settings.build-mode.move-items", true);
        // Settings - Builder
        Builder.blockWorldEditNonBuilder = CONFIG.getBoolean("settings.builder.block-worldedit-non-builder", true);
        Builder.worldEditWand = parseWorldEditWand();
        // Settings - Navigator
        Navigator.item = XMaterial.valueOf(CONFIG.getString("settings.navigator.item", "CLOCK"));
        Navigator.giveItemOnJoin = CONFIG.getBoolean("settings.navigator.give-item-on-join", true);

        // World
        World.lockWeather = CONFIG.getBoolean("world.lock-weather", true);
        World.invalidCharacters = CONFIG.getString("world.invalid-characters", "^\b$");
        World.importAllDelay = CONFIG.getInt("world.import-all.delay", 30);
        World.deletionBlacklist = CONFIG.getStringList("world.deletion-blacklist").stream().map(String::toLowerCase).collect(Collectors.toSet());
        // World - Default
        Default.worldBoarderSize = CONFIG.getInt("world.default.worldborder.size", 6000000);
        Default.difficulty = Difficulty.valueOf(CONFIG.getString("world.default.difficulty", "PEACEFUL").toUpperCase(Locale.ROOT));
        Default.gameRules = CONFIG.getConfigurationSection("world.default.gamerules")
                .getValues(true)
                .entrySet()
                .stream()
                .map(entry -> {
                    GameRule<?> rule = GameRule.getByName(entry.getKey());
                    return rule != null ? Map.entry(rule, entry.getValue()) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        // World - Default - Permission
        Permission.publicPermission = CONFIG.getString("world.default.permission.public", "-");
        Permission.privatePermission = CONFIG.getString("world.default.permission.private", "-");
        // World - Default - Time
        Time.sunrise = CONFIG.getInt("world.default.time.sunrise", 0);
        Time.noon = CONFIG.getInt("world.default.time.noon", 6000);
        Time.night = CONFIG.getInt("world.default.time.night", 18000);
        // World - Default - Settings
        Default.Settings.physics = CONFIG.getBoolean("world.default.settings.physics", true);
        Default.Settings.explosions = CONFIG.getBoolean("world.default.settings.explosions", true);
        Default.Settings.mobAi = CONFIG.getBoolean("world.default.settings.mob-ai", true);
        Default.Settings.blockBreaking = CONFIG.getBoolean("world.default.settings.block-breaking", true);
        Default.Settings.blockPlacement = CONFIG.getBoolean("world.default.settings.block-placement", true);
        Default.Settings.blockInteractions = CONFIG.getBoolean("world.default.settings.block-interactions", true);
        // World - Default - Settings - Builders Enabled
        BuildersEnabled.publicBuilders = CONFIG.getBoolean("world.default.settings.builders-enabled.public", false);
        BuildersEnabled.privateBuilders = CONFIG.getBoolean("world.default.settings.builders-enabled.private", true);
        // World - Limits
        Limits.publicWorlds = CONFIG.getInt("world.default.settings.public-worlds", -1);
        Limits.privateWorlds = CONFIG.getInt("world.default.settings.private-worlds", -1);
        // World - Unload
        Unload.enabled = CONFIG.getBoolean("world.unload.enabled", false);
        Unload.timeUntilUnload = CONFIG.getString("world.unload.time-until-unload", "01:00:00");
        Unload.blacklistedWorlds = new HashSet<>(CONFIG.getStringList("world.unload.blacklisted-worlds"));
        // World - Backup
        Backup.maxBackupsPerWorld = Math.min(CONFIG.getInt("world.backup.max-backups-per-world", 5), 18);
        Backup.storage = createBackupStorage();
        // World - Backup - AutoBackup
        AutoBackup.enabled = CONFIG.getBoolean("world.backup.auto-backup.enabled", true);
        AutoBackup.interval = CONFIG.getInt("world.backup.auto-backup.interval", 900);
        AutoBackup.onlyActiveWorlds = CONFIG.getBoolean("world.backup.auto-backup.only-active-worlds", true);

        // Folder
        Folder.overridePermissions = CONFIG.getBoolean("folder.override-permissions", true);
        Folder.overrideProjects = CONFIG.getBoolean("folder.override-projects", false);
    }

    /**
     * Parsing the {@link GameMode} from a string. Defaulting to {@link GameMode#ADVENTURE} if the string is not a valid {@link GameMode}.
     *
     * @param gameModeName The name of the {@link GameMode} to parse.
     * @return The parsed {@link GameMode}.
     */
    private static GameMode parseGameMode(@Nullable String gameModeName) {
        if (gameModeName == null) {
            return GameMode.ADVENTURE;
        }

        return Arrays.stream(GameMode.values())
                .filter(gameMode -> gameMode.name().equalsIgnoreCase(gameModeName))
                .findAny()
                .orElse(GameMode.ADVENTURE);
    }

    /**
     * Parses the WorldEdit wand from the plugin's configuration.
     *
     * @return The parsed {@link XMaterial} for the WorldEdit wand.
     */
    private static XMaterial parseWorldEditWand() {
        File pluginDir = PLUGIN.getDataFolder().getParentFile();
        File configFile = null;

        File weConfig = new File(pluginDir + File.separator + "WorldEdit", "config.yml");
        if (weConfig.exists()) {
            configFile = weConfig;
        }

        File faweConfig = new File(pluginDir + File.separator + "FastAsyncWorldEdit", "worldedit-config.yml");
        if (faweConfig.exists()) {
            configFile = faweConfig;
        }

        XMaterial defaultWand = XMaterial.WOODEN_AXE;
        if (configFile == null) {
            return defaultWand;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String wand = config.getString("wand-item");
        if (wand == null) {
            return defaultWand;
        }

        String namespace = "minecraft:";
        if (wand.toLowerCase(Locale.ROOT).startsWith(namespace)) {
            wand = wand.substring(namespace.length());
        }

        return XMaterial.matchXMaterial(wand).orElse(defaultWand);
    }

    private static BackupStorage createBackupStorage() {
        String type = CONFIG.getString("world.backup.storage.type", "local").toLowerCase();

        switch (type) {
            case "s3" -> {
                ConfigurationSection s3 = CONFIG.getConfigurationSection("world.backup.storage.s3");
                return new S3BackupStorage(
                        PLUGIN,
                        s3.getString("url"),
                        s3.getString("access-key"),
                        s3.getString("secret-key"),
                        s3.getString("region"),
                        s3.getString("bucket"),
                        s3.getString("path")
                );
            }
            case "sftp" -> {
                ConfigurationSection sftp = CONFIG.getConfigurationSection("world.backup.storage.sftp");
                return new SftpBackupStorage(
                        PLUGIN,
                        sftp.getString("host"),
                        sftp.getInt("port"),
                        sftp.getString("username"),
                        sftp.getString("password"),
                        sftp.getString("path")
                );
            }
            default -> {
                if (!type.equals("local")) {
                    PLUGIN.getLogger().warning("Unknown backup storage type '" + type + "', defaulting to local storage.");
                }
                return new LocalBackupStorage(PLUGIN);
            }
        }
    }
}
