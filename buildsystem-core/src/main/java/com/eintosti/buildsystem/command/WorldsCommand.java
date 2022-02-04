/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.Titles;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.Builder;
import com.eintosti.buildsystem.object.world.Generator;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import com.eintosti.buildsystem.util.external.UUIDFetcher;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class WorldsCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final PlayerManager playerManager;
    private final WorldManager worldManager;

    public WorldsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.playerManager = plugin.getPlayerManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("worlds").setExecutor(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            if (!player.hasPermission("buildsystem.gui")) {
                plugin.sendPermissionMessage(player);
                return true;
            }

            plugin.getNavigatorInventory().openInventory(player);
            XSound.BLOCK_CHEST_OPEN.play(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "addbuilder": {
                if (!player.hasPermission("buildsystem.addbuilder")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_addbuilder_unknown_world"));
                        return true;
                    }

                    if ((buildWorld.getCreatorId() == null || !buildWorld.getCreatorId().equals(player.getUniqueId()))
                            && !player.hasPermission("buildsystem.admin")) {
                        player.sendMessage(plugin.getString("worlds_addbuilder_not_creator"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    getAddBuilderInput(player, true);
                } else {
                    player.sendMessage(plugin.getString("worlds_addbuilder_usage"));
                }
                break;
            }

            case "builders": {
                if (!player.hasPermission("buildsystem.builders")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_builders_unknown_world"));
                        return true;
                    }
                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    player.openInventory(plugin.getBuilderInventory().getInventory(buildWorld, player));
                } else {
                    player.sendMessage(plugin.getString("worlds_builders_usage"));
                }
                break;
            }

            case "delete": {
                if (!player.hasPermission("buildsystem.delete")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_delete_unknown_world"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    plugin.getDeleteInventory().openInventory(player, buildWorld);
                } else {
                    player.sendMessage(plugin.getString("worlds_delete_usage"));
                }
                break;
            }

            case "edit": {
                if (!player.hasPermission("buildsystem.edit")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_edit_unknown_world"));
                        return true;
                    }

                    if (buildWorld.isLoaded()) {
                        playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                        plugin.getEditInventory().openInventory(player, buildWorld);
                    } else {
                        XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
                        String subtitle = plugin.getString("world_not_loaded");
                        Titles.sendTitle(player, "", subtitle);
                    }
                } else {
                    player.sendMessage(plugin.getString("worlds_edit_usage"));
                }
                break;
            }

            case "help": {
                if (args.length == 1) {
                    sendHelpMessage(player, 1);
                } else if (args.length == 2) {
                    try {
                        int page = Integer.parseInt(args[1]);
                        sendHelpMessage(player, page);
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getString("worlds_help_invalid_page"));
                    }
                } else {
                    player.sendMessage(plugin.getString("worlds_help_usage"));
                }
                break;
            }

            case "import": {
                if (!player.hasPermission("buildsystem.import")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length >= 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld != null) {
                        player.sendMessage(plugin.getString("worlds_import_world_is_imported"));
                        return true;
                    }

                    File worldFolder = new File(Bukkit.getWorldContainer(), args[1]);
                    File levelFile = new File(worldFolder.getAbsolutePath() + File.separator + "level.dat");
                    if (!worldFolder.isDirectory() || !levelFile.exists()) {
                        player.sendMessage(plugin.getString("worlds_import_unknown_world"));
                        return true;
                    }

                    if (args.length == 2) {
                        worldManager.importWorld(player, args[1], Generator.VOID);
                    } else if (args.length == 4) {
                        if (!args[2].equalsIgnoreCase("-g")) {
                            player.sendMessage(plugin.getString("worlds_import_usage"));
                            return true;
                        }

                        Generator generator;
                        boolean customGenerator = false;
                        switch (args[3].toLowerCase()) {
                            case "normal":
                                generator = Generator.NORMAL;
                                break;
                            case "flat":
                                generator = Generator.FLAT;
                                break;
                            case "void":
                                generator = Generator.VOID;
                                break;
                            default:
                                generator = Generator.CUSTOM;
                                customGenerator = true;
                                break;
                        }

                        if (!customGenerator) {
                            worldManager.importWorld(player, args[1], generator);
                        } else {
                            worldManager.importWorld(player, args[1], generator, args[3]);
                        }
                    } else {
                        player.sendMessage(plugin.getString("worlds_import_usage"));
                    }
                } else {
                    player.sendMessage(plugin.getString("worlds_import_usage"));
                }
                break;
            }

            case "importall": {
                if (!player.hasPermission("buildsystem.import.all")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 1) {
                    File worldContainer = Bukkit.getWorldContainer();
                    String[] directories = worldContainer.list((dir, name) -> {
                        File worldFolder = new File(dir, name);
                        if (!worldFolder.isDirectory()) {
                            return false;
                        }

                        File levelFile = new File(dir + File.separator + name + File.separator + "level.dat");
                        if (!levelFile.exists()) {
                            return false;
                        }

                        BuildWorld buildWorld = worldManager.getBuildWorld(name);
                        return buildWorld == null;
                    });

                    if (directories == null || directories.length == 0) {
                        player.sendMessage(plugin.getString("worlds_importall_no_worlds"));
                        return true;
                    }
                    worldManager.importWorlds(player, directories);
                } else {
                    player.sendMessage(plugin.getString("worlds_importall_usage"));
                }
                break;
            }

            case "info": {
                if (!player.hasPermission("buildsystem.info")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
                if (args.length == 2) {
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_info_unknown_world"));
                        return true;
                    }
                    buildWorld = worldManager.getBuildWorld(args[1]);
                } else if (args.length > 2) {
                    player.sendMessage(plugin.getString("worlds_info_usage"));
                }

                sendInfoMessage(player, buildWorld);
                break;
            }

            case "item": {
                player.getInventory().addItem(inventoryManager.getItemStack(plugin.getConfigValues().getNavigatorItem(), plugin.getString("navigator_item")));
                player.sendMessage(plugin.getString("worlds_item_receive"));
                break;
            }

            case "removebuilder": {
                if (!player.hasPermission("buildsystem.removebuilder")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_removebuilder_unknown_world"));
                        return true;
                    }

                    if ((buildWorld.getCreatorId() == null || !buildWorld.getCreatorId().equals(player.getUniqueId())) && !player.hasPermission("buildsystem.admin")) {
                        player.sendMessage(plugin.getString("worlds_removebuilder_not_creator"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    getRemoveBuilderInput(player, true);
                } else {
                    player.sendMessage(plugin.getString("worlds_removebuilder_usage"));
                }
                break;
            }

            case "rename": {
                if (!player.hasPermission("buildsystem.rename")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_rename_unknown_world"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    getRenameInput(player);
                } else {
                    player.sendMessage(plugin.getString("worlds_rename_usage"));
                }
                break;
            }

            case "setcreator": {
                if (!player.hasPermission("buildsystem.setcreator")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_setcreator_unknown_world"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    getCreatorInput(player);
                } else {
                    player.sendMessage(plugin.getString("worlds_setcreator_usage"));
                }
                break;
            }

            case "setitem": {
                if (!player.hasPermission("buildsystem.setitem")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_setitem_unknown_world"));
                        return true;
                    }

                    ItemStack itemStack = player.getItemInHand();
                    if (itemStack.getType().equals(Material.AIR)) {
                        player.sendMessage(plugin.getString("worlds_setitem_hand_empty"));
                        return true;
                    }

                    buildWorld.setMaterial(XMaterial.matchXMaterial(itemStack));
                    player.sendMessage(plugin.getString("worlds_setitem_set").replace("%world%", buildWorld.getName()));
                } else {
                    player.sendMessage(plugin.getString("worlds_setitem_usage"));
                }
                break;
            }

            case "setpermission": {
                if (!player.hasPermission("buildsystem.setpermission")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_setpermission_unknown_world"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    getPermissionInput(player, true);
                } else {
                    player.sendMessage(plugin.getString("worlds_setpermission_usage"));
                }
                break;
            }

            case "setproject": {
                if (!player.hasPermission("buildsystem.setproject")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_setproject_unknown_world"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    getProjectInput(player, true);
                } else {
                    player.sendMessage(plugin.getString("worlds_setproject_usage"));
                }
                break;
            }

            case "setstatus": {
                if (!player.hasPermission("buildsystem.setstatus")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_setstatus_unknown_world"));
                        return true;
                    }

                    playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
                    plugin.getStatusInventory().openInventory(player);
                } else {
                    player.sendMessage(plugin.getString("worlds_setstatus_usage"));
                }
                break;
            }

            case "setspawn": {
                if (!player.hasPermission("buildsystem.setspawn")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
                if (buildWorld == null) {
                    player.sendMessage(plugin.getString("worlds_setspawn_world_not_imported"));
                    return true;
                }

                buildWorld.setCustomSpawn(player.getLocation());
                player.sendMessage(plugin.getString("worlds_setspawn_world_spawn_set").replace("%world%", buildWorld.getName()));
                break;
            }

            case "removespawn": {
                if (!player.hasPermission("buildsystem.removespawn")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
                if (buildWorld == null) {
                    player.sendMessage(plugin.getString("worlds_removespawn_world_not_imported"));
                    return true;
                }

                buildWorld.removeCustomSpawn();
                player.sendMessage(plugin.getString("worlds_removespawn_world_spawn_removed").replace("%world%", buildWorld.getName()));
                break;
            }

            case "tp": {
                if (!player.hasPermission("buildsystem.worldtp")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_tp_unknown_world"));
                        return true;
                    }

                    World bukkitWorld = Bukkit.getServer().getWorld(args[1]);
                    if (buildWorld.isLoaded() && bukkitWorld == null) {
                        player.sendMessage(plugin.getString("worlds_tp_unknown_world"));
                        return true;
                    }

                    if (player.hasPermission(buildWorld.getPermission()) || buildWorld.getPermission().equalsIgnoreCase("-")) {
                        worldManager.teleport(player, buildWorld);
                    } else {
                        player.sendMessage(plugin.getString("worlds_tp_entry_forbidden"));
                    }
                    return true;
                } else {
                    player.sendMessage(plugin.getString("worlds_tp_usage"));
                }
                break;
            }

            case "unimport": {
                if (!player.hasPermission("buildsystem.unimport")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                if (args.length == 2) {
                    BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
                    if (buildWorld == null) {
                        player.sendMessage(plugin.getString("worlds_unimport_unknown_world"));
                        return true;
                    }

                    worldManager.unimportWorld(buildWorld);
                    player.sendMessage(plugin.getString("worlds_unimport_finished").replace("%world%", buildWorld.getName()));
                } else {
                    player.sendMessage(plugin.getString("worlds_unimport_usage"));
                }
                break;
            }

            default: {
                player.sendMessage(plugin.getString("worlds_unknown_command"));
                break;
            }
        }
        return true;
    }

    private void sendHelpMessage(Player player, int page) {
        final int maxPages = 2;
        if (page > maxPages) {
            page = maxPages;
        }

        TextComponent line1 = new TextComponent("§7§m----------------------------------------------------\n");
        TextComponent line2 = new TextComponent(plugin.getString("worlds_help_title_with_page")
                .replace("%page%", String.valueOf(page))
                .replace("%max%", String.valueOf(maxPages))
                .concat("\n"));
        TextComponent line3 = new TextComponent("§7 \n");

        TextComponent line4;
        TextComponent line5;
        TextComponent line6;
        TextComponent line7;
        TextComponent line8;
        TextComponent line9;
        TextComponent line10;
        TextComponent line11;
        TextComponent line12;
        TextComponent line13;

        switch (page) {
            case 1:
                line4 = createComponent("/worlds help <page>", " §8» " + plugin.getString("worlds_help_help"), "/worlds help", "-");
                line5 = createComponent("/worlds info", " §8» " + plugin.getString("worlds_help_info"), "/worlds info", "buildsystem.info");
                line6 = createComponent("/worlds item", " §8» " + plugin.getString("worlds_help_item"), "/worlds item", "-");
                line7 = createComponent("/worlds tp <world>", " §8» " + plugin.getString("worlds_help_tp"), "/worlds tp ", "buildsystem.worldtp");
                line8 = createComponent("/worlds edit <world>", " §8» " + plugin.getString("worlds_help_edit"), "/worlds edit ", "buildsystem.edit");
                line9 = createComponent("/worlds addBuilder <world>", " §8» " + plugin.getString("worlds_help_addbuilder"), "/worlds addBuilder ", "buildsystem.addbuilder");
                line10 = createComponent("/worlds removeBuilder <world>", " §8» " + plugin.getString("worlds_help_removebuilder"), "/worlds removeBuilder ", "buildsystem.removebuilder");
                line11 = createComponent("/worlds builders <world>", " §8» " + plugin.getString("worlds_help_builders"), "/worlds builders ", "buildsystem.builders");
                line12 = createComponent("/worlds rename <world>", " §8» " + plugin.getString("worlds_help_rename"), "/worlds rename ", "buildsystem.rename");
                line13 = createComponent("/worlds setItem <world>", " §8» " + plugin.getString("worlds_help_setitem"), "/worlds setItem ", "buildsystem.setitem");
                break;

            default:
                line4 = createComponent("/worlds setCreator <world>", " §8» " + plugin.getString("worlds_help_setcreator"), "/worlds setCreator ", "buildsystem.setcreator");
                line5 = createComponent("/worlds setProject <world>", " §8» " + plugin.getString("worlds_help_setproject"), "/worlds setProject ", "buildsystem.setproject");
                line6 = createComponent("/worlds setPermission <world>", " §8» " + plugin.getString("worlds_help_setpermission"), "/worlds setPermission ", "buildsystem.setpermission");
                line7 = createComponent("/worlds setStatus <world>", " §8» " + plugin.getString("worlds_help_setstatus"), "/worlds setStatus ", "buildsystem.setstatus");
                line8 = createComponent("/worlds setSpawn", " §8» " + plugin.getString("worlds_help_setspawn"), "/worlds setSpawn", "buildsystem.setspawn");
                line9 = createComponent("/worlds removeSpawn", " §8» " + plugin.getString("worlds_help_removespawn"), "/worlds removeSpawn", "buildsystem.removespawn");
                line10 = createComponent("/worlds delete <world>", " §8» " + plugin.getString("worlds_help_delete"), "/worlds delete ", "buildsystem.delete");
                line11 = createComponent("/worlds import <world>", " §8» " + plugin.getString("worlds_help_import"), "/worlds import ", "buildsystem.import");
                line12 = createComponent("/worlds importAll", " §8» " + plugin.getString("worlds_help_importall"), "/worlds importAll", "buildsystem.import.all");
                line13 = createComponent("/worlds unimport", " §8» " + plugin.getString("worlds_help_unimport"), "/worlds unimport", "buildsystem.unimport");
                break;
        }

        TextComponent line14 = new TextComponent("§7§m----------------------------------------------------");

        player.spigot().sendMessage(line1, line2, line3, line4, line5, line6, line7, line8, line9, line10, line11, line12, line13, line14);
    }

    private TextComponent createComponent(String command, String text, String suggest, String permission) {
        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(text + "\n");

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(plugin.getString("worlds_help_permission").replace("%permission%", permission)).create()));
        commandComponent.addExtra(textComponent);
        return commandComponent;
    }

    //TODO: Print information about the custom generator?
    private void sendInfoMessage(Player player, BuildWorld buildWorld) {
        List<String> infoMessage = new ArrayList<>();
        for (String line : plugin.getStringList("world_info")) {
            String replace = line
                    .replace("%world%", buildWorld.getName())
                    .replace("%creator%", buildWorld.getCreator())
                    .replace("%type%", buildWorld.getTypeName())
                    .replace("%private%", String.valueOf(buildWorld.isPrivate()))
                    .replace("%builders_enabled%", String.valueOf(buildWorld.isBuilders()))
                    .replace("%builders%", buildWorld.getBuildersInfo())
                    .replace("%block_breaking%", String.valueOf(buildWorld.isBlockBreaking()))
                    .replace("%block_placement%", String.valueOf(buildWorld.isBlockPlacement()))
                    .replace("%item%", buildWorld.getMaterial().name())
                    .replace("%status%", buildWorld.getStatusName())
                    .replace("%project%", buildWorld.getProject())
                    .replace("%permission%", buildWorld.getPermission())
                    .replace("%time%", buildWorld.getWorldTime())
                    .replace("%creation%", buildWorld.getFormattedCreationDate())
                    .replace("%date%", buildWorld.getFormattedCreationDate())
                    .replace("%physics%", String.valueOf(buildWorld.isPhysics()))
                    .replace("%explosions%", String.valueOf(buildWorld.isExplosions()))
                    .replace("%mobai%", String.valueOf(buildWorld.isMobAI()))
                    .replace("%custom_spawn%", getCustomSpawn(buildWorld));
            infoMessage.add(replace);
        }
        StringBuilder stringBuilder = new StringBuilder();
        infoMessage.forEach(line -> stringBuilder.append(line).append("\n"));
        player.sendMessage(stringBuilder.toString());
    }

    private String getCustomSpawn(BuildWorld buildWorld) {
        if (buildWorld.getCustomSpawn() == null) {
            return "-";
        }

        String[] spawnString = buildWorld.getCustomSpawn().split(";");
        Location location = new Location(
                Bukkit.getWorld(buildWorld.getName()),
                Double.parseDouble(spawnString[0]),
                Double.parseDouble(spawnString[1]),
                Double.parseDouble(spawnString[2]),
                Float.parseFloat(spawnString[3]),
                Float.parseFloat(spawnString[4])
        );

        return "XYZ: " + round(location.getX()) + " / " + round(location.getY()) + " / " + round(location.getZ());
    }

    private double round(double value) {
        int scale = (int) Math.pow(10, 2);
        return (double) Math.round(value * scale) / scale;
    }

    public void getAddBuilderInput(Player player, boolean closeInventory) {
        BuildWorld buildWorld = playerManager.getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_addbuilder_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_player_name", input -> {
            String builderName = input.trim();
            Player builderPlayer = Bukkit.getPlayer(builderName);
            Builder builder;
            UUID builderId;

            if (builderPlayer == null) {
                builderId = UUIDFetcher.getUUID(builderName);
                if (builderId == null) {
                    player.sendMessage(plugin.getString("worlds_addbuilder_player_not_found"));
                    player.closeInventory();
                    return;
                }
                builder = new Builder(builderId, builderName);
            } else {
                builder = new Builder(builderPlayer);
                builderId = builderPlayer.getUniqueId();
            }

            if (buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(builderId)) {
                player.sendMessage(plugin.getString("worlds_addbuilder_already_creator"));
                player.closeInventory();
                return;
            }

            if (buildWorld.isBuilder(builderId)) {
                player.sendMessage(plugin.getString("worlds_addbuilder_already_added"));
                player.closeInventory();
                return;
            }

            buildWorld.addBuilder(builder);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_addbuilder_added").replace("%builder%", builderName));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getBuilderInventory().getInventory(buildWorld, player));
            }
        });
    }

    private void getCreatorInput(Player player) {
        BuildWorld buildWorld = playerManager.getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setcreator_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_creator", input -> {
            String creator = input.trim();
            buildWorld.setCreator(creator);
            if (!creator.equalsIgnoreCase("-")) {
                buildWorld.setCreatorId(UUIDFetcher.getUUID(creator));
            } else {
                buildWorld.setCreatorId(null);
            }

            playerManager.forceUpdateSidebar(buildWorld);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_setcreator_set").replace("%world%", buildWorld.getName()));
            player.closeInventory();
        });
    }

    public void getProjectInput(Player player, boolean closeInventory) {
        BuildWorld buildWorld = playerManager.getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setproject_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_project", input -> {
            buildWorld.setProject(input.trim());
            playerManager.forceUpdateSidebar(buildWorld);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_setproject_set").replace("%world%", buildWorld.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getEditInventory().getInventory(player, buildWorld));
            }
        });
    }

    public void getPermissionInput(Player player, boolean closeInventory) {
        BuildWorld buildWorld = playerManager.getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setpermission_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_permission", input -> {
            buildWorld.setPermission(input.trim());
            playerManager.forceUpdateSidebar(buildWorld);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_setpermission_set").replace("%world%", buildWorld.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getEditInventory().getInventory(player, buildWorld));
            }
        });
    }

    public void getRemoveBuilderInput(Player player, boolean closeInventory) {
        BuildWorld buildWorld = playerManager.getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_removebuilder_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_player_name", input -> {
            String builderName = input.trim();
            Player builderPlayer = Bukkit.getPlayer(builderName);
            UUID builderId;

            if (builderPlayer == null) {
                builderId = UUIDFetcher.getUUID(builderName);
                if (builderId == null) {
                    player.sendMessage(plugin.getString("worlds_removebuilder_player_not_found"));
                    player.closeInventory();
                    return;
                }
            } else {
                builderId = builderPlayer.getUniqueId();
            }

            if (buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(builderId)) {
                player.sendMessage(plugin.getString("worlds_removebuilder_not_yourself"));
                player.closeInventory();
                return;
            }

            if (!buildWorld.isBuilder(builderId)) {
                player.sendMessage(plugin.getString("worlds_removebuilder_not_builder"));
                player.closeInventory();
                return;
            }

            buildWorld.removeBuilder(builderId);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_removebuilder_removed").replace("%builder%", builderName));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getBuilderInventory().getInventory(buildWorld, player));
            }
        });
    }

    private void getRenameInput(Player player) {
        BuildWorld buildWorld = playerManager.getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_rename_unknown_world"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_name", input -> {
            player.closeInventory();
            worldManager.renameWorld(player, buildWorld, input.trim());
            playerManager.getSelectedWorld().remove(player.getUniqueId());
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.closeInventory();
        });
    }
}