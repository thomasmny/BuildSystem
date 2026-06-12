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
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Compile-guard: ensures BuildSystemPlugin has no static self-referencing singleton. Fails if someone re-adds a static
 * BuildSystemPlugin instance field or get() method. Parses class file structure without loading the class (avoids full
 * plugin classpath).
 */
@NullMarked
class BuildSystemPluginSingletonGuardTest {

    private static final String CLASS_RESOURCE = "de/eintosti/buildsystem/BuildSystemPlugin.class";
    private static final String SELF_DESCRIPTOR = "Lde/eintosti/buildsystem/BuildSystemPlugin;";
    private static final String GET_METHOD_DESCRIPTOR = "()Lde/eintosti/buildsystem/BuildSystemPlugin;";

    private static String[] constantPool = new String[0];
    // Each entry: [accessFlags, nameIndex, descriptorIndex]
    private static int[][] fields = new int[0][];
    private static int[][] methods = new int[0][];

    @BeforeAll
    static void loadClassFile() throws Exception {
        InputStream is =
                BuildSystemPluginSingletonGuardTest.class.getClassLoader().getResourceAsStream(CLASS_RESOURCE);
        assumeTrue(is != null, "BuildSystemPlugin.class not found on classpath — skipping guard");

        try (DataInputStream dis = new DataInputStream(is)) {
            dis.readInt(); // magic 0xCAFEBABE
            dis.readShort(); // minor version
            dis.readShort(); // major version

            int cpCount = dis.readUnsignedShort();
            String[] pool = new String[cpCount];

            for (int i = 1; i < cpCount; i++) {
                int tag = dis.readUnsignedByte();
                switch (tag) {
                    case 1 -> { // CONSTANT_Utf8
                        int len = dis.readUnsignedShort();
                        byte[] bytes = new byte[len];
                        dis.readFully(bytes);
                        pool[i] = new String(bytes, StandardCharsets.UTF_8);
                    }
                    case 3, 4 -> dis.readInt();
                    case 5, 6 -> {
                        dis.readLong();
                        i++;
                    } // long/double occupy two slots
                    case 7, 8, 16, 19, 20 -> dis.readUnsignedShort();
                    case 9, 10, 11, 12 -> dis.readInt();
                    case 15 -> {
                        dis.readUnsignedByte();
                        dis.readUnsignedShort();
                    }
                    case 17, 18 -> dis.readInt();
                    default -> throw new IllegalStateException("Unknown constant pool tag: " + tag + " at index " + i);
                }
            }
            constantPool = pool;

            dis.readUnsignedShort(); // access flags
            dis.readUnsignedShort(); // this class
            dis.readUnsignedShort(); // super class
            int interfaceCount = dis.readUnsignedShort();
            for (int i = 0; i < interfaceCount; i++) dis.readUnsignedShort();

            fields = readMemberTable(dis);
            methods = readMemberTable(dis);
        }
    }

    private static int[][] readMemberTable(DataInputStream dis) throws Exception {
        int count = dis.readUnsignedShort();
        List<int[]> members = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int accessFlags = dis.readUnsignedShort();
            int nameIndex = dis.readUnsignedShort();
            int descriptorIndex = dis.readUnsignedShort();
            members.add(new int[] {accessFlags, nameIndex, descriptorIndex});
            int attrCount = dis.readUnsignedShort();
            for (int j = 0; j < attrCount; j++) {
                dis.readUnsignedShort(); // attribute name index
                int attrLen = dis.readInt();
                dis.skipBytes(attrLen);
            }
        }
        return members.toArray(new int[0][]);
    }

    @Test
    void noStaticSelfReferenceField() {
        for (int[] field : fields) {
            int accessFlags = field[0];
            if ((accessFlags & Modifier.STATIC) == 0) continue;
            String descriptor = constantPool[field[2]];
            assertNotEquals(
                    SELF_DESCRIPTOR,
                    descriptor,
                    "BuildSystemPlugin has a static field of type BuildSystemPlugin — singleton was re-added (field name: "
                            + constantPool[field[1]] + ")");
        }
    }

    @Test
    void noStaticGetMethod() {
        for (int[] method : methods) {
            int accessFlags = method[0];
            if ((accessFlags & Modifier.STATIC) == 0) continue;
            String name = constantPool[method[1]];
            String descriptor = constantPool[method[2]];
            assertFalse(
                    "get".equals(name) && GET_METHOD_DESCRIPTOR.equals(descriptor),
                    "BuildSystemPlugin has a static get() method returning itself — singleton was re-added");
        }
    }
}
