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
package de.eintosti.buildsystem.storage.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Pins the stored folder format: a file written by 4.0 loads with the same values and writes back the same keys, and
 * keys missing from older files fall back to the same defaults.
 */
@NullMarked
class FolderCodecTest {

    private final WorldContext context = TestData.worldContext();
    private final FolderCodec codec = new FolderCodec(context, TestData.categoryRegistry());

    @Test
    void writtenFolders_keepTheirKeysInDeclarationOrder() {
        FolderImpl full = CodecSamples.fullFolder(context);
        FolderImpl minimal = CodecSamples.minimalFolder(context);
        Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        entries.put(codec.key(full), codec.serialize(full));
        entries.put(codec.key(minimal), codec.serialize(minimal));

        assertEquals(CodecSamples.resource("folders-written.yml"), CodecSamples.toYaml(entries));
    }

    @Test
    void fileWrittenBy40_writesBackTheSameValues() {
        String yaml = CodecSamples.resource("folders-4.0.yml");
        for (String key : List.of(CodecSamples.FOLDER_ID.toString(), CodecSamples.MINIMAL_FOLDER_ID.toString())) {
            ConfigurationSection section = CodecSamples.section(yaml, key);
            FolderImpl folder = codec.deserialize(key, section);
            String parent = FolderCodec.parentReference(section);
            if (parent != null) {
                folder.setParent(parent(UUID.fromString(parent), folder));
            }

            assertEquals(CodecSamples.asMap(section), CodecSamples.reparsed(codec.serialize(folder)), key);
        }
    }

    @Test
    void fileWrittenBy40_loadsEveryField() {
        String key = CodecSamples.FOLDER_ID.toString();
        ConfigurationSection section = CodecSamples.section(CodecSamples.resource("folders-4.0.yml"), key);

        FolderImpl folder = codec.deserialize(key, section);

        assertEquals(CodecSamples.FOLDER_ID, folder.getUniqueId());
        assertEquals("Lobbies", folder.getName());
        assertEquals(1_700_000_000_000L, folder.getCreation());
        assertEquals(TestData.ARCHIVE, folder.getCategory());
        assertEquals(CodecSamples.ALEX.toString(), folder.getCreator().toString());
        assertEquals(Material.OAK_SIGN, folder.getIcon());
        assertEquals("skull-texture", folder.getIconSkullTexture());
        assertEquals("maps.lobby", folder.getPermission());
        assertEquals("Hub", folder.getProject());
        assertEquals(2, folder.getWorldUUIDs().size());
        assertEquals(CodecSamples.PARENT_ID.toString(), FolderCodec.parentReference(section));
    }

    @Test
    void sparseFolder_fallsBackToDefaults() {
        String key = CodecSamples.FOLDER_ID.toString();
        ConfigurationSection section = CodecSamples.section("""
                %s:
                  creator: 0c0c0c0c-0000-4000-8000-000000000001,Alex
                """.formatted(key), key);

        FolderImpl folder = codec.deserialize(key, section);

        assertEquals(key, folder.getName());
        assertEquals(TestData.categoryRegistry().getDefault(), folder.getCategory());
        assertEquals(Material.CHEST, folder.getIcon());
        assertNull(folder.getIconSkullTexture());
        assertEquals("-", folder.getPermission());
        assertEquals("-", folder.getProject());
        assertEquals(List.of(), folder.getWorldUUIDs());
        assertNull(FolderCodec.parentReference(section));
    }

    @Test
    void preV4UpperCaseCategory_resolvesToTheBuiltInCategory() {
        String key = CodecSamples.FOLDER_ID.toString();
        ConfigurationSection section = CodecSamples.section("""
                %s:
                  creator: 0c0c0c0c-0000-4000-8000-000000000001,Alex
                  category: PRIVATE
                  material: not_a_material
                """.formatted(key), key);

        FolderImpl folder = codec.deserialize(key, section);

        assertEquals(TestData.PRIVATE, folder.getCategory());
        assertEquals(Material.CHEST, folder.getIcon());
    }

    @Test
    void unresolvedCategoryAndIcon_areWrittenBackUntilSet() {
        String key = CodecSamples.FOLDER_ID.toString();
        ConfigurationSection section = CodecSamples.section("""
                %s:
                  creator: 0c0c0c0c-0000-4000-8000-000000000001,Alex
                  category: deleted_category
                  material: not_a_material
                """.formatted(key), key);

        FolderImpl folder = codec.deserialize(key, section);

        assertEquals(TestData.categoryRegistry().getDefault(), folder.getCategory());
        assertEquals("deleted_category", codec.serialize(folder).get("category"));
        assertEquals("not_a_material", codec.serialize(folder).get("material"));

        folder.setCategory(TestData.PRIVATE);
        folder.setIcon(Material.OAK_SIGN);
        assertEquals(TestData.PRIVATE.getId(), codec.serialize(folder).get("category"));
        assertEquals("OAK_SIGN", codec.serialize(folder).get("material"));
    }

    private FolderImpl parent(UUID uuid, FolderImpl child) {
        return FolderImpl.builder(context, uuid)
                .name("Parent")
                .creation(0L)
                .category(child.getCategory())
                .creator(CodecSamples.SAM)
                .build();
    }
}
