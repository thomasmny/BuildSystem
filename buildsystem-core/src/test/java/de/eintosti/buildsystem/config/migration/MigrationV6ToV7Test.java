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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MigrationV6ToV7Test {

    private static final String BASE = "storage.database";

    @Test
    void migrate_addsTheDatabaseSectionAsShipped() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.sftp.host", "sftp.example.com");

        new MigrationV6ToV7().migrate(config);

        YamlConfiguration shipped;
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            shipped = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        ConfigurationSection migrated = YamlConfiguration.loadConfiguration(new StringReader(config.saveToString()))
                .getConfigurationSection(BASE);
        assertEquals(shipped.getConfigurationSection(BASE).getValues(false), migrated.getValues(false));
        for (String key :
                List.of("", ".type", ".host", ".port", ".database", ".username", ".password", ".table-prefix")) {
            assertEquals(shipped.getComments(BASE + key), config.getComments(BASE + key), BASE + key);
        }
        assertEquals("sftp.example.com", config.getString("storage.sftp.host"));
    }

    @Test
    void migrate_keepsADatabaseSectionThatIsAlreadyThere() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(BASE + ".type", "sqlite");

        new MigrationV6ToV7().migrate(config);

        assertEquals(
                Map.of("type", "sqlite"), config.getConfigurationSection(BASE).getValues(false));
    }
}
