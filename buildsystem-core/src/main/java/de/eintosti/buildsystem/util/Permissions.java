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
package de.eintosti.buildsystem.util;

import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/**
 * Every permission node the plugin checks. Nodes are declared here rather than inline so a typo is a compile error
 * instead of a check that silently never passes, and so the full set the plugin ships can be read in one place.
 *
 * <p>Nodes whose last segment is supplied at runtime (world types, templates, generators, navigator categories,
 * statuses, settings toggles, gamemodes) are built by the helper methods at the bottom rather than by concatenating
 * a prefix at the call site.
 */
@NullMarked
public final class Permissions {

    private Permissions() {}

    public static final String ADDBUILDER = "buildsystem.addbuilder";
    public static final String ADMIN = "buildsystem.admin";
    public static final String BACK = "buildsystem.back";
    public static final String BACKUP = "buildsystem.backup";
    public static final String BACKUP_CREATE = "buildsystem.backup.create";
    public static final String BLOCKS = "buildsystem.blocks";
    public static final String BUILD = "buildsystem.build";
    public static final String BUILD_OTHER = "buildsystem.build.other";
    public static final String BUILDERS = "buildsystem.builders";
    public static final String BYPASS_ARCHIVE = "buildsystem.bypass.archive";
    public static final String BYPASS_BUILDERS = "buildsystem.bypass.builders";
    public static final String BYPASS_PERMISSION = "buildsystem.bypass.permission";
    public static final String BYPASS_PERMISSION_ARCHIVE = "buildsystem.bypass.permission.archive";
    public static final String BYPASS_PERMISSION_PRIVATE = "buildsystem.bypass.permission.private";
    public static final String BYPASS_PERMISSION_PUBLIC = "buildsystem.bypass.permission.public";
    public static final String BYPASS_SETTINGS = "buildsystem.bypass.settings";
    public static final String COLOR_CHAT = "buildsystem.color.chat";
    public static final String COLOR_SIGN = "buildsystem.color.sign";
    public static final String CONFIG = "buildsystem.config";
    public static final String CREATE_FOLDER = "buildsystem.create.folder";
    public static final String DAY = "buildsystem.day";
    public static final String DELETE = "buildsystem.delete";
    public static final String EDIT = "buildsystem.edit";
    public static final String EDIT_BREAKING = "buildsystem.edit.breaking";
    public static final String EDIT_BUILDERS = "buildsystem.edit.builders";
    public static final String EDIT_DIFFICULTY = "buildsystem.edit.difficulty";
    public static final String EDIT_ENTITIES = "buildsystem.edit.entities";
    public static final String EDIT_EXPLOSIONS = "buildsystem.edit.explosions";
    public static final String EDIT_GAMERULES = "buildsystem.edit.gamerules";
    public static final String EDIT_ICON = "buildsystem.edit.icon";
    public static final String EDIT_INTERACTIONS = "buildsystem.edit.interactions";
    public static final String EDIT_MOBAI = "buildsystem.edit.mobai";
    public static final String EDIT_PERMISSION = "buildsystem.edit.permission";
    public static final String EDIT_PHYSICS = "buildsystem.edit.physics";
    public static final String EDIT_PIN = "buildsystem.edit.pin";
    public static final String EDIT_PLACEMENT = "buildsystem.edit.placement";
    public static final String EDIT_PROJECT = "buildsystem.edit.project";
    public static final String EDIT_STATUS = "buildsystem.edit.status";
    public static final String EDIT_TIME = "buildsystem.edit.time";
    public static final String EDIT_VISIBILITY = "buildsystem.edit.visibility";
    public static final String EXPLOSIONS = "buildsystem.explosions";
    public static final String FOLDER = "buildsystem.folder";
    public static final String FOLDER_ADD = "buildsystem.folder.add";
    public static final String FOLDER_DELETE = "buildsystem.folder.delete";
    public static final String FOLDER_REMOVE = "buildsystem.folder.remove";
    public static final String FOLDER_SETITEM = "buildsystem.folder.setitem";
    public static final String FOLDER_SETPERMISSION = "buildsystem.folder.setpermission";
    public static final String FOLDER_SETPROJECT = "buildsystem.folder.setproject";
    public static final String GAMEMODE = "buildsystem.gamemode";
    public static final String HELP_BUILDSYSTEM = "buildsystem.help.buildsystem";
    public static final String HELP_WORLDS = "buildsystem.help.worlds";
    public static final String IMPORT = "buildsystem.import";
    public static final String IMPORT_ALL = "buildsystem.import.all";
    public static final String INFO = "buildsystem.info";
    public static final String NAVIGATOR = "buildsystem.navigator";
    public static final String NAVIGATOR_ITEM = "buildsystem.navigator.item";
    public static final String NIGHT = "buildsystem.night";
    public static final String NOAI = "buildsystem.noai";
    public static final String PHYSICS = "buildsystem.physics";
    public static final String PHYSICS_MESSAGE = "buildsystem.physics.message";
    public static final String REMOVEBUILDER = "buildsystem.removebuilder";
    public static final String REMOVESPAWN = "buildsystem.removespawn";
    public static final String RENAME = "buildsystem.rename";
    public static final String SAVETEMPLATE = "buildsystem.savetemplate";
    public static final String SETCREATOR = "buildsystem.setcreator";
    public static final String SETITEM = "buildsystem.setitem";
    public static final String SETPERMISSION = "buildsystem.setpermission";
    public static final String SETPROJECT = "buildsystem.setproject";
    public static final String SETSPAWN = "buildsystem.setspawn";
    public static final String SETSTATUS = "buildsystem.setstatus";
    public static final String SETTINGS = "buildsystem.settings";
    public static final String SETUP = "buildsystem.setup";
    public static final String SKULL = "buildsystem.skull";
    public static final String SPAWN = "buildsystem.spawn";
    public static final String SPEED = "buildsystem.speed";
    public static final String TOP = "buildsystem.top";
    public static final String UNIMPORT = "buildsystem.unimport";
    public static final String UPDATES = "buildsystem.updates";
    public static final String WORLDTP = "buildsystem.worldtp";

