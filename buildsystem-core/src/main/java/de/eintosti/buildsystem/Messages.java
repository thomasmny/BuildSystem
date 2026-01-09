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

import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Messages {

    private static final BuildSystemPlugin PLUGIN = BuildSystemPlugin.get();
    private static final Map<String, String> MESSAGES = new HashMap<>();
    private static final boolean PLACEHOLDER_API_ENABLED = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

    @Nullable
    private static YamlConfiguration config = null;

    public static void createMessageFile() {
        File file = new File(PLUGIN.getDataFolder(), "messages.yml");
        try {
            if (file.createNewFile()) {
                PLUGIN.getLogger().info("Created file: " + file.getName());
            }
        } catch (IOException e) {
            PLUGIN.getLogger().log(Level.SEVERE, "Could not create file: " + file.getName(), e);
        }

        config = YamlConfiguration.loadConfiguration(file);
        StringBuilder sb = new StringBuilder();

        addSpacer(sb, "# ██████╗ ██╗   ██╗██╗██╗     ██████╗ ███████╗██╗   ██╗███████╗████████╗███████╗███╗   ███╗");
        addSpacer(sb, "# ██╔══██╗██║   ██║██║██║     ██╔══██╗██╔════╝╚██╗ ██╔╝██╔════╝╚══██╔══╝██╔════╝████╗ ████║");
        addSpacer(sb, "# ██████╔╝██║   ██║██║██║     ██║  ██║███████╗ ╚████╔╝ ███████╗   ██║   █████╗  ██╔████╔██║");
        addSpacer(sb, "# ██╔══██╗██║   ██║██║██║     ██║  ██║╚════██║  ╚██╔╝  ╚════██║   ██║   ██╔══╝  ██║╚██╔╝██║");
        addSpacer(sb, "# ██████╔╝╚██████╔╝██║███████╗██████╔╝███████║   ██║   ███████║   ██║   ███████╗██║ ╚═╝ ██║");
        addSpacer(sb, "# ╚═════╝  ╚═════╝ ╚═╝╚══════╝╚═════╝ ╚══════╝   ╚═╝   ╚══════╝   ╚═╝   ╚══════╝╚═╝     ╚═╝");
        addSpacer(sb, "");
        addSpacer(sb, "");
        addSpacer(sb, "# ---------");
        addSpacer(sb, "# Messages");
        addSpacer(sb, "# ---------");
        setMessage(config, sb, "prefix", "&8▎ &bBuildSystem &8»");
        setMessage(config, sb, "player_join", "&7[&a+&7] &a%player%");
        setMessage(config, sb, "player_quit", "&7[&c-&7] &c%player%");
        setMessage(config, sb, "loading_world", "&7Loading &b%world%&7...");
        setMessage(config, sb, "world_not_loaded", "&cWorld is not loaded!");
        setMessage(config, sb, "enter_world_name", "&7Enter &bWorld Name");
        setMessage(config, sb, "enter_folder_name", "&7Enter &bFolder Name");
        setMessage(config, sb, "enter_generator_name", "&7Enter &bGenerator Name");
        setMessage(config, sb, "enter_world_creator", "&7Enter &bWorld Creator");
        setMessage(config, sb, "enter_world_permission", "&7Enter &bPermission");
        setMessage(config, sb, "enter_world_project", "&7Enter &bProject");
        setMessage(config, sb, "enter_player_name", "&7Enter &bPlayer Name");
        setMessage(config, sb, "cancel_subtitle", "&7Type &ccancel &7to cancel");
        setMessage(config, sb, "input_cancelled", "%prefix% &cInput cancelled!");
        setMessage(config, sb, "update_available", Arrays.asList(
                "%prefix% &7Great! A new update is available &8[&bv%new_version%&8]",
                " &8➥ &7Your current version: &bv%current_version%"
        ));
        setMessage(config, sb, "command_archive_world", "%prefix% &cYou can't use that command here!");
        setMessage(config, sb, "command_not_builder", "%prefix% &cOnly builders can use that command!");
        addSpacer(sb, "");
        addSpacer(sb, "");
        addSpacer(sb, "# ---------");
        addSpacer(sb, "# Scoreboard");
        addSpacer(sb, "# ---------");
        setMessage(config, sb, "title", "&b&lBuildSystem");
        setMessage(config, sb, "body", Arrays.asList(
                "&7&m                     &8",
                "&7World:",
                " &b%world%",
                " ",
                "&7Status:",
                " %status%",
                "&7&m                     &7"
        ));
        addSpacer(sb, "");
        addSpacer(sb, "");
        addSpacer(sb, "# ---------");
        addSpacer(sb, "# Commands");
        addSpacer(sb, "# ---------");
        setMessage(config, sb, "sender_not_player", "You have to be a player to use this command!");
        setMessage(config, sb, "no_permissions", "%prefix% &cNot enough permissions.");
        addSpacer(sb, "");
        addSpacer(sb, "# /back");
        setMessage(config, sb, "back_usage", "%prefix% &7Usage: &b/back");
        setMessage(config, sb, "back_teleported", "%prefix% &7You were teleported to your &bprevious location&7.");
        setMessage(config, sb, "back_failed", "%prefix% &cNo previous location was found.");
        addSpacer(sb, "");
        addSpacer(sb, "# /build");
        setMessage(config, sb, "build_usage", "%prefix% &7Usage: &b/build [player]");
        setMessage(config, sb, "build_player_not_found", "%prefix% &cThat player was not found.");
        setMessage(config, sb, "build_activated_self", "%prefix% &7Build mode was &aactivated&7.");
        setMessage(config, sb, "build_activated_other_sender", "%prefix% &7Build mode &8[&7for %target%&8] &7was &aactivated&7.");
        setMessage(config, sb, "build_activated_other_target", "%prefix% &7Build mode was &aactivated &8[&7by %sender%&8]&7.");
        setMessage(config, sb, "build_deactivated_self", "%prefix% &7Build mode was &cdeactivated&7.");
        setMessage(config, sb, "build_deactivated_other_sender", "%prefix% &7Build mode &8[&7for %target%&8] &7was &cdeactivated&7.");
        setMessage(config, sb, "build_deactivated_other_target", "%prefix% &7Build mode was &cdeactivated &8[&7by %sender%&8]&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /buildsystem");
        setMessage(config, sb, "buildsystem_usage", "%prefix% &7Usage: &b/buildsystem [page]");
        setMessage(config, sb, "buildsystem_invalid_page", "%prefix% &cInvalid page.");
        setMessage(config, sb, "buildsystem_title_with_page", "%prefix% &7&nBuildSystem Help:&8 (&7%page%/%max%&8)");
        setMessage(config, sb, "buildsystem_permission", "&7&nPermission&8: &b%permission%");
        setMessage(config, sb, "buildsystem_back", "&7Teleport to your previous location.");
        setMessage(config, sb, "buildsystem_blocks", "&7Opens a menu with secret blocks.");
        setMessage(config, sb, "buildsystem_build", "&7Puts you into 'build mode'.");
        setMessage(config, sb, "buildsystem_config", "&7Reload the config.");
        setMessage(config, sb, "buildsystem_day", "&7Set a world's time to daytime.");
        setMessage(config, sb, "buildsystem_explosions", "&7Toggle explosions.");
        setMessage(config, sb, "buildsystem_gamemode", "&7Change your gamemode.");
        setMessage(config, sb, "buildsystem_night", "&7Set a world's time to nighttime.");
        setMessage(config, sb, "buildsystem_noai", "&7Toggle entity AIs.");
        setMessage(config, sb, "buildsystem_physics", "&7Toggle block physics.");
        setMessage(config, sb, "buildsystem_settings", "&7Manage user settings.");
        setMessage(config, sb, "buildsystem_setup", "&7Change the default items when creating worlds.");
        setMessage(config, sb, "buildsystem_skull", "&7Receive a player or custom skull.");
        setMessage(config, sb, "buildsystem_speed", "&7Change your flying/walking speed.");
        setMessage(config, sb, "buildsystem_spawn", "&7Teleport to the spawn.");
        setMessage(config, sb, "buildsystem_top", "&7Teleport to the the highest location above you.");
        setMessage(config, sb, "buildsystem_worlds", "&7An overview of all &o/worlds &7commands.");
        addSpacer(sb, "");
        addSpacer(sb, "# /config");
        setMessage(config, sb, "config_usage", "%prefix% &7Usage: &b/config reload");
        setMessage(config, sb, "config_reloaded", "%prefix% &7The config was reloaded.");
        addSpacer(sb, "");
        addSpacer(sb, "# /explosions");
        setMessage(config, sb, "explosions_usage", "%prefix% &7Usage: &b/explosions <world>");
        setMessage(config, sb, "explosions_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "explosions_world_not_imported", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "explosions_activated", "%prefix% &7Explosions in &b%world% &7were &aactivated&7.");
        setMessage(config, sb, "explosions_deactivated", "%prefix% &7Explosions in &b%world% &7were &cdeactivated&7.");
        setMessage(config, sb, "explosions_deactivated_in_world", "%prefix% &7Explosions in &b%world% &7are currently &cdeactivated&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /gamemode");
        setMessage(config, sb, "gamemode_usage", "%prefix% &7Usage: &b/gamemode <mode> [player]");
        setMessage(config, sb, "gamemode_player_not_found", "%prefix% &cThat player was not found.");
        setMessage(config, sb, "gamemode_survival", "Survival");
        setMessage(config, sb, "gamemode_creative", "Creative");
        setMessage(config, sb, "gamemode_adventure", "Adventure");
        setMessage(config, sb, "gamemode_spectator", "Spectator");
        setMessage(config, sb, "gamemode_set_self", "%prefix% &7Your gamemode was set to &b%gamemode%&7.");
        setMessage(config, sb, "gamemode_set_other", "%prefix% &b%target%&7's gamemode was set to &b%gamemode%&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /physics");
        setMessage(config, sb, "physics_usage", "%prefix% &7Usage: &b/physics <world>");
        setMessage(config, sb, "physics_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "physics_world_not_imported", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "physics_activated", "%prefix% &7Physics in &b%world% &7were &aactivated&7.");
        setMessage(config, sb, "physics_activated_all", "%prefix% &7Physics in &ball worlds &7were &aactivated&7.");
        setMessage(config, sb, "physics_deactivated", "%prefix% &7Physics in &b%world% &7were &cdeactivated&7.");
        setMessage(config, sb, "physics_deactivated_in_world", "%prefix% &7Physics in &b%world% &7are currently &cdeactivated&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /noai");
        setMessage(config, sb, "noai_usage", "%prefix% &7Usage: &b/noai <world>");
        setMessage(config, sb, "noai_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "noai_world_not_imported", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "noai_deactivated", "%prefix% &7Entity AIs in &b%world% &7were &aactivated&7.");
        setMessage(config, sb, "noai_activated", "%prefix% &7Entity AIs in &b%world% &7were &cdeactivated&7.");
        setMessage(config, sb, "noai_activated_in_world", "%prefix% &7Entity AIs in &b%world% &7are currently &cdeactivated&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /skull");
        setMessage(config, sb, "skull_usage", "%prefix% &7Usage: &b/skull [name]");
        setMessage(config, sb, "skull_player_received", "%prefix% &7You received the skull of &b%player%&7.");
        setMessage(config, sb, "skull_custom_received", "%prefix% &7You received a &bcustom skull&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /spawn");
        setMessage(config, sb, "spawn_usage", "%prefix% &7Usage: &b/spawn");
        setMessage(config, sb, "spawn_admin", "%prefix% &7Usage: &b/spawn [set/remove]");
        setMessage(config, sb, "spawn_teleported", "%prefix% &7You were teleported to the &bspawn&7.");
        setMessage(config, sb, "spawn_unavailable", "%prefix% &cThere isn't a spawn to teleport to.");
        setMessage(config, sb, "spawn_world_not_imported", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "spawn_set", "%prefix% &7Spawn set to &b%x% %y% %z% &7in &b%world%&7.");
        setMessage(config, sb, "spawn_remove", "%prefix% &7The spawn was removed.");
        addSpacer(sb, "");
        addSpacer(sb, "# /speed");
        setMessage(config, sb, "speed_usage", "%prefix% &7Usage: &b/speed [1-5]");
        setMessage(config, sb, "speed_set_flying", "%prefix% &7Your flying speed was set to &b%speed%&7.");
        setMessage(config, sb, "speed_set_walking", "%prefix% &7Your walking speed was set to &b%speed%&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /day");
        setMessage(config, sb, "day_usage", "%prefix% &7Usage: &b/day [world]");
        setMessage(config, sb, "day_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "day_set", "%prefix% &7It is now day in &b%world%&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /night");
        setMessage(config, sb, "night_usage", "%prefix% &7Usage: &b/night [world]");
        setMessage(config, sb, "night_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "night_set", "%prefix% &7It is now night in &b%world%&7.");
        addSpacer(sb, "");
        addSpacer(sb, "# /top");
        setMessage(config, sb, "top_usage", "%prefix% &7Usage: &b/top");
        setMessage(config, sb, "top_teleported", "%prefix% &7You were teleported to the &btop&7.");
        setMessage(config, sb, "top_failed", "%prefix% &cNo higher location was found.");
        addSpacer(sb, "");
        addSpacer(sb, "# /worlds");
        setMessage(config, sb, "worlds_addbuilder_usage", "%prefix% &7Usage: &b/worlds addBuilder [player]");
        setMessage(config, sb, "worlds_addbuilder_unknown_world", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "worlds_addbuilder_player_not_found", "%prefix% &cThat player was not found.");
        setMessage(config, sb, "worlds_addbuilder_already_creator", "%prefix% &cYou are already the creator.");
        setMessage(config, sb, "worlds_addbuilder_already_added", "%prefix% &cThis player is already a builder.");
        setMessage(config, sb, "worlds_addbuilder_added", "%prefix% &b%builder% &7was &aadded &7as a builder.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_backup_usage", "%prefix% &7Usage: &b/worlds backup [create]");
        setMessage(config, sb, "worlds_backup_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_backup_world_not_imported", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "worlds_backup_created", "%prefix% &7Successfully &acreated &7a backup of &b%world%&7.");
        setMessage(config, sb, "worlds_backup_failed", "%prefix% &cUnable to create a backup of %world%.");
        setMessage(config, sb, "worlds_backup_restoration_in_progress", "%prefix% &7&oThe world you are in is being restored...");
        setMessage(config, sb, "worlds_backup_restoration_successful", "%prefix% &7The world has been successfully reset to the state from &a%timestamp%&7.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_builders_usage", "%prefix% &7Usage: &b/worlds builders <world>");
        setMessage(config, sb, "worlds_builders_unknown_world", "%prefix% &cUnknown world.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_world_name", "World name");
        setMessage(config, sb, "worlds_world_exists", "%prefix% &cThis world already exists.");
        setMessage(config, sb, "worlds_world_creation_invalid_characters", "%prefix% &7&oRemoved invalid characters from world name.");
        setMessage(config, sb, "worlds_world_creation_name_bank", "%prefix% &cThe world name cannot be blank.");
        setMessage(config, sb, "worlds_world_creation_started", "%prefix% &7The creation of &b%world% &8(&7Type: &f%type%&8) &7has started...");
        setMessage(config, sb, "worlds_template_creation_started", "%prefix% &7The creation of &b%world% &8(&7Template: &f%template%&8) &7has started...");
        setMessage(config, sb, "worlds_creation_finished", "%prefix% &7The world was &asuccessfully &7created.");
        setMessage(config, sb, "worlds_template_does_not_exist", "%prefix% &cThis template does not exist.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_unknown_command", "%prefix% &7Unknown command: &b/worlds help");
        setMessage(config, sb, "worlds_navigator_open", "%prefix% &cYou have already opened the navigator!");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_delete_usage", "%prefix% &7Usage: &b/worlds delete <world>");
        setMessage(config, sb, "worlds_delete_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_delete_unknown_directory", "%prefix% &cError while deleting world: Directory not found!");
        setMessage(config, sb, "worlds_delete_forbidden", "%prefix% &cYou are not allowed to delete this world.");
        setMessage(config, sb, "worlds_delete_canceled", "%prefix% &7The deletion of &b%world% &7was canceled.");
        setMessage(config, sb, "worlds_delete_error", "%prefix% &7cThe deletion of %world% failed.");
        setMessage(config, sb, "worlds_delete_started", "%prefix% &7The deletion of &b%world% &7has started...");
        setMessage(config, sb, "worlds_delete_finished", "%prefix% &7The world was &asuccessfully &7deleted.");
        setMessage(config, sb, "worlds_delete_players_world", "%prefix% &7&oThe world you were in was deleted.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_edit_usage", "%prefix% &7Usage: &b/worlds edit <world>");
        setMessage(config, sb, "worlds_edit_unknown_world", "%prefix% &cUnknown world.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_folder_usage", "%prefix% &7Usage: &b/worlds folder <folder> <add|remove|delete|setPermission|setProject|setItem> [<world>]");
        setMessage(config, sb, "worlds_folder_unknown_folder", "%prefix% &cUnknown folder.");
        setMessage(config, sb, "worlds_folder_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_folder_world_already_in_folder", "%prefix% &c%world% is already contained within %folder%.");
        setMessage(config, sb, "worlds_folder_world_already_in_another_folder", "%prefix% &c%world% is already contained within another folder.");
        setMessage(config, sb, "worlds_folder_world_category_mismatch", "%prefix% &cThe folder's category (%folder_category%) does not match the world's category (%world_category%).");
        setMessage(config, sb, "worlds_folder_world_not_in_folder", "%prefix% &c%world% is not contained within %folder%.");
        setMessage(config, sb, "worlds_folder_world_added_to_folder", "%prefix% &b%world% &7was &aadded &7to &b%folder%&7.");
        setMessage(config, sb, "worlds_folder_world_removed_from_folder", "%prefix% &b%world% &7was &cremoved &7from &b%folder%&7.");
        setMessage(config, sb, "worlds_folder_exists", "%prefix% &cThis folder already exists.");
        setMessage(config, sb, "worlds_folder_creation_invalid_characters", "%prefix% &7&oRemoved invalid characters from folder name.");
        setMessage(config, sb, "worlds_folder_creation_name_bank", "%prefix% &cThe folder name cannot be blank.");
        setMessage(config, sb, "worlds_folder_created", "%prefix% &7The folder &b%folder% &7was &asuccessfully &7created.");
        setMessage(config, sb, "worlds_folder_not_empty", "%prefix% &cThe folder %folder% is not empty.");
        setMessage(config, sb, "worlds_folder_deleted", "%prefix% &7The folder &b%folder% &7was &asuccessfully &7deleted.");
        setMessage(config, sb, "worlds_folder_permission_set", "%prefix% &b%folder%&7's permission was successfully changed.");
        setMessage(config, sb, "worlds_folder_project_set", "%prefix% &b%folder%&7's project was successfully changed.");
        setMessage(config, sb, "worlds_folder_item_set", "%prefix% &b%folder%&7's item was successfully changed.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_help_usage", "%prefix% &7Usage: &b/worlds help [page]");
        setMessage(config, sb, "worlds_help_invalid_page", "%prefix% &cInvalid page.");
        setMessage(config, sb, "worlds_help_title_with_page", "%prefix% &7&nWorlds Help:&8 (&7%page%/%max%&8)");
        setMessage(config, sb, "worlds_help_permission", "&7&nPermission&8: &b%permission%");
        setMessage(config, sb, "worlds_help_help", "&7Shows the list of all subcommands.");
        setMessage(config, sb, "worlds_help_info", "&7Shows information about a world.");
        setMessage(config, sb, "worlds_help_item", "&7Receive the 'World Navigator'.");
        setMessage(config, sb, "worlds_help_tp", "&7Teleport to another world.");
        setMessage(config, sb, "worlds_help_edit", "&7Opens the world editor.");
        setMessage(config, sb, "worlds_help_addbuilder", "&7Add a builder to a world.");
        setMessage(config, sb, "worlds_help_removebuilder", "&7Remove a builder from a &7world.");
        setMessage(config, sb, "worlds_help_builders", "&7Opens a world's list of builders.");
        setMessage(config, sb, "worlds_help_rename", "&7Rename an existing world.");
        setMessage(config, sb, "worlds_help_setitem", "&7Set a world's item.");
        setMessage(config, sb, "worlds_help_setcreator", "&7Set a world's creator.");
        setMessage(config, sb, "worlds_help_setproject", "&7Set a world's project.");
        setMessage(config, sb, "worlds_help_setpermission", "&7Set a world's permission.");
        setMessage(config, sb, "worlds_help_setstatus", "&7Set a world's status.");
        setMessage(config, sb, "worlds_help_setspawn", "&7Set a world's spawnpoint.");
        setMessage(config, sb, "worlds_help_removespawn", "&7Removes a world's spawnpoint.");
        setMessage(config, sb, "worlds_help_delete", "&7Delete a world.");
        setMessage(config, sb, "worlds_help_import", "&7Import a world.");
        setMessage(config, sb, "worlds_help_importall", "&7Import all worlds at once.");
        setMessage(config, sb, "worlds_help_unimport", "&7Unimport a world.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_import_usage", "%prefix% &7Usage: &b/worlds import <world> [-g <generator> | -c <creator>]");
        setMessage(config, sb, "worlds_import_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_import_world_is_imported", "%prefix% &cThis world is already imported.");
        setMessage(config, sb, "worlds_import_unknown_generator", "%prefix% &cUnknown generator.");
        setMessage(config, sb, "worlds_import_player_not_found", "%prefix% &cThat player was not found.");
        setMessage(config, sb, "worlds_import_newer_version", "%prefix% &b%world% &7was created in a &cnewer version &7of Minecraft. Unable to import.");
        setMessage(config, sb, "worlds_import_started", "%prefix% &7The import of &b%world% &7has started...");
        setMessage(config, sb, "worlds_import_invalid_character", "%prefix% &7Unable to import &c%world%&7.\n" +
                "%prefix% &7&oName contains invalid character: &c%char%");
        setMessage(config, sb, "worlds_import_finished", "%prefix% &7The world was &asuccessfully &7imported.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_importall_usage", "%prefix% &7Usage: &b/worlds importall [-g <generator> | -c <creator>]");
        setMessage(config, sb, "worlds_importall_no_worlds", "%prefix% &cNo worlds were found.");
        setMessage(config, sb, "worlds_importall_started", "%prefix% &7Beginning import of &b%amount% &7worlds...");
        setMessage(config, sb, "worlds_importall_delay", "%prefix% &8➥ &7Delay between each world: &b%delay%s&7.");
        setMessage(config, sb, "worlds_importall_already_started", "%prefix% &cAll worlds are already being imported.");
        setMessage(config, sb, "worlds_importall_player_not_found", "%prefix% &cThat player was not found.");
        setMessage(config, sb, "worlds_importall_invalid_character", "%prefix% &c✘ &7&o%world% &7contains invalid character &8(&c%char%&8)");
        setMessage(config, sb, "worlds_importall_world_already_imported", "%prefix% &c✘ &7World already imported: &b%world%");
        setMessage(config, sb, "worlds_importall_newer_version", "%prefix% &c✘ &b%world% &7was created in a &cnewer version &7of Minecraft");
        setMessage(config, sb, "worlds_importall_world_imported", "%prefix% &a✔ &7World imported: &b%world%");
        setMessage(config, sb, "worlds_importall_finished", "%prefix% &7All worlds have been &asuccessfully &7imported.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_info_usage", "%prefix% &7Usage: &b/worlds info [world]");
        setMessage(config, sb, "worlds_info_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "world_info", Arrays.asList(
                "&7&m-------------------------------------",
                "%prefix% &7&nWorld info:&b %world%",
                " ",
                " &8- &7UUID: &b%uuid%",
                " &8- &7Creator: &b%creator%",
                " &8- &7Type: &b%type%",
                " &8- &7Private: &b%private%",
                " &8- &7Builders enabled: &b%builders_enabled%",
                " &8- &7Builders: &b%builders%",
                " &8- &7Item: &b%item%",
                " &8- &7Status: &b%status%",
                " &8- &7Project: &b%project%",
                " &8- &7Permission: &b%permission%",
                " &8- &7Time: &b%time%",
                " &8- &7Creation date: &b%creation%",
                " &8- &7Physics: &b%physics%",
                " &8- &7Explosions: &b%explosions%",
                " &8- &7Block breaking: &b%block_breaking%",
                " &8- &7Block placement: &b%block_placement%",
                " &8- &7MobAI: &b%mobai%",
                " &8- &7Custom spawn: &b%custom_spawn%",
                "&7&m-------------------------------------"));
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_item_receive", "%prefix% &7You received the &bNavigator&7.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_removebuilder_usage", "%prefix% &7Usage: &b/worlds removeBuilder [player]");
        setMessage(config, sb, "worlds_removebuilder_error", "%prefix% &cError: Please try again!");
        setMessage(config, sb, "worlds_removebuilder_unknown_world", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "worlds_removebuilder_player_not_found", "%prefix% &cThat player was not found.");
        setMessage(config, sb, "worlds_removebuilder_not_yourself", "%prefix% &cYou cannot remove yourself as creator.");
        setMessage(config, sb, "worlds_removebuilder_not_builder", "%prefix% &cThis player is not a builder.");
        setMessage(config, sb, "worlds_removebuilder_removed", "%prefix% &b%builder% &7was &cremoved &7as a builder.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_rename_usage", "%prefix% &7Usage: &b/worlds rename <world>");
        setMessage(config, sb, "worlds_rename_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_rename_error", "%prefix% &cPlease try again.");
        setMessage(config, sb, "worlds_rename_same_name", "%prefix% &cThis is the world's current name.");
        setMessage(config, sb, "worlds_rename_set", "%prefix% &b%oldName% &7was successfully renamed to &b%newName%&7.");
        setMessage(config, sb, "worlds_rename_players_world", "%prefix% &7&oThe world you are in is being renamed...");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_setitem_usage", "%prefix% &7Usage: &b/worlds setItem <world>");
        setMessage(config, sb, "worlds_setitem_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_setitem_hand_empty", "%prefix% &cYou do not have an item in your hand.");
        setMessage(config, sb, "worlds_setitem_set", "%prefix% &b%world%&7's item was successfully changed.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_setcreator_usage", "%prefix% &7Usage: &b/worlds setCreator <world>");
        setMessage(config, sb, "worlds_setcreator_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_setcreator_error", "%prefix% &cPlease try again.");
        setMessage(config, sb, "worlds_setcreator_set", "%prefix% &b%world%&7's creator was successfully changed.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_setproject_usage", "%prefix% &7Usage: &b/worlds setProject <world>");
        setMessage(config, sb, "worlds_setproject_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_setproject_error", "%prefix% &cPlease try again.");
        setMessage(config, sb, "worlds_setproject_set", "%prefix% &b%world%&7's project was successfully changed.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_setstatus_usage", "%prefix% &7Usage: &b/worlds setStatus <world>");
        setMessage(config, sb, "worlds_setstatus_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_setstatus_set", "%prefix% &b%world%&7's status was was changed to: %status%&7.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_setpermission_usage", "%prefix% &7Usage: &b/worlds setPermission <world>");
        setMessage(config, sb, "worlds_setpermission_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_setpermission_error", "%prefix% &cPlease try again.");
        setMessage(config, sb, "worlds_setpermission_set", "%prefix% &b%world%&7's permission was successfully changed.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_setspawn_world_not_imported", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "worlds_setspawn_world_spawn_set", "%prefix% &b%world%&7's spawnpoint was set.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_removespawn_world_not_imported", "%prefix% &cWorld must be imported » /worlds import <world>");
        setMessage(config, sb, "worlds_removespawn_world_spawn_removed", "%prefix% &b%world%&7's spawnpoint was removed.");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_tp_usage", "%prefix% &7Usage: &b/worlds tp <world>");
        setMessage(config, sb, "worlds_tp_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_tp_world_not_imported", "%prefix% &cWorld must be imported: /worlds import <world>");
        setMessage(config, sb, "worlds_tp_entry_forbidden", "%prefix% &cYou are not allowed to enter this world!");
        addSpacer(sb, "");
        setMessage(config, sb, "worlds_unimport_usage", "%prefix% &7Usage: &b/worlds unimport <world>");
        setMessage(config, sb, "worlds_unimport_unknown_world", "%prefix% &cUnknown world.");
        setMessage(config, sb, "worlds_unimport_players_world", "%prefix% &7&oThe world you were in was unimported.");
        setMessage(config, sb, "worlds_unimport_finished", "%prefix% &b%world% &7has been &aunimported&7.");
        addSpacer(sb, "");
        addSpacer(sb, "");
        addSpacer(sb, "# ---------");
        addSpacer(sb, "# Items");
        addSpacer(sb, "# ---------");
        setMessage(config, sb, "navigator_item", "&b&lNavigator");
        setMessage(config, sb, "barrier_item", "&c&lClose Inventory");
        setMessage(config, sb, "custom_skull_item", "&bCustom Skull");
        addSpacer(sb, "");
        addSpacer(sb, "");
        addSpacer(sb, "# ---------");
        addSpacer(sb, "# GUIs");
        addSpacer(sb, "# ---------");
        addSpacer(sb, "# Multi-page inventory");
        setMessage(config, sb, "gui_previous_page", "&b« &7Previous Page");
        setMessage(config, sb, "gui_next_page", "&7Next Page &b»");
        addSpacer(sb, "");
        addSpacer(sb, "# Old Navigator");
        setMessage(config, sb, "old_navigator_title", "&3» &8Navigator");
        setMessage(config, sb, "old_navigator_world_navigator", "&aWorld Navigator");
        setMessage(config, sb, "old_navigator_world_archive", "&6World Archive");
        setMessage(config, sb, "old_navigator_private_worlds", "&bPrivate Worlds");
        setMessage(config, sb, "old_navigator_settings", "&cSettings");
        addSpacer(sb, "");
        addSpacer(sb, "# New Navigator");
        setMessage(config, sb, "new_navigator_world_navigator", "&a&lWORLD NAVIGATOR");
        setMessage(config, sb, "new_navigator_world_archive", "&6&lWORLD ARCHIVE");
        setMessage(config, sb, "new_navigator_private_worlds", "&b&lPRIVATE WORLDS");
        addSpacer(sb, "");
        addSpacer(sb, "# World Navigator");
        setMessage(config, sb, "world_navigator_title", "&3» &8World Navigator");
        setMessage(config, sb, "world_navigator_no_worlds", "&c&nNo worlds available");
        setMessage(config, sb, "world_navigator_create_world", "&bCreate a Public World");
        setMessage(config, sb, "world_navigator_create_folder", "&bCreate a Folder");
        setMessage(config, sb, "world_item_title", "&3&l%world%");
        setMessage(config, sb, "world_item_lore_normal", Arrays.asList(
                "&7Status&8: %status%",
                "",
                "&7Creator&8: &b%creator%",
                "&7Project&8: &b%project%",
                "&7Permission&8: &b%permission%",
                "",
                "&7Builders&8:",
                "%builders%"
        ));
        setMessage(config, sb, "world_item_lore_edit", Arrays.asList(
                "&7Status&8: %status%",
                "",
                "&7Creator&8: &b%creator%",
                "&7Project&8: &b%project%",
                "&7Permission&8: &b%permission%",
                "",
                "&7Builders&8:",
                "%builders%",
                "",
                "&8- &7&oLeft click&8: &7Teleport",
                "&8- &7&oRight click&8: &7Edit"
        ));
        setMessage(config, sb, "folder_title", "&3» &8%folder%");
        setMessage(config, sb, "folder_item_title", "&3&l%folder%");
        setMessage(config, sb, "folder_item_lore", Arrays.asList(
                "&7Project&8: &b%project%",
                "&7Permission&8: &b%permission%",
                "",
                "&7Worlds&8: &b%worlds%"
        ));
        setMessage(config, sb, "world_item_builders_builder_template", "&b%builder%&7, ");
        setMessage(config, sb, "world_sort_title", "&bSort");
        setMessage(config, sb, "world_sort_name_az", "&8» &7&oName (A-Z)");
        setMessage(config, sb, "world_sort_name_za", "&8» &7&oName (Z-A)");
        setMessage(config, sb, "world_sort_project_az", "&8» &7&oProject (A-Z)");
        setMessage(config, sb, "world_sort_project_za", "&8» &7&oProject (Z-A)");
        setMessage(config, sb, "world_sort_status_not_started", "&8» &7&oNot Started &8&o➡ &7&oFinished");
        setMessage(config, sb, "world_sort_status_finished", "&8» &7&oFinished &8&o➡ &7&oNot Started");
        setMessage(config, sb, "world_sort_date_newest", "&8» &7&oCreation date (Newest)");
        setMessage(config, sb, "world_sort_date_oldest", "&8» &7&oCreation date (Oldest)");
        setMessage(config, sb, "world_filter_title", "&bFilter");
        setMessage(config, sb, "world_filter_mode_none", "&8» &7&oNone");
        setMessage(config, sb, "world_filter_mode_starts_with", "&8» &7&oStarts with: &b&o%text%");
        setMessage(config, sb, "world_filter_mode_contains", "&8» &7&oContains: &b&o%text%");
        setMessage(config, sb, "world_filter_mode_matches", "&8» &7&oMatches: &b&o%text%");
        setMessage(config, sb, "world_filter_lore", Arrays.asList(
                "",
                "&8- &7&oLeft click&8: &7Change text",
                "&8- &7&oRight click&8: &7Change mode",
                "&8- &7&oShift click&8: &7Reset to default"
        ));
        addSpacer(sb, "");
        addSpacer(sb, "# World Archive");
        setMessage(config, sb, "archive_title", "&3» &8World Archive");
        setMessage(config, sb, "archive_no_worlds", "&c&nNo worlds available");
        addSpacer(sb, "");
        addSpacer(sb, "# Private Worlds");
        setMessage(config, sb, "private_title", "&3» &8Private Worlds");
        setMessage(config, sb, "private_no_worlds", "&c&nNo worlds available");
        setMessage(config, sb, "private_create_world", "&bCreate a Private World");
        addSpacer(sb, "");
        addSpacer(sb, "# World Backups");
        setMessage(config, sb, "backups_title", "&3» &8World Backups");
        setMessage(config, sb, "backups_information_name", "&aInformation");
        setMessage(config, sb, "backups_information_lore", Arrays.asList(
                "",
                "&7A backup is automatically created",
                "&7every &a%interval% minutes &7in which a",
                "&7builder is present in the world.",
                "",
                "&7Next backup in: &a%remaining%"
        ));
        setMessage(config, sb, "backups_backup_name", "&a%timestamp%");
        setMessage(config, sb, "restore_backup_title", "&3» &8Restore Backup");
        setMessage(config, sb, "restore_backup_confirm_name", "&aConfirm");
        setMessage(config, sb, "restore_backup_confirm_lore", Arrays.asList(
                "",
                "&7Are you sure you to &arestore &7to the",
                "&7backup from &f%timestamp%&7?",
                "",
                "&c&nWarning&c: &7This action &cCANNOT &7be undone."
        ));
        setMessage(config, sb, "restore_backup_cancel_name", "&cCancel");
        addSpacer(sb, "");
        addSpacer(sb, "# Setup");
        setMessage(config, sb, "setup_title", "&3» &8Setup");
        setMessage(config, sb, "setup_default_item_name", "&bDefault Item");
        setMessage(config, sb, "setup_default_item_lore", Arrays.asList(
                "&7The item which a world",
                "&7has by default when created.",
                "",
                "&7&nTo change&7:",
                "&8» &7&oDrag new item onto old one"
        ));
        setMessage(config, sb, "setup_status_item_name", "&bStatus Item");
        setMessage(config, sb, "setup_status_item_name_lore", Arrays.asList(
                "&7The item which is shown when",
                "&7you change a world's status.",
                "",
                "&7&nTo change&7:",
                "&8» &7&oDrag new item onto old one"
        ));
        setMessage(config, sb, "setup_normal_world", "&bNormal World");
        setMessage(config, sb, "setup_flat_world", "&aFlat World");
        setMessage(config, sb, "setup_nether_world", "&cNether World");
        setMessage(config, sb, "setup_end_world", "&eEnd World");
        setMessage(config, sb, "setup_void_world", "&fEmpty World");
        setMessage(config, sb, "setup_imported_world", "&7Imported World");
        addSpacer(sb, "");
        addSpacer(sb, "# Create World");
        setMessage(config, sb, "create_title", "&3» &8Create World");
        setMessage(config, sb, "create_predefined_worlds", "&6Predefined Worlds");
        setMessage(config, sb, "create_templates", "&6Templates");
        setMessage(config, sb, "create_generators", "&6Generators");
        setMessage(config, sb, "create_no_templates", "&c&nNo templates available");
        setMessage(config, sb, "create_normal_world", "&bNormal World");
        setMessage(config, sb, "create_flat_world", "&aFlat World");
        setMessage(config, sb, "create_nether_world", "&cNether World");
        setMessage(config, sb, "create_end_world", "&eEnd World");
        setMessage(config, sb, "create_void_world", "&fEmpty World");
        setMessage(config, sb, "create_template", "&e%template%");
        setMessage(config, sb, "create_generators_create_world", "&eCreate world with generator");
        addSpacer(sb, "");
        addSpacer(sb, "# World Type");
        setMessage(config, sb, "type_normal", "Normal");
        setMessage(config, sb, "type_flat", "Flat");
        setMessage(config, sb, "type_nether", "Nether");
        setMessage(config, sb, "type_end", "End");
        setMessage(config, sb, "type_void", "Void");
        setMessage(config, sb, "type_custom", "Custom");
        setMessage(config, sb, "type_template", "Template");
        setMessage(config, sb, "type_private", "Private");
        setMessage(config, sb, "type_imported", "Imported");
        addSpacer(sb, "");
        addSpacer(sb, "# World Status");
        setMessage(config, sb, "status_title", "&8Status &7» &3%world%");
        setMessage(config, sb, "status_not_started", "&cNot Started");
        setMessage(config, sb, "status_in_progress", "&6In Progress");
        setMessage(config, sb, "status_almost_finished", "&aAlmost Finished");
        setMessage(config, sb, "status_finished", "&2Finished");
        setMessage(config, sb, "status_archive", "&9Archive");
        setMessage(config, sb, "status_hidden", "&fHidden");
        addSpacer(sb, "");
        addSpacer(sb, "# World Difficulty");
        setMessage(config, sb, "difficulty_peaceful", "&fPeaceful");
        setMessage(config, sb, "difficulty_easy", "&aEasy");
        setMessage(config, sb, "difficulty_normal", "&6Normal");
        setMessage(config, sb, "difficulty_hard", "&cHard");
        addSpacer(sb, "");
        addSpacer(sb, "# Delete World");
        setMessage(config, sb, "delete_title", "&3» &8Delete World");
        setMessage(config, sb, "delete_world_name", "&e%world%");
        setMessage(config, sb, "delete_world_name_lore", Arrays.asList(
                "",
                "&c&nWarning&c: &7&oOnce a world is",
                "&7&odeleted it is lost forever",
                "&7&oand cannot be recovered!"
        ));
        setMessage(config, sb, "delete_world_cancel", "&cCancel");
        setMessage(config, sb, "delete_world_confirm", "&aConfirm");
        addSpacer(sb, "");
        addSpacer(sb, "# World Editor");
        setMessage(config, sb, "worldeditor_title", "&3» &8World Editor");
        setMessage(config, sb, "worldeditor_world_item", "&3&l%world%");
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_blockbreaking_item", "&bBlock Breaking");
        setMessage(config, sb, "worldeditor_blockbreaking_lore", Arrays.asList(
                "&7&oToggle whether or not blocks",
                "&7&oare able to be broken."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_blockplacement_item", "&bBlock Placement");
        setMessage(config, sb, "worldeditor_blockplacement_lore", Arrays.asList(
                "&7&oToggle whether or not blocks",
                "&7&oare able to be placed."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_physics_item", "&bBlock Physics");
        setMessage(config, sb, "worldeditor_physics_lore", Arrays.asList(
                "&7&oToggle whether or not block",
                "&7&ophysics are activated."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_time_item", "&bTime");
        setMessage(config, sb, "worldeditor_time_lore", Arrays.asList(
                "&7&oAlter the time of day",
                "&7&oin the world.",
                "",
                "&7&nCurrently&7: %time%"
        ));
        setMessage(config, sb, "worldeditor_time_lore_sunrise", "&6Sunrise");
        setMessage(config, sb, "worldeditor_time_lore_noon", "&eNoon");
        setMessage(config, sb, "worldeditor_time_lore_night", "&9Night");
        setMessage(config, sb, "worldeditor_time_lore_unknown", "&fUnknown");
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_explosions_item", "&bExplosions");
        setMessage(config, sb, "worldeditor_explosions_lore", Arrays.asList(
                "&7&oToggle whether or not",
                "&7&oexplosions are activated."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_butcher_item", "&bButcher");
        setMessage(config, sb, "worldeditor_butcher_lore", Collections.singletonList("&7&oKill all the mobs in the world."));
        setMessage(config, sb, "worldeditor_butcher_removed", "%prefix% &b%amount% &7mobs were removed.");
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_builders_item", "&bBuilders");
        setMessage(config, sb, "worldeditor_builders_lore", Arrays.asList(
                "&7&oManage which players can",
                "&7&obuild in the world.",
                "",
                "&8- &7&oLeft click&8: &7Toggle feature",
                "&8- &7&oRight click&8: &7Manage builders"));
        setMessage(config, sb, "worldeditor_builders_not_creator_item", "&c&mBuilders");
        setMessage(config, sb, "worldeditor_builders_not_creator_lore", Arrays.asList(
                "&7&oYou are not the creator",
                "&7&oof this world."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_builders_title", "&3» &8Builders");
        setMessage(config, sb, "worldeditor_builders_creator_item", "&7&nCreator&7:");
        setMessage(config, sb, "worldeditor_builders_creator_lore", "&8» &b%creator%");
        setMessage(config, sb, "worldeditor_builders_no_creator_item", "&cWorld has no creator!");
        setMessage(config, sb, "worldeditor_builders_builder_item", "&b%builder%");
        setMessage(config, sb, "worldeditor_builders_builder_lore", Collections.singletonList("&8- &7&oShift click&8: &7Remove"));
        setMessage(config, sb, "worldeditor_builders_add_builder_item", "&bAdd builder");
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_gamerules_item", "&bGamerules");
        setMessage(config, sb, "worldeditor_gamerules_lore", Collections.singletonList("&7&oManage the world's gamerules"));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_gamerules_title", "&3» &8Gamerules");
        setMessage(config, sb, "worldeditor_gamerules_boolean_enabled", Collections.singletonList("&7&nCurrently&7: &atrue"));
        setMessage(config, sb, "worldeditor_gamerules_boolean_disabled", Collections.singletonList("&7&nCurrently&7: &cfalse"));
        setMessage(config, sb, "worldeditor_gamerules_integer", Arrays.asList(
                "&7&nCurrently&7: &e%value%",
                "",
                "&8- &7&oLeft Click&8: &7decrease by 1",
                "&8- &7&oShift &7+ &7&oLeft Click&8: &7decrease by 10",
                "&8- &7&oRight Click&8: &7increase by 1",
                "&8- &7&oShift &7+ &7&oRight Click&8: &7increase by 10"));
        setMessage(config, sb, "worldsettings_gamerule_", Arrays.asList(
                "",
                "&c&nWarning&c: &7&oOnce a world is",
                "&7&odeleted it is lost forever",
                "&7&oand cannot be recovered!"
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_visibility_item", "&bVisibility");
        setMessage(config, sb, "worldeditor_visibility_lore_public", Arrays.asList(
                "&7&oChange the world's visibility",
                "",
                "&7&nCurrently&7: &bPublic"));
        setMessage(config, sb, "worldeditor_visibility_lore_private", Arrays.asList(
                "&7&oChange the world's visibility",
                "",
                "&7&nCurrently&7: &bPrivate"
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_mobai_item", "&bMob AI");
        setMessage(config, sb, "worldeditor_mobai_lore", Collections.singletonList("&7&oToggle whether mobs have an AI."));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_blockinteractions_item", "&bBlock Interactions");
        setMessage(config, sb, "worldeditor_blockinteractions_lore", Arrays.asList(
                "&7&oToggle whether interactions",
                "&7&owith blocks are cancelled."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_difficulty_item", "&bDifficulty");
        setMessage(config, sb, "worldeditor_difficulty_lore", Arrays.asList(
                "&7&oChange the world's difficulty.",
                "",
                "&7&nCurrently&7: %difficulty%"
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_status_item", "&bStatus");
        setMessage(config, sb, "worldeditor_status_lore", Arrays.asList(
                "&7&oChange the world's status.",
                "",
                "&7&nCurrently&7: %status%"
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_project_item", "&bProject");
        setMessage(config, sb, "worldeditor_project_lore", Arrays.asList(
                "&7&oChange the world's project.",
                "",
                "&7&nCurrently&7: &b%project%"
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "worldeditor_permission_item", "&bPermission");
        setMessage(config, sb, "worldeditor_permission_lore", Arrays.asList(
                "&7&oChange the world's permission.",
                "",
                "&7&nCurrently&7: &b%permission%"
        ));
        addSpacer(sb, "");
        addSpacer(sb, "# Settings");
        setMessage(config, sb, "settings_title", "&3» &8Settings");
        addSpacer(sb, "");
        setMessage(config, sb, "settings_change_design_item", "&bChange Design");
        setMessage(config, sb, "settings_change_design_lore", Arrays.asList(
                "&7&oSelect which colour the",
                "&7&oglass panes should have."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_clear_inventory_item", "&bClear Inventory");
        setMessage(config, sb, "settings_clear_inventory_lore", Arrays.asList(
                "&7&oWhen enabled, a player's",
                "&7&oinventory is cleared on join."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_disableinteract_item", "&bDisable Block Interactions");
        setMessage(config, sb, "settings_disableinteract_lore", Arrays.asList(
                "&7&oWhen enabled, interactions with",
                "&7&ocertain blocks are disabled."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_hideplayers_item", "&bHide Players");
        setMessage(config, sb, "settings_hideplayers_lore", Arrays.asList(
                "&7&oWhen enabled, all online",
                "&7&oplayers will be hidden."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_instantplacesigns_item", "&bInstant Place Signs");
        setMessage(config, sb, "settings_instantplacesigns_lore", Arrays.asList(
                "&7&oWhen enabled, signs are placed",
                "&7&owithout opening the text input."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_keep_navigator_item", "&bKeep Navigator");
        setMessage(config, sb, "settings_keep_navigator_lore", Arrays.asList(
                "&7&oWhen enabled, the navigator",
                "&7&owill remain in your inventory",
                "&7&oeven when you clear it."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_new_navigator_item", "&bNew Navigator");
        setMessage(config, sb, "settings_new_navigator_lore", Arrays.asList(
                "&7&oA new and improved navigator",
                "&7&owhich is no longer a GUI."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_nightvision_item", "&bNightvision");
        setMessage(config, sb, "settings_nightvision_lore", Arrays.asList(
                "&7&oWhen enabled, you will",
                "&7&oreceive permanent night vision."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_no_clip_item", "&bNoClip");
        setMessage(config, sb, "settings_no_clip_lore", Arrays.asList(
                "&7&oWhen flying against a wall, you",
                "&7&owill be put into spectator mode."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_open_trapdoors_item", "&bOpen Trapdoors");
        setMessage(config, sb, "settings_open_trapdoors_lore", Arrays.asList(
                "&7&oWhen right clicking iron (trap-)doors,",
                "&7&othey will be opened/closed."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_placeplants_item", "&bPlace Plants");
        setMessage(config, sb, "settings_placeplants_lore", Arrays.asList(
                "&7&oWhen enabled, you can place",
                "&7&oplants on every kind of block."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_scoreboard_item", "&bScoreboard");
        setMessage(config, sb, "settings_scoreboard_lore", Arrays.asList(
                "&7&oA scoreboard which provides",
                "&7&oyou with useful information."
        ));
        setMessage(config, sb, "settings_scoreboard_disabled_item", "&c&mScoreboard");
        setMessage(config, sb, "settings_scoreboard_disabled_lore", Arrays.asList(
                "&7&oThe scoreboard has been",
                "&7&odisabled in the config."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_slab_breaking_item", "&bSlab breaking");
        setMessage(config, sb, "settings_slab_breaking_lore", Arrays.asList(
                "&7&oWhen breaking double slabs,",
                "&7&oonly one half will be broken."
        ));
        addSpacer(sb, "");
        setMessage(config, sb, "settings_spawnteleport_item", "&bSpawn Teleport");
        setMessage(config, sb, "settings_spawnteleport_lore", Arrays.asList(
                "&7&oWhen enabled, you will be",
                "&7&oteleported to the spawn",
                "&7&oif it has been set."
        ));
        addSpacer(sb, "");
        addSpacer(sb, "# Change Design");
        setMessage(config, sb, "design_title", "&3» &8Change Design");
        setMessage(config, sb, "design_red", "Red");
        setMessage(config, sb, "design_orange", "Orange");
        setMessage(config, sb, "design_yellow", "Yellow");
        setMessage(config, sb, "design_pink", "Pink");
        setMessage(config, sb, "design_magenta", "Magenta");
        setMessage(config, sb, "design_purple", "Purple");
        setMessage(config, sb, "design_brown", "Brown");
        setMessage(config, sb, "design_lime", "Lime");
        setMessage(config, sb, "design_green", "Green");
        setMessage(config, sb, "design_blue", "Blue");
        setMessage(config, sb, "design_aqua", "Aqua");
        setMessage(config, sb, "design_light_blue", "Light Blue");
        setMessage(config, sb, "design_white", "White");
        setMessage(config, sb, "design_grey", "Grey");
        setMessage(config, sb, "design_dark_grey", "Dark Grey");
        setMessage(config, sb, "design_black", "Black");
        addSpacer(sb, "");
        addSpacer(sb, "# Speed");
        setMessage(config, sb, "speed_title", "&3» &8Speed");
        setMessage(config, sb, "speed_1", "&b1");
        setMessage(config, sb, "speed_2", "&b2");
        setMessage(config, sb, "speed_3", "&b3");
        setMessage(config, sb, "speed_4", "&b4");
        setMessage(config, sb, "speed_5", "&b5");
        addSpacer(sb, "");
        addSpacer(sb, "# Secret Blocks");
        setMessage(config, sb, "blocks_title", "&3» &8Secret Blocks");
        setMessage(config, sb, "blocks_full_oak_barch", "&bFull Oak Bark");
        setMessage(config, sb, "blocks_full_spruce_barch", "&bFull Spruce Bark");
        setMessage(config, sb, "blocks_full_birch_barch", "&bFull Birch Bark");
        setMessage(config, sb, "blocks_full_jungle_barch", "&bFull Jungle Bark");
        setMessage(config, sb, "blocks_full_acacia_barch", "&bFull Acacia Bark");
        setMessage(config, sb, "blocks_full_dark_oak_barch", "&bFull Dark Oak Bark");
        setMessage(config, sb, "blocks_red_mushroom", "&bRed Mushroom");
        setMessage(config, sb, "blocks_brown_mushroom", "&bBrown Mushroom");
        setMessage(config, sb, "blocks_full_mushroom_stem", "&bFull Mushroom Stem");
        setMessage(config, sb, "blocks_mushroom_stem", "&bMushroom Stem");
        setMessage(config, sb, "blocks_mushroom_block", "&bMushroom Block");
        setMessage(config, sb, "blocks_smooth_stone", "&bSmooth Stone");
        setMessage(config, sb, "blocks_double_stone_slab", "&bDouble Stone Slab");
        setMessage(config, sb, "blocks_smooth_sandstone", "&bSmooth Sandstone");
        setMessage(config, sb, "blocks_smooth_red_sandstone", "&bSmooth Red Sandstone");
        setMessage(config, sb, "blocks_powered_redstone_lamp", "&bPowered Redstone Lamp");
        setMessage(config, sb, "blocks_burning_furnace", "&bBurning Furnace");
        setMessage(config, sb, "blocks_piston_head", "&bPiston Head");
        setMessage(config, sb, "blocks_command_block", "&bCommand Block");
        setMessage(config, sb, "blocks_barrier", "&bBarrier");
        setMessage(config, sb, "blocks_invisible_item_frame", "&bInvisible Item Frame");
        setMessage(config, sb, "blocks_mob_spawner", "&bMob Spawner");
        setMessage(config, sb, "blocks_nether_portal", "&bNether Portal");
        setMessage(config, sb, "blocks_end_portal", "&bEnd Portal");
        setMessage(config, sb, "blocks_dragon_egg", "&bDragon Egg");
        setMessage(config, sb, "blocks_debug_stick", "&bDebug Stick");

        try (
                FileOutputStream fileStream = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fileStream, StandardCharsets.UTF_8)
        ) {
            writer.write(sb.toString());
        } catch (IOException e) {
            PLUGIN.getLogger().log(Level.SEVERE, "Could not write messages.yml file", e);
        }

        loadMessages();
    }

    private static void addSpacer(StringBuilder stringBuilder, String value) {
        stringBuilder.append(value).append("\n");
    }

    private static void setMessage(YamlConfiguration config, StringBuilder stringBuilder, String key, String defaultValue) {
        String value = config.getString(key, defaultValue);
        stringBuilder.append(key).append(": \"").append(value).append("\"").append("\n");
    }

    private static void setMessage(YamlConfiguration config, StringBuilder stringBuilder, String key, List<String> defaultValues) {
        List<String> values = config.getStringList(key);
        if (values.isEmpty()) {
            values = defaultValues;
        }

        stringBuilder.append(key).append(":\n");
        for (String value : values) {
            stringBuilder.append(" - \"").append(value).append("\"").append("\n");
        }
    }

    private static void loadMessages() {
        if (config == null) {
            throw new IllegalStateException("Messages have not been initialized yet");
        }

        ConfigurationSection messagesSection = config.getConfigurationSection("");
        if (messagesSection == null) {
            throw new IllegalStateException("Messages section is null in the configuration file");
        }

        messagesSection.getKeys(false).forEach(message -> {
            if (config.isList(message)) {
                MESSAGES.put(message, String.join("\n", config.getStringList(message)));
            } else {
                MESSAGES.put(message, Objects.requireNonNull(config.getString(message), "Message key '%s' is null".formatted(message)));
            }
        });
    }

    public static void reloadMessages() {
        MESSAGES.clear();
        createMessageFile();
    }

    private static void checkIfKeyPresent(String key) {
        if (!MESSAGES.containsKey(key)) {
            Bukkit.getConsoleSender().sendMessage(
                    ChatColor.RED + "[BuildSystem] Could not find message with key: " + key
            );
            createMessageFile();
        }
    }

    private static String getPrefix() {
        return MESSAGES.get("prefix");
    }

    public static void sendPermissionError(CommandSender sender) {
        Messages.sendMessage(sender, "no_permissions");
    }

    @SafeVarargs
    public static void sendMessage(CommandSender sender, String key, Entry<String, Object>... placeholders) {
        String message = getString(key, sender, placeholders);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    /**
     * Gets a message using the given key.
     *
     * @param key          The key of the message
     * @param sender       The sender to send the message to
     * @param placeholders The placeholders which are to be injected
     * @return The message uniquely identified by the given key
     */
    @SafeVarargs
    public static String getString(String key, CommandSender sender, Entry<String, Object>... placeholders) {
        checkIfKeyPresent(key);

        String message = MESSAGES.get(key).replace("%prefix%", getPrefix());
        String injectedPlaceholders = replacePlaceholders(message, placeholders);

        if (PLACEHOLDER_API_ENABLED && sender instanceof Player player) { // Sender is not a player if the message is sent to console
            injectedPlaceholders = PlaceholderAPI.setPlaceholders(player, injectedPlaceholders);
        }

        return ColorAPI.process(injectedPlaceholders);
    }

    /**
     * Gets a list of messages using the given key and injects the same placeholders into each line.
     *
     * @param key          The key of the message
     * @param player       The player to parse the placeholders against
     * @param placeholders The placeholders which are to be injected into all lines
     * @return A list of messages using the given key
     */
    @SafeVarargs
    public static List<String> getStringList(String key, @Nullable Player player, Entry<String, Object>... placeholders) {
        return getStringList(key, player, (line) -> placeholders);
    }

    /**
     * Gets a list of messages using the given key and injects placeholders into each line.
     *
     * @param key          The key of the message
     * @param player       The player to parse the placeholders against
     * @param placeholders The function which gets the placeholders to be injected into a given line
     * @return A list of messages using the given key
     */
    @Unmodifiable
    public static List<String> getStringList(String key, @Nullable Player player, Function<String, Entry<String, Object>[]> placeholders) {
        String message = MESSAGES.get(key).replace("%prefix%", getPrefix());
        return Arrays.stream(message.split("\n"))
                .map(line -> replacePlaceholders(line, placeholders.apply(line)))
                .map(line -> PLACEHOLDER_API_ENABLED && player != null
                        ? PlaceholderAPI.setPlaceholders(player, line)
                        : line
                )
                .map(ColorAPI::process)
                .toList();
    }

    /**
     * Gets the message key for the {@link BuildWorldStatus}'s display name.
     *
     * @return The type's display name message key
     */
    public static String getMessageKey(BuildWorldStatus status) {
        return switch (status) {
            case NOT_STARTED -> "status_not_started";
            case IN_PROGRESS -> "status_in_progress";
            case ALMOST_FINISHED -> "status_almost_finished";
            case FINISHED -> "status_finished";
            case ARCHIVE -> "status_archive";
            case HIDDEN -> "status_hidden";
        };
    }

    /**
     * Get the message key for the {@link BuildWorldType}'s display name.
     *
     * @return The type's display name message key, or {@code null} for {@link BuildWorldType#IMPORTED} and {@link BuildWorldType#UNKNOWN}
     */
    @Nullable
    public static String getMessageKey(BuildWorldType type) {
        return switch (type) {
            case NORMAL -> "type_normal";
            case FLAT -> "type_flat";
            case NETHER -> "type_nether";
            case END -> "type_end";
            case VOID -> "type_void";
            case TEMPLATE -> "type_template";
            case PRIVATE -> "type_private";
            case IMPORTED -> "type_imported";
            case CUSTOM -> "type_custom";
            case UNKNOWN -> null; // No message key for unknown worlds
        };
    }

    @SafeVarargs
    private static String replacePlaceholders(String query, Entry<String, Object>... placeholders) {
        if (placeholders.length == 0) {
            return query;
        }

        return Arrays.stream(placeholders)
                .map(entry -> (Function<String, String>) data -> data.replaceAll(entry.getKey(), String.valueOf(entry.getValue())))
                .reduce(Function.identity(), Function::andThen)
                .apply(query);
    }

    public static String formatDate(long millis) {
        return millis > 0
                ? new SimpleDateFormat(Config.Messages.dateFormat).format(millis)
                : "-";
    }
}