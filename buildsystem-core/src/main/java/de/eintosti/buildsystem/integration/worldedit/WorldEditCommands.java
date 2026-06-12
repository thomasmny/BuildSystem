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
package de.eintosti.buildsystem.integration.worldedit;

import com.google.common.collect.Sets;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WorldEditCommands {

    public static final Set<String> RESTRICTED = Sets.newHashSet(
            "/worldedit",
            "/we",

            // History Control
            "//undo",
            "//redo",
            "/clearhistory",

            // Region Selection
            "//wand",
            "/toggleeditwand",
            "//sel",
            "//desel",
            "//pos1",
            "//pos2",
            "//0",
            "//1",
            "//2",
            "//hpos1",
            "//hpos2",
            "//chunk",
            "//expand",
            "//contract",
            "//outset",
            "//inset",
            "//count",
            "//distr",

            // Region Operation
            "//set",
            "//replace",
            "//repl",
            "//overlay",
            "//walls",
            "//outline",
            "//center",
            "//smooth",
            "//deform",
            "//regen",
            "//hollow",
            "//move",
            "//stack",
            "//naturalize",
            "//line",
            "//curve",
            "//forest",
            "//flora",
            "//air",

            // Clipboards and Schematics
            "//copy",
            "//cut",
            "//paste",
            "//rotate",
            "//flip",
            "//schematic",
            "//schem",
            "/clearclipboard",

            // Generation
            "//generate",
            "//generatebiome",
            "//hcyl",
            "//cyl",
            "//sphere",
            "//hsphere",
            "//pyramid",
            "/forestgen",
            "/pumpkins",

            // Utilities
            "/toggleplace",
            "//fill",
            "//fillr",
            "//drain",
            "//fixwater",
            "//fixlava",
            "/removeabove",
            "/removebelow",
            "/replacenear",
            "/removenear",
            "/snow",
            "/thaw",
            "/ex",
            "/butcher",
            "/remove",
            "/green",
            "//calc",

            // Chunk Tools
            "/chunkinfo",
            "/listchunks",
            "/delchunks",

            // Superpickaxe Tools
            "//",
            "/sp single",
            "/sp area",
            "/sp recur",

            // General Tools
            "/tool",
            "/none",
            "/farwand",
            "/lrbuild",
            "/tree",
            "/deltree",
            "/repl",
            "/cycler",
            "/flood",

            // Brushes
            "//brush",
            "//br",
            "/brush",
            "/br",
            "/size",
            "/mat",
            "/range",
            "/mask",
            "//gmask",

            // Quick-Travel
            "/unstuck",
            "/ascend",
            "/asc",
            "/descend",
            "/desc",
            "/thru",
            "/jumpto",
            "/up",

            // Snapshots
            "//restore",
            "/snapshot",

            // Java Scriptings
            "//cs",
            "/.s",

            // Biomes
            "/biomelist",
            "/biomels",
            "/biomeinfo",
            "//setbiome",

            // Voxel Sniper
            "/vs",
            "/voxel",
            "/voxel_chunk",
            "/voxel_height",
            "/voxel_ink",
            "/voxel_ink_replace",
            "/voxel_list",
            "/voxel_replace",
            "/voxel_sniper",
            "/b",
            "/brush",
            "/brush_toolkit",
            "/d",
            "/default",
            "/goto",
            "/p",
            "/paint",
            "/perf",
            "/performer",
            "/v",
            "/vc",
            "/vchunk",
            "/vh",
            "/vi",
            "/vir",
            "/vl",
            "/vr--"
    );

    private WorldEditCommands() {
        throw new UnsupportedOperationException();
    }
}
