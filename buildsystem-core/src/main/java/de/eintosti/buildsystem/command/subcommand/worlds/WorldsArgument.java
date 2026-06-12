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
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.command.subcommand.Argument;
import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum WorldsArgument implements Argument {
    ARCHIVE("archive", "buildsystem.navigator"),
    ADD_BUILDER("addBuilder", "buildsystem.addbuilder"),
    BACKUP("backup", "buildsystem.backup"),
    BUILDERS("builders", "buildsystem.builders"),
    DELETE("delete", "buildsystem.delete"),
    EDIT("edit", "buildsystem.edit"),
    FOLDER("folder", "buildsystem.folder"),
    HELP("help", "buildsystem.help.worlds"),
    IMPORT("import", "buildsystem.import"),
    IMPORT_ALL("importAll", "buildsystem.import.all"),
    INFO("info", "buildsystem.info"),
    ITEM("item", "buildsystem.navigator.item"),
    PRIVATE("private", "buildsystem.navigator"),
    PUBLIC("public", "buildsystem.navigator"),
    REMOVE_BUILDER("removeBuilder", "buildsystem.removebuilder"),
    RENAME("rename", "buildsystem.rename"),
    SET_CREATOR("setCreator", "buildsystem.setcreator"),
    SET_ITEM("setItem", "buildsystem.setitem"),
    SET_PERMISSION("setPermission", "buildsystem.setpermission"),
    SET_PROJECT("setProject", "buildsystem.setproject"),
    SET_STATUS("setStatus", "buildsystem.setstatus"),
    SET_SPAWN("setSpawn", "buildsystem.setspawn"),
    REMOVE_SPAWN("removeSpawn", "buildsystem.removespawn"),
    TP("tp", "buildsystem.worldtp"),
    UNIMPORT("unimport", "buildsystem.unimport");

    private final String command;
    private final String permission;

    WorldsArgument(String command, String permission) {
        this.command = command;
        this.permission = permission;
    }

    public static @Nullable WorldsArgument matchArgument(String input) {
        return Arrays.stream(values())
                .filter(argument -> argument.getName().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getName() {
        return command;
    }

    @Override
    public String getPermission() {
        return permission;
    }
}
