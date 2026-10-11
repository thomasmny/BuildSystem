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
package de.eintosti.buildsystem.config.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationV6ToV7Test {

    private static final List<String> KEYS =
            List.of("type", "table-prefix", "mysql", "mariadb", "postgresql", "mongodb");

    @TempDir
    File dataFolder;

    @Test
    void migrate_withoutYamlData_addsTheStorageKeysAsShipped() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.sftp.host", "sftp.example.com");

        new MigrationV6ToV7(dataFolder).migrate(config);

        YamlConfiguration shipped;
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            shipped = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(new StringReader(config.saveToString()));
        for (String key : KEYS) {
            String path = "storage." + key;
            assertEquals(values(shipped, path), values(migrated, path), path);
            assertEquals(shipped.getComments(path), migrated.getComments(path), path);
        }
        assertEquals("sqlite", migrated.getString("storage.type"));
        assertEquals("sftp.example.com", config.getString("storage.sftp.host"));
    }

    @Test
    void migrate_withWorldsOrPlayersInYaml_keepsYaml() throws Exception {
        for (String file : List.of("worlds.yml", "players.yml")) {
            File folder = new File(dataFolder, file);
            folder.mkdirs();
            Files.writeString(new File(folder, file).toPath(), "");
            YamlConfiguration config = new YamlConfiguration();

            new MigrationV6ToV7(folder).migrate(config);

            assertEquals("yaml", config.getString("storage.type"), file);
        }
    }

    @Test
    void migrate_keepsAStorageTypeThatIsAlreadyThere() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.type", "postgresql");

        new MigrationV6ToV7(dataFolder).migrate(config);

        assertEquals(
                Map.of("type", "postgresql"),
                config.getConfigurationSection("storage").getValues(false));
    }

    private static Object values(YamlConfiguration config, String path) {
        return config.get(path) instanceof ConfigurationSection section ? section.getValues(true) : config.get(path);
    }
}
