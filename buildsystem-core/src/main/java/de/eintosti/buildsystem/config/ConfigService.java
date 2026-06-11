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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.world.modification.GameRuleEntry;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ConfigService {

    private final BuildSystemPlugin plugin;
    private volatile PluginConfig current;

    public ConfigService(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        // Initialize with a placeholder; load() must be called before current() is used.
        this.current = parse(plugin.getConfig(), plugin.getLogger());
    }

    /**
     * Returns the currently loaded {@link PluginConfig}.
     *
     * @return The current plugin configuration
     */
    public PluginConfig current() {
        return current;
    }

    /**
     * Reloads the configuration from disk and re-parses it into a new {@link PluginConfig}.
     */
    public void load() {
        this.current = parse(plugin.getConfig(), plugin.getLogger());
    }

    /**
     * Gets the current version of the plugin's configuration.
     *
     * @return The version number of the configuration
     */
    public int getVersion() {
        return plugin.getConfig().getInt("version", 1);
    }

    /**
     * Sets the version of the plugin's configuration and saves the config file.
     *
     * @param version The version number to set
     */
    public void setVersion(int version) {
        plugin.getConfig().set("version", version);
        plugin.getConfig().setComments("version", List.of("Internal, do not change manually!"));
        plugin.saveConfig();
    }

    /**
     * Gets the raw {@link FileConfiguration} for use by migration code.
     *
     * @return The raw file configuration
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Saves the plugin config to disk.
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Parses the given {@link FileConfiguration} into a {@link PluginConfig} record tree.
     *
     * @param config The file configuration to parse
     * @param logger The logger to use for warnings
     * @return The parsed {@link PluginConfig}
     */
    PluginConfig parse(FileConfiguration config, Logger logger) {
        return parse(config, logger, plugin.getDataFolder().getParentFile());
    }

    /**
     * Parses a config for testing purposes (WorldEdit wand detection skipped — returns default {@link XMaterial#WOODEN_AXE}).
     *
     * @param config The file configuration to parse
     * @param logger The logger to use for warnings
     * @return The parsed {@link PluginConfig}
     */
    static PluginConfig parseForTest(FileConfiguration config, Logger logger) {
        return parse(config, logger, null);
    }

    private static PluginConfig parse(FileConfiguration config, Logger logger, @Nullable File pluginParentDir) {
        // Messages
        PluginConfig.Messages messages = new PluginConfig.Messages(
                config.getBoolean("messages.spawn-teleport-message", false),
                config.getBoolean("messages.join-quit-messages", true),
                Objects.requireNonNullElse(config.getString("messages.date-format"), "dd/MM/yyyy")
        );

        // Settings - Archive
        PluginConfig.Settings.Archive archive = new PluginConfig.Settings.Archive(
                config.getBoolean("settings.archive.vanish", true),
                config.getBoolean("settings.archive.change-gamemode", true),
                parseGameMode(config.getString("settings.archive.world-gamemode"))
        );

        // Settings - DisabledPhysics
        PluginConfig.Settings.DisabledPhysics disabledPhysics = new PluginConfig.Settings.DisabledPhysics(
                config.getBoolean("settings.disabled-physics.prevent-connections", true),
                config.getBoolean("settings.disabled-physics.prevent-fluid-flow", true),
                config.getBoolean("settings.disabled-physics.prevent-falling-blocks", true)
        );

        // Settings - SaveFromDeath
        PluginConfig.Settings.SaveFromDeath saveFromDeath = new PluginConfig.Settings.SaveFromDeath(
                config.getBoolean("settings.save-from-death.enabled", true),
                config.getBoolean("settings.save-from-death.teleport-to-map-spawn", true)
        );

        // Settings - BuildMode
        PluginConfig.Settings.BuildMode buildMode = new PluginConfig.Settings.BuildMode(
                config.getBoolean("settings.build-mode.drop-items", true),
                config.getBoolean("settings.build-mode.move-items", true)
        );

        // Settings - Builder
        PluginConfig.Settings.Builder builder = new PluginConfig.Settings.Builder(
                config.getBoolean("settings.builder.block-worldedit-non-builder", true),
                parseWorldEditWand(pluginParentDir)
        );

        // Settings - Navigator
        PluginConfig.Settings.Navigator navigator = new PluginConfig.Settings.Navigator(
                XMaterial.valueOf(Objects.requireNonNullElse(config.getString("settings.navigator.item"), "CLOCK")),
                config.getBoolean("settings.navigator.give-item-on-join", true)
        );

        PluginConfig.Settings settings = new PluginConfig.Settings(
                config.getBoolean("settings.update-checker", true),
                config.getBoolean("settings.scoreboard", true),
                archive,
                disabledPhysics,
                saveFromDeath,
                buildMode,
                builder,
                navigator
        );

        // World - Default - Permission
        PluginConfig.World.Default.Permission permission = new PluginConfig.World.Default.Permission(
                Objects.requireNonNullElse(config.getString("world.default.permission.public"), "-"),
                Objects.requireNonNullElse(config.getString("world.default.permission.private"), "worlds.%world%")
        );

        // World - Default - Time
        PluginConfig.World.Default.Time time = new PluginConfig.World.Default.Time(
                config.getInt("world.default.time.sunrise", 0),
                config.getInt("world.default.time.noon", 6000),
                config.getInt("world.default.time.night", 18000)
        );

        // World - Default - Settings - BuildersEnabled
        PluginConfig.World.Default.DefaultSettings.BuildersEnabled buildersEnabled = new PluginConfig.World.Default.DefaultSettings.BuildersEnabled(
                config.getBoolean("world.default.settings.builders-enabled.public", false),
                config.getBoolean("world.default.settings.builders-enabled.private", true)
        );

        // World - Default - Settings
        PluginConfig.World.Default.DefaultSettings defaultSettings = new PluginConfig.World.Default.DefaultSettings(
                config.getBoolean("world.default.settings.physics", true),
                config.getBoolean("world.default.settings.explosions", true),
                config.getBoolean("world.default.settings.mob-ai", true),
                config.getBoolean("world.default.settings.block-breaking", true),
                config.getBoolean("world.default.settings.block-placement", true),
                config.getBoolean("world.default.settings.block-interactions", true),
                buildersEnabled
        );

        // World - Default - GameRules
        var gameRulesSection = config.getConfigurationSection("world.default.gamerules");
        Map<String, Object> gameRulesMap = gameRulesSection == null ? Map.of() : gameRulesSection.getValues(true);
        List<GameRuleEntry<?>> gameRules = gameRulesMap.entrySet()
                .stream()
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    GameRule<?> rule = GameRule.getByName(key);
                    if (rule == null || value == null) {
                        logger.warning("Could not parse game rule '%s' with value '%s'".formatted(key, value));
                        return null;
                    }

                    return switch (value) {
                        case Boolean booleanValue -> {
                            if (rule.getType() != Boolean.class) {
                                logger.warning("Game rule '%s' is not a boolean type, but a boolean value was provided".formatted(key));
                                yield null;
                            }
                            yield (GameRuleEntry<?>) new GameRuleEntry<>((GameRule<Boolean>) rule, booleanValue);
                        }
                        case Integer integerValue -> {
                            if (rule.getType() != Integer.class) {
                                logger.warning("Game rule '%s' is not an integer type, but an integer value was provided".formatted(key));
                                yield null;
                            }
                            yield (GameRuleEntry<?>) new GameRuleEntry<>((GameRule<Integer>) rule, integerValue);
                        }
                        default -> {
                            logger.warning("Invalid game rule value type. Must be of type Boolean or Integer. Found %s".formatted(value.getClass().getName()));
                            yield null;
                        }
                    };
                })
                .filter(Objects::nonNull)
                .toList();

        // World - Default
        PluginConfig.World.Default defaults = new PluginConfig.World.Default(
                config.getInt("world.default.worldborder.size", 6000000),
                Difficulty.valueOf(Objects.requireNonNullElse(config.getString("world.default.difficulty"), "PEACEFUL").toUpperCase(Locale.ROOT)),
                gameRules,
                permission,
                time,
                defaultSettings
        );

        // World - Limits
        PluginConfig.World.Limits limits = new PluginConfig.World.Limits(
                config.getInt("world.default.settings.public-worlds", -1),
                config.getInt("world.default.settings.private-worlds", -1)
        );

        // World - Unload
        PluginConfig.World.Unload unload = new PluginConfig.World.Unload(
                config.getBoolean("world.unload.enabled", false),
                Objects.requireNonNullElse(config.getString("world.unload.time-until-unload"), "01:00:00"),
                new HashSet<>(config.getStringList("world.unload.blacklisted-worlds"))
        );

        // World - Backup - StorageSettings
        PluginConfig.World.Backup.StorageSettings storageSettings = parseStorageSettings(config, logger);

        // World - Backup - AutoBackup
        PluginConfig.World.Backup.AutoBackup autoBackup = new PluginConfig.World.Backup.AutoBackup(
                config.getBoolean("world.backup.auto-backup.enabled", true),
                config.getBoolean("world.backup.auto-backup.only-active-worlds", true),
                config.getInt("world.backup.auto-backup.interval", 900)
        );

        // World - Backup
        PluginConfig.World.Backup backup = new PluginConfig.World.Backup(
                Math.min(config.getInt("world.backup.max-backups-per-world", 5), 18),
                storageSettings,
                autoBackup
        );

        // World
        Set<String> deletionBlacklist = config.getStringList("world.deletion-blacklist").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        PluginConfig.World world = new PluginConfig.World(
                config.getBoolean("world.lock-weather", true),
                Objects.requireNonNullElse(config.getString("world.invalid-characters"), "^\b$"),
                config.getInt("world.import-all.delay", 30),
                deletionBlacklist,
                defaults,
                limits,
                unload,
                backup
        );

        // Folder
        PluginConfig.Folder folder = new PluginConfig.Folder(
                config.getBoolean("folder.override-permissions", true),
                config.getBoolean("folder.override-projects", false)
        );

        return new PluginConfig(messages, settings, world, folder);
    }

    private static PluginConfig.World.Backup.StorageSettings parseStorageSettings(FileConfiguration config, Logger logger) {
        String type = Objects.requireNonNullElse(config.getString("world.backup.storage.type"), "local").toLowerCase();

        return switch (type) {
            case "s3" -> new PluginConfig.World.Backup.S3(
                    config.getString("world.backup.storage.s3.url"),
                    config.getString("world.backup.storage.s3.access-key"),
                    config.getString("world.backup.storage.s3.secret-key"),
                    config.getString("world.backup.storage.s3.region"),
                    config.getString("world.backup.storage.s3.bucket"),
                    config.getString("world.backup.storage.s3.path")
            );
            case "sftp" -> new PluginConfig.World.Backup.Sftp(
                    config.getString("world.backup.storage.sftp.host"),
                    config.getInt("world.backup.storage.sftp.port", 22),
                    config.getString("world.backup.storage.sftp.username"),
                    config.getString("world.backup.storage.sftp.password"),
                    config.getString("world.backup.storage.sftp.path")
            );
            default -> {
                if (!type.equals("local")) {
                    logger.warning("Unknown backup storage type '" + type + "', defaulting to local storage.");
                }
                yield new PluginConfig.World.Backup.Local();
            }
        };
    }

    /**
     * Parses the WorldEdit wand material from the WorldEdit or FastAsyncWorldEdit config file.
     *
     * @param pluginDir The parent directory of all plugins, or null to skip WorldEdit detection
     * @return The parsed {@link XMaterial} for the WorldEdit wand
     */
    static XMaterial parseWorldEditWand(@Nullable File pluginDir) {
        File configFile = null;

        if (pluginDir != null) {
            File weConfig = new File(pluginDir + File.separator + "WorldEdit", "config.yml");
            if (weConfig.exists()) {
                configFile = weConfig;
            }

            File faweConfig = new File(pluginDir + File.separator + "FastAsyncWorldEdit", "worldedit-config.yml");
            if (faweConfig.exists()) {
                configFile = faweConfig;
            }
        }

        XMaterial defaultWand = XMaterial.WOODEN_AXE;
        if (configFile == null) {
            return defaultWand;
        }

        YamlConfiguration weYaml = YamlConfiguration.loadConfiguration(configFile);
        String wand = weYaml.getString("wand-item");
        if (wand == null) {
            return defaultWand;
        }

        String namespace = "minecraft:";
        if (wand.toLowerCase(Locale.ROOT).startsWith(namespace)) {
            wand = wand.substring(namespace.length());
        }

        return XMaterial.matchXMaterial(wand).orElse(defaultWand);
    }

    /**
     * Parses the {@link GameMode} from a string, defaulting to {@link GameMode#ADVENTURE} if the string is not valid.
     *
     * @param gameModeName The name of the {@link GameMode} to parse
     * @return The parsed {@link GameMode}
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
}