    /**
     * {@return the permission to create a world of the given type} The type is lowercased, matching the nodes
     * declared in {@code plugin.yml}.
     *
     * @param worldType The world type name
     */
    public static String createType(String worldType) {
        return "buildsystem.create.type." + worldType.toLowerCase(Locale.ROOT);
    }

    /**
     * {@return the permission to create a world from the given template}
     *
     * @param templateName The template name, as it appears on disk
     */
    public static String createTemplate(String templateName) {
        return "buildsystem.create.template." + templateName;
    }

    /**
     * {@return the permission to create a world with the given custom generator}
     *
     * @param generatorName The generator name
     */
    public static String createGenerator(String generatorName) {
        return "buildsystem.create.generator." + generatorName;
    }

    /**
     * {@return the permission to create a world in the given navigator category}
     *
     * @param categoryId The category id
     */
    public static String createInCategory(String categoryId) {
        return "buildsystem.create." + categoryId;
    }

    /**
     * {@return the permission to see the given navigator category and use its {@code /worlds <category>} shortcut}
     *
     * @param categoryId The category id
     */
    public static String navigatorCategory(String categoryId) {
        return "buildsystem.navigator." + categoryId;
    }

    /**
     * {@return the permission to assign the given world status}
     *
     * @param statusId The status id
     */
    public static String setStatus(String statusId) {
        return "buildsystem.setstatus." + statusId.toLowerCase(Locale.ROOT);
    }

    /**
     * {@return the permission to toggle the given {@code /settings} option}
     *
     * @param node The settings node, e.g. {@code no-clip}
     */
    public static String setting(String node) {
        return "buildsystem.setting." + node;
    }

    /**
     * {@return the permission to change one's own gamemode}
     *
     * @param gameMode The gamemode name
     */
    public static String gamemode(String gameMode) {
        return "buildsystem.gamemode." + gameMode.toLowerCase(Locale.ROOT);
    }

    /**
     * {@return the permission to change another player's gamemode}
     *
     * @param gameMode The gamemode name
     */
    public static String gamemodeOther(String gameMode) {
        return "buildsystem.gamemode." + gameMode.toLowerCase(Locale.ROOT) + ".other";
    }

    /**
     * {@return the per-world command permission for the given command name} Used by the time commands, which gate
     * {@code /day}, {@code /night} and friends per world.
     *
     * @param command The command name
     */
    public static String command(String command) {
        return "buildsystem." + command.toLowerCase(Locale.ROOT);
    }
}
