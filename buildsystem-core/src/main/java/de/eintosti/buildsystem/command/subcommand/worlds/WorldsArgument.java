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
import de.eintosti.buildsystem.util.Permissions;
import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum WorldsArgument implements Argument {
    ADD_BUILDER("addBuilder", Permissions.ADDBUILDER),
    BACKUP("backup", Permissions.BACKUP),
    BUILDERS("builders", Permissions.BUILDERS),
    DELETE("delete", Permissions.DELETE),
    DOWNLOAD("download", Permissions.DOWNLOAD),
    EDIT("edit", Permissions.EDIT),
    FOLDER("folder", Permissions.FOLDER),
    HELP("help", Permissions.HELP_WORLDS),
    IMPORT("import", Permissions.IMPORT),
    IMPORT_ALL("importAll", Permissions.IMPORT_ALL),
    INFO("info", Permissions.INFO),
    ITEM("item", Permissions.NAVIGATOR_ITEM),
    REMOVE_BUILDER("removeBuilder", Permissions.REMOVEBUILDER),
    RENAME("rename", Permissions.RENAME),
    SAVE_TEMPLATE("saveTemplate", Permissions.SAVETEMPLATE),
    SET_CREATOR("setCreator", Permissions.SETCREATOR),
    SET_ITEM("setItem", Permissions.SETITEM),
    SET_PERMISSION("setPermission", Permissions.SETPERMISSION),
    SET_PROJECT("setProject", Permissions.SETPROJECT),
    SET_STATUS("setStatus", Permissions.SETSTATUS),
    SET_SPAWN("setSpawn", Permissions.SETSPAWN),
    REMOVE_SPAWN("removeSpawn", Permissions.REMOVESPAWN),
    TP("tp", Permissions.WORLDTP),
    UNIMPORT("unimport", Permissions.UNIMPORT);

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
