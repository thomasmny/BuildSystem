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
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigTest {

    private static final Logger LOGGER = Logger.getLogger("PluginConfigTest");

    private PluginConfig parse(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ConfigService.parseForTest(config, LOGGER);
    }

    // -----------------------------------------------------------------------
    // 1. Defaults: missing keys produce documented defaults
    // -----------------------------------------------------------------------

    @Test
    void defaults_emptyConfig_producesDocumentedDefaults() {
        PluginConfig cfg = parse("");
        // Messages
        assertFalse(cfg.messages().spawnTeleportMessage());
        assertTrue(cfg.messages().joinQuitMessages());
        assertEquals("dd/MM/yyyy", cfg.messages().dateFormat());
        // Settings
        assertTrue(cfg.settings().updateChecker());
        assertTrue(cfg.settings().scoreboard());
        // World
        assertTrue(cfg.world().lockWeather());
        assertEquals("^\b$", cfg.world().invalidCharacters());
        assertEquals(30, cfg.world().importAllDelay());
        // World - Default
        assertEquals(6000000, cfg.world().defaults().worldBorderSize());
        assertEquals(0, cfg.world().defaults().time().sunrise());
        assertEquals(6000, cfg.world().defaults().time().noon());
        assertEquals(18000, cfg.world().defaults().time().night());
        // World - Limits
        assertEquals(-1, cfg.world().limits().publicWorlds());
        assertEquals(-1, cfg.world().limits().privateWorlds());
        // World - Unload
        assertFalse(cfg.world().unload().enabled());
        assertEquals("01:00:00", cfg.world().unload().timeUntilUnload());
        // World - Backup
        assertEquals(5, cfg.world().backup().maxBackupsPerWorld());
        assertInstanceOf(PluginConfig.World.Backup.Local.class, cfg.world().backup().storage());
        assertTrue(cfg.world().backup().autoBackup().enabled());
        assertEquals(900, cfg.world().backup().autoBackup().interval());
        assertTrue(cfg.world().backup().autoBackup().onlyActiveWorlds());
        // Folder
        assertTrue(cfg.folder().overridePermissions());
        assertFalse(cfg.folder().overrideProjects());
    }

    // -----------------------------------------------------------------------
    // 2. Full parse: representative config produces expected record values
    // -----------------------------------------------------------------------

    @Test
    void fullParse_representativeSnippet_producesExpectedValues() {
        PluginConfig cfg = parse("""
                messages:
                  spawn-teleport-message: true
                  join-quit-messages: false
                  date-format: "MM/dd/yyyy"
                settings:
                  update-checker: false
                  scoreboard: false
                  archive:
                    vanish: false
                    change-gamemode: false
                    world-gamemode: "CREATIVE"
                  save-from-death:
                    enabled: false
                    teleport-to-map-spawn: false
                  build-mode:
                    drop-items: false
                    move-items: false
                  builder:
                    block-worldedit-non-builder: false
                  navigator:
                    item: "COMPASS"
                    give-item-on-join: false
                world:
                  lock-weather: false
                  invalid-characters: "[!]"
                  import-all:
                    delay: 60
                  deletion-blacklist:
                    - world
                  unload:
                    enabled: true
                    time-until-unload: "00:30:00"
                    blacklisted-worlds:
                      - world
                  default:
                    worldborder:
                      size: 1000
                    difficulty: "HARD"
                    time:
                      sunrise: 100
                      noon: 6100
                      night: 18100
                    gamerules: {}
                    settings:
                      physics: false
                      explosions: false
                      mob-ai: false
                      block-breaking: false
                      block-placement: false
                      block-interactions: false
                      builders-enabled:
                        public: true
                        private: false
                  backup:
                    max-backups-per-world: 10
                    auto-backup:
                      enabled: false
                      interval: 1800
                      only-active-worlds: false
                    storage:
                      type: local
                folder:
                  override-permissions: false
                  override-projects: true
                """);

        assertTrue(cfg.messages().spawnTeleportMessage());
        assertFalse(cfg.messages().joinQuitMessages());
        assertEquals("MM/dd/yyyy", cfg.messages().dateFormat());
        assertFalse(cfg.settings().updateChecker());
        assertFalse(cfg.settings().scoreboard());
        assertFalse(cfg.settings().archive().vanish());
        assertFalse(cfg.settings().archive().changeGamemode());
        assertEquals(org.bukkit.GameMode.CREATIVE, cfg.settings().archive().worldGameMode());
        assertFalse(cfg.settings().saveFromDeath().enabled());
        assertFalse(cfg.settings().saveFromDeath().teleportToMapSpawn());
        assertFalse(cfg.settings().buildMode().dropItems());
        assertFalse(cfg.settings().buildMode().moveItems());
        assertFalse(cfg.settings().builder().blockWorldEditNonBuilder());
        assertEquals(XMaterial.COMPASS, cfg.settings().navigator().item());
        assertFalse(cfg.settings().navigator().giveItemOnJoin());
        assertFalse(cfg.world().lockWeather());
        assertEquals("[!]", cfg.world().invalidCharacters());
        assertEquals(60, cfg.world().importAllDelay());
        assertTrue(cfg.world().deletionBlacklist().contains("world"));
        assertEquals(1, cfg.world().deletionBlacklist().size());
        assertTrue(cfg.world().unload().enabled());
        assertEquals("00:30:00", cfg.world().unload().timeUntilUnload());
        assertEquals(1000, cfg.world().defaults().worldBorderSize());
        assertEquals(org.bukkit.Difficulty.HARD, cfg.world().defaults().difficulty());
        assertEquals(100, cfg.world().defaults().time().sunrise());
        assertEquals(6100, cfg.world().defaults().time().noon());
        assertEquals(18100, cfg.world().defaults().time().night());
        assertFalse(cfg.world().defaults().settings().physics());
        assertFalse(cfg.world().defaults().settings().explosions());
        assertFalse(cfg.world().defaults().settings().mobAi());
        assertFalse(cfg.world().defaults().settings().blockBreaking());
        assertFalse(cfg.world().defaults().settings().blockPlacement());
        assertFalse(cfg.world().defaults().settings().blockInteractions());
        assertTrue(cfg.world().defaults().settings().buildersEnabled().publicBuilders());
        assertFalse(cfg.world().defaults().settings().buildersEnabled().privateBuilders());
        // backup is capped at 18
        assertEquals(10, cfg.world().backup().maxBackupsPerWorld());
        assertFalse(cfg.world().backup().autoBackup().enabled());
        assertEquals(1800, cfg.world().backup().autoBackup().interval());
        assertFalse(cfg.world().backup().autoBackup().onlyActiveWorlds());
        assertFalse(cfg.folder().overridePermissions());
        assertTrue(cfg.folder().overrideProjects());
    }

    // -----------------------------------------------------------------------
    // 3. Backup storage: s3 type
    // -----------------------------------------------------------------------

    @Test
    void backupStorage_s3Type_returnsS3Record() {
        PluginConfig cfg = parse("""
                world:
                  default:
                    gamerules: {}
                  backup:
                    max-backups-per-world: 5
                    auto-backup:
                      enabled: true
                      interval: 900
                      only-active-worlds: true
                    storage:
                      type: s3
                      s3:
                        url: "https://example.com"
                        access-key: "MYACCESSKEY"
                        secret-key: "MYSECRETKEY"
                        region: "eu-central-1"
                        bucket: "my-bucket"
                        path: "backups/"
                """);

        assertInstanceOf(PluginConfig.World.Backup.S3.class, cfg.world().backup().storage());
        PluginConfig.World.Backup.S3 s3 = (PluginConfig.World.Backup.S3) cfg.world().backup().storage();
        assertEquals("https://example.com", s3.url());
        assertEquals("MYACCESSKEY", s3.accessKey());
        assertEquals("MYSECRETKEY", s3.secretKey());
        assertEquals("eu-central-1", s3.region());
        assertEquals("my-bucket", s3.bucket());
        assertEquals("backups/", s3.path());
    }

    // -----------------------------------------------------------------------
    // 4. Backup storage: sftp type
    // -----------------------------------------------------------------------

    @Test
    void backupStorage_sftpType_returnsSftpRecord() {
        PluginConfig cfg = parse("""
                world:
                  default:
                    gamerules: {}
                  backup:
                    max-backups-per-world: 5
                    auto-backup:
                      enabled: true
                      interval: 900
                      only-active-worlds: true
                    storage:
                      type: sftp
                      sftp:
                        host: "sftp.example.com"
                        port: 22
                        username: "user"
                        password: "pass"
                        path: "/backups/"
                """);

        assertInstanceOf(PluginConfig.World.Backup.Sftp.class, cfg.world().backup().storage());
        PluginConfig.World.Backup.Sftp sftp = (PluginConfig.World.Backup.Sftp) cfg.world().backup().storage();
        assertEquals("sftp.example.com", sftp.host());
        assertEquals(22, sftp.port());
        assertEquals("user", sftp.username());
        assertEquals("pass", sftp.password());
        assertEquals("/backups/", sftp.path());
    }

    // -----------------------------------------------------------------------
    // 5. Backup storage: unknown type defaults to Local
    // -----------------------------------------------------------------------

    @Test
    void backupStorage_unknownType_defaultsToLocal() {
        PluginConfig cfg = parse("""
                world:
                  default:
                    gamerules: {}
                  backup:
                    max-backups-per-world: 5
                    auto-backup:
                      enabled: true
                      interval: 900
                      only-active-worlds: true
                    storage:
                      type: unknown_type
                """);

        assertInstanceOf(PluginConfig.World.Backup.Local.class, cfg.world().backup().storage());
    }

    // -----------------------------------------------------------------------
    // 6. GameRule parsing
    // -----------------------------------------------------------------------

    // NOTE: Full GameRule parsing tests (e.g. verifying advance_time or random_tick_speed are
    // resolved and stored) require a running Bukkit server for registry access, and therefore
    // cannot be exercised in plain unit tests. The cases below cover what is safe to check.

    @Test
    void gameRules_emptySection_producesEmptyList() {
        PluginConfig cfg = parse("""
                world:
                  default:
                    gamerules: {}
                  backup:
                    max-backups-per-world: 5
                    auto-backup:
                      enabled: true
                      interval: 900
                      only-active-worlds: true
                    storage:
                      type: local
                """);

        var rules = cfg.world().defaults().gameRules();
        assertEquals(0, rules.size());
    }

    @Test
    void gameRules_missingSectionAltogether_producesEmptyList() {
        PluginConfig cfg = parse("""
                world:
                  backup:
                    max-backups-per-world: 5
                    auto-backup:
                      enabled: true
                      interval: 900
                      only-active-worlds: true
                    storage:
                      type: local
                """);

        // No gamerules section at all — should default to empty list without error
        var rules = cfg.world().defaults().gameRules();
        assertNotNull(rules);
        assertEquals(0, rules.size());
    }

    // -----------------------------------------------------------------------
    // 7. Deletion blacklist is immutable
    // -----------------------------------------------------------------------

    @Test
    void deletionBlacklist_isImmutable() {
        PluginConfig cfg = parse("""
                world:
                  deletion-blacklist:
                    - world
                    - world_nether
                  default:
                    gamerules: {}
                  backup:
                    max-backups-per-world: 5
                    auto-backup:
                      enabled: true
                      interval: 900
                      only-active-worlds: true
                    storage:
                      type: local
                """);

        var blacklist = cfg.world().deletionBlacklist();
        assertThrows(UnsupportedOperationException.class, () -> blacklist.add("test_world"));
    }

    // -----------------------------------------------------------------------
    // 8. maxBackupsPerWorld is capped at 18
    // -----------------------------------------------------------------------

    @Test
    void maxBackupsPerWorld_valueAbove18_isCappedAt18() {
        PluginConfig cfg = parse("""
                world:
                  default:
                    gamerules: {}
                  backup:
                    max-backups-per-world: 100
                    auto-backup:
                      enabled: true
                      interval: 900
                      only-active-worlds: true
                    storage:
                      type: local
                """);

        assertEquals(18, cfg.world().backup().maxBackupsPerWorld());
    }
}
