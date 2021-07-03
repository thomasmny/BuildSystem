package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.Builder;
import de.eintosti.buildsystem.object.world.Generator;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.util.external.PlayerChatInput;
import de.eintosti.buildsystem.util.external.UUIDFetcher;
import de.eintosti.buildsystem.util.external.xseries.Titles;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
    private final WorldManager worldManager;

    public WorldsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("worlds").setExecutor(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_addbuilder_unknown_world"));
                        return true;
                    }
                    if ((world.getCreatorId() == null || !world.getCreatorId().equals(player.getUniqueId()))
                            && !player.hasPermission("buildsystem.admin")) {
                        player.sendMessage(plugin.getString("worlds_addbuilder_not_creator"));
                        return true;
                    }

                    plugin.selectedWorld.put(player.getUniqueId(), world);
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
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_builders_unknown_world"));
                        return true;
                    }
                    plugin.selectedWorld.put(player.getUniqueId(), world);
                    player.openInventory(plugin.getBuilderInventory().getInventory(world, player));
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
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_delete_unknown_world"));
                        return true;
                    }
                    plugin.selectedWorld.put(player.getUniqueId(), world);
                    plugin.getDeleteInventory().openInventory(player, world);
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
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_edit_unknown_world"));
                        return true;
                    }

                    if (world.isLoaded()) {
                        plugin.selectedWorld.put(player.getUniqueId(), world);
                        plugin.getEditInventory().openInventory(player, world);
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
                sendHelpMessage(player);
                break;
            }

            case "import": {
                if (!player.hasPermission("buildsystem.import")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                if (args.length >= 2) {
                    World world = worldManager.getWorld(args[1]);
                    if (world != null) {
                        player.sendMessage(plugin.getString("worlds_import_world_is_imported"));
                        return true;
                    }

                    File worldFolder = new File(plugin.getServer().getWorldContainer(), args[1]);
                    File levelFile = new File(worldFolder.getAbsolutePath() + File.separator + "level.dat");
                    if (!worldFolder.isDirectory() || !levelFile.exists()) {
                        player.sendMessage(plugin.getString("worlds_import_unknown_world"));
                        return true;
                    }

                    if (args.length == 2) {
                        worldManager.importWorld(args[1], player, Generator.VOID);
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
                            worldManager.importWorld(args[1], player, generator);
                        } else {
                            worldManager.importWorld(args[1], player, generator, args[3]);
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
                    File worldContainer = plugin.getServer().getWorldContainer();
                    String[] directories = worldContainer.list((dir, name) -> {
                        File worldFolder = new File(dir, name);
                        if (!worldFolder.isDirectory()) return false;

                        File levelFile = new File(dir + File.separator + name + File.separator + "level.dat");
                        if (!levelFile.exists()) return false;

                        World world = worldManager.getWorld(name);
                        return world == null;
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
                World world = worldManager.getWorld(player.getWorld().getName());
                if (args.length == 2) {
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_info_unknown_world"));
                        return true;
                    }
                    world = worldManager.getWorld(args[1]);
                } else if (args.length > 2) {
                    player.sendMessage(plugin.getString("worlds_info_usage"));
                }
                sendInfoMessage(player, world);
                break;
            }

            case "item": {
                player.getInventory().addItem(inventoryManager.getItemStack(plugin.getNavigatorItem(), plugin.getString("navigator_item")));
                player.sendMessage(plugin.getString("worlds_item_receive"));
                break;
            }

            case "removebuilder": {
                if (!player.hasPermission("buildsystem.removebuilder")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                if (args.length == 2) {
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_removebuilder_unknown_world"));
                        return true;
                    }
                    if ((world.getCreatorId() == null || !world.getCreatorId().equals(player.getUniqueId()))
                            && !player.hasPermission("buildsystem.admin")) {
                        player.sendMessage(plugin.getString("worlds_removebuilder_not_creator"));
                        return true;
                    }

                    plugin.selectedWorld.put(player.getUniqueId(), world);
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
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_rename_unknown_world"));
                        return true;
                    }
                    plugin.selectedWorld.put(player.getUniqueId(), world);
                    getRenameInput(player);
                } else {
                    player.sendMessage(plugin.getString("worlds_rename_usage"));
                }
                break;
            }

            case "setitem": {
                if (!player.hasPermission("buildsystem.setitem")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                if (args.length == 2) {
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_setitem_unknown_world"));
                        return true;
                    }

                    ItemStack itemStack = player.getItemInHand();
                    if (itemStack.getType().equals(Material.AIR)) {
                        player.sendMessage(plugin.getString("worlds_setitem_hand_empty"));
                        return true;
                    }
                    world.setMaterial(XMaterial.matchXMaterial(itemStack));
                    player.sendMessage(plugin.getString("worlds_setitem_set").replace("%world%", world.getName()));
                } else {
                    player.sendMessage(plugin.getString("worlds_setitem_usage"));
                }
                break;
            }

            case "setcreator": {
                if (!player.hasPermission("buildsystem.setcreator")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                if (args.length == 2) {
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_setcreator_unknown_world"));
                        return true;
                    }
                    plugin.selectedWorld.put(player.getUniqueId(), world);
                    getCreatorInput(player);
                } else {
                    player.sendMessage(plugin.getString("worlds_setcreator_usage"));
                }
                break;
            }

            case "setproject": {
                if (!player.hasPermission("buildsystem.setproject")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                if (args.length == 2) {
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_setproject_unknown_world"));
                        return true;
                    }
                    plugin.selectedWorld.put(player.getUniqueId(), world);
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
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_setstatus_unknown_world"));
                        return true;
                    }
                    plugin.selectedWorld.put(player.getUniqueId(), world);
                    plugin.getStatusInventory().openInventory(player);
                } else {
                    player.sendMessage(plugin.getString("worlds_setstatus_usage"));
                }
                break;
            }

            case "setpermission": {
                if (!player.hasPermission("buildsystem.setpermission")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                if (args.length == 2) {
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_setpermission_unknown_world"));
                        return true;
                    }
                    plugin.selectedWorld.put(player.getUniqueId(), world);
                    getPermissionInput(player, true);
                } else {
                    player.sendMessage(plugin.getString("worlds_setpermission_usage"));
                }
                break;
            }

            case "setspawn": {
                if (!player.hasPermission("buildsystem.setspawn")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                World world = worldManager.getWorld(player.getWorld().getName());
                if (world == null) {
                    player.sendMessage(plugin.getString("worlds_setspawn_world_not_imported"));
                    return true;
                }
                world.setCustomSpawn(player.getLocation());
                player.sendMessage(plugin.getString("worlds_setspawn_world_spawn_set").replace("%world%", world.getName()));
                break;
            }

            case "tp": {
                if (!player.hasPermission("buildsystem.worldtp")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }
                if (args.length == 2) {
                    World world = worldManager.getWorld(args[1]);
                    if (world == null) {
                        player.sendMessage(plugin.getString("worlds_tp_unknown_world"));
                        return true;
                    }

                    org.bukkit.World bukkitWorld = Bukkit.getServer().getWorld(args[1]);
                    if (world.isLoaded() && bukkitWorld == null) {
                        player.sendMessage(plugin.getString("worlds_tp_unknown_world"));
                        return true;
                    }

                    if (player.hasPermission(world.getPermission()) || world.getPermission().equalsIgnoreCase("-")) {
                        worldManager.teleport(player, world);
                    } else {
                        player.sendMessage(plugin.getString("worlds_tp_entry_forbidden"));
                    }
                    return true;
                } else {
                    player.sendMessage(plugin.getString("worlds_tp_usage"));
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

    private void sendHelpMessage(Player player) {
        TextComponent line1 = new TextComponent("§7§m----------------------------------------------------\n");
        TextComponent line2 = new TextComponent(plugin.getString("worlds_help_title") + "\n");
        TextComponent line3 = new TextComponent("§7 \n");
        TextComponent line4 = createComponent(player, "/worlds info", " §8» " + plugin.getString("worlds_help_info"), "/worlds info", "buildsystem.info");
        TextComponent line5 = createComponent(player, "/worlds item", " §8» " + plugin.getString("worlds_help_item"), "/worlds item", "-");
        TextComponent line6 = createComponent(player, "/worlds tp <world>", " §8» " + plugin.getString("worlds_help_tp"), "/worlds tp ", "buildsystem.worldtp");
        TextComponent line7 = createComponent(player, "/worlds edit <world>", " §8» " + plugin.getString("worlds_help_edit"), "/worlds edit ", "buildsystem.edit");
        TextComponent line8 = createComponent(player, "/worlds addBuilder <world>", " §8» " + plugin.getString("worlds_help_addbuilder"), "/worlds addBuilder ", "buildsystem.addbuilder");
        TextComponent line9 = createComponent(player, "/worlds removeBuilder <world>", " §8» " + plugin.getString("worlds_help_removebuilder"), "/worlds removeBuilder ", "buildsystem.removebuilder");
        TextComponent line10 = createComponent(player, "/worlds builders <world>", " §8» " + plugin.getString("worlds_help_builders"), "/worlds builders ", "buildsystem.builders");
        TextComponent line11 = createComponent(player, "/worlds rename <world>", " §8» " + plugin.getString("worlds_help_rename"), "/worlds rename ", "buildsystem.rename");
        TextComponent line12 = createComponent(player, "/worlds setItem <world>", " §8» " + plugin.getString("worlds_help_setitem"), "/worlds setItem ", "buildsystem.setitem");
        TextComponent line13 = createComponent(player, "/worlds setCreator <world>", " §8» " + plugin.getString("worlds_help_setcreator"), "/worlds setCreator ", "buildsystem.setcreator");
        TextComponent line14 = createComponent(player, "/worlds setProject <world>", " §8» " + plugin.getString("worlds_help_setproject"), "/worlds setProject ", "buildsystem.setproject");
        TextComponent line15 = createComponent(player, "/worlds setPermission <world>", " §8» " + plugin.getString("worlds_help_setpermission"), "/worlds setPermission ", "buildsystem.setpermission");
        TextComponent line16 = createComponent(player, "/worlds setStatus <world>", " §8» " + plugin.getString("worlds_help_setstatus"), "/worlds setStatus ", "buildsystem.setstatus");
        TextComponent line17 = createComponent(player, "/worlds setSpawn", " §8» " + plugin.getString("worlds_help_setspawn"), "/worlds setSpawn", "buildsystem.setspawn");
        TextComponent line18 = createComponent(player, "/worlds delete <world>", " §8» " + plugin.getString("worlds_help_delete"), "/worlds delete ", "buildsystem.delete");
        TextComponent line19 = createComponent(player, "/worlds import <world>", " §8» " + plugin.getString("worlds_help_import"), "/worlds import ", "buildsystem.import");
        TextComponent line20 = createComponent(player, "/worlds importall", " §8» " + plugin.getString("worlds_help_importall"), "/worlds importall", "buildsystem.import.all");
        TextComponent line21 = new TextComponent("§7§m----------------------------------------------------");

        line20.addExtra(line21);
        line19.addExtra(line20);
        line18.addExtra(line19);
        line17.addExtra(line18);
        line16.addExtra(line17);
        line15.addExtra(line16);
        line14.addExtra(line15);
        line13.addExtra(line14);
        line12.addExtra(line13);
        line11.addExtra(line12);
        line10.addExtra(line11);
        line9.addExtra(line10);
        line8.addExtra(line9);
        line7.addExtra(line8);
        line6.addExtra(line7);
        line5.addExtra(line6);
        line4.addExtra(line5);
        line3.addExtra(line4);
        line2.addExtra(line3);
        line1.addExtra(line2);

        player.spigot().sendMessage(line1);
    }

    private TextComponent createComponent(Player player, String command, String text, String suggest, String permission) {
        if (!player.hasPermission(permission) && !permission.equals("-")) {
            return new TextComponent("");
        }

        TextComponent lineComponent = new TextComponent("§8 - ");
        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(text + "\n");

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(plugin.getString("worlds_help_permission").replace("%permission%", permission)).create()));

        commandComponent.addExtra(textComponent);
        lineComponent.addExtra(commandComponent);
        return lineComponent;
    }

    private void sendInfoMessage(Player player, World world) {
        List<String> infoMessage = new ArrayList<>();
        for (String line : plugin.getStringList("world_info")) {
            String replace = line
                    .replace("%world%", world.getName())
                    .replace("%creator%", world.getCreator())
                    .replace("%type%", world.getTypeName())
                    .replace("%private%", String.valueOf(world.isPrivate()))
                    .replace("%builders_enabled%", String.valueOf(world.isBuilders()))
                    .replace("%builders%", plugin.getBuilders(world))
                    .replace("%block_breaking%", String.valueOf(world.isBlockBreaking()))
                    .replace("%block_placement%", String.valueOf(world.isBlockPlacement()))
                    .replace("%item%", world.getMaterial().name())
                    .replace("%status%", world.getStatusName())
                    .replace("%project%", world.getProject())
                    .replace("%permission%", world.getPermission())
                    .replace("%time%", plugin.getWorldTime(world))
                    .replace("%creation%", plugin.formatDate(world.getCreationDate()))
                    .replace("%date%", plugin.formatDate(world.getCreationDate()))
                    .replace("%physics%", String.valueOf(world.isPhysics()))
                    .replace("%explosions%", String.valueOf(world.isExplosions()))
                    .replace("%mobai%", String.valueOf(world.isMobAI()))
                    .replace("%customspawn%", getCustomSpawn(world))
                    .replace("%custom_spawn%", getCustomSpawn(world));
            infoMessage.add(replace);
        }
        StringBuilder stringBuilder = new StringBuilder();
        infoMessage.forEach(line -> stringBuilder.append(line).append("\n"));
        player.sendMessage(stringBuilder.toString());
    }

    private String getCustomSpawn(World world) {
        if (world.getCustomSpawn() == null) {
            return "-";
        }

        String[] spawnString = world.getCustomSpawn().split(";");
        Location location = new Location(
                Bukkit.getWorld(world.getName()),
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
        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
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

            if (world.getCreatorId() != null && world.getCreatorId().equals(builderId)) {
                player.sendMessage(plugin.getString("worlds_addbuilder_already_creator"));
                player.closeInventory();
                return;
            }

            if (world.isBuilder(builderId)) {
                player.sendMessage(plugin.getString("worlds_addbuilder_already_added"));
                player.closeInventory();
                return;
            }

            world.addBuilder(builder);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_addbuilder_added").replace("%builder%", builderName));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getBuilderInventory().getInventory(world, player));
            }
        });
    }

    private void getCreatorInput(Player player) {
        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setcreator_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_creator", input -> {
            String creator = input.trim();
            world.setCreator(creator);
            if (!creator.equalsIgnoreCase("-")) {
                world.setCreatorId(UUIDFetcher.getUUID(creator));
            } else {
                world.setCreatorId(null);
            }

            plugin.forceUpdateSidebar(world);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_setcreator_set").replace("%world%", world.getName()));
            player.closeInventory();
        });
    }

    public void getProjectInput(Player player, boolean closeInventory) {
        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setproject_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_project", input -> {
            world.setProject(input.trim());
            plugin.forceUpdateSidebar(world);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_setproject_set").replace("%world%", world.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getEditInventory().getInventory(player, world));
            }
        });
    }

    public void getPermissionInput(Player player, boolean closeInventory) {
        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setpermission_error"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_permission", input -> {
            world.setPermission(input.trim());
            plugin.forceUpdateSidebar(world);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_setpermission_set").replace("%world%", world.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getEditInventory().getInventory(player, world));
            }
        });
    }

    public void getRemoveBuilderInput(Player player, boolean closeInventory) {
        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
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

            if (world.getCreatorId() != null && world.getCreatorId().equals(builderId)) {
                player.sendMessage(plugin.getString("worlds_removebuilder_not_yourself"));
                player.closeInventory();
                return;
            }

            if (!world.isBuilder(builderId)) {
                player.sendMessage(plugin.getString("worlds_removebuilder_not_builder"));
                player.closeInventory();
                return;
            }

            world.removeBuilder(builderId);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_removebuilder_removed").replace("%builder%", builderName));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getBuilderInventory().getInventory(world, player));
            }
        });
    }

    private void getRenameInput(Player player) {
        World world = plugin.selectedWorld.get(player.getUniqueId());
        if (world == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_rename_unknown_world"));
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_name", input -> {
            player.closeInventory();
            worldManager.renameWorld(player, world, input.trim());
            plugin.selectedWorld.remove(player.getUniqueId());
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.closeInventory();
        });
    }
}
