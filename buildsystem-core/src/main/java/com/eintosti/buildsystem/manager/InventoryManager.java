/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.manager;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.Titles;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.object.settings.Settings;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.Builder;
import com.eintosti.buildsystem.object.world.WorldStatus;
import com.eintosti.buildsystem.object.world.WorldType;
import com.eintosti.buildsystem.util.ConfigValues;
import com.eintosti.buildsystem.util.config.SetupConfig;
import com.eintosti.buildsystem.util.exception.UnexpectedEnumValueException;
import com.eintosti.buildsystem.util.external.ItemSkulls;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author einTosti
 */
public class InventoryManager {

    private final BuildSystem plugin;
    private final ConfigValues configValues;
    private final SetupConfig setupConfig;

    private final PlayerManager playerManager;

    private XMaterial normalCreateItem, flatCreateItem, netherCreateItem, endCreateItem, voidCreateItem, customCreateItem;
    private XMaterial normalDefaultItem, flatDefaultItem, netherDefaultItem, endDefaultItem, voidDefaultItem, importedDefaultItem;
    private XMaterial notStartedItem, inProgressItem, almostFinishedItem, finishedItem, archivedItem, hiddenItem;

    public InventoryManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        this.setupConfig = new SetupConfig(plugin);

        this.playerManager = plugin.getPlayerManager();
    }

    public boolean isNavigator(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != configValues.getNavigatorItem().parseMaterial()) {
            return false;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return false;
        }

        return itemMeta.getDisplayName().equals(plugin.getString("navigator_item"));
    }

    public boolean inventoryContainsNavigator(PlayerInventory playerInventory) {
        for (ItemStack itemStack : playerInventory.getContents()) {
            if (isNavigator(itemStack)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Integer> getNavigatorSlots(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        ArrayList<Integer> navigatorSlots = new ArrayList<>();

        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack currentItem = playerInventory.getItem(i);
            if (isNavigator(currentItem)) {
                navigatorSlots.add(i);
            }
        }

        return navigatorSlots;
    }

    public void replaceItem(Player player, String findItemName, XMaterial findItemType, ItemStack replaceItem) {
        PlayerInventory playerInventory = player.getInventory();
        int slot = -1;

        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack currentItem = playerInventory.getItem(i);
            if (currentItem != null && currentItem.getType() == findItemType.parseMaterial()) {
                ItemMeta itemMeta = currentItem.getItemMeta();
                if (itemMeta != null) {
                    if (itemMeta.getDisplayName().equals(findItemName)) {
                        slot = i;
                    }
                }
            }
        }

        if (slot != -1) {
            playerInventory.setItem(slot, replaceItem);
        } else {
            ItemStack slot8 = playerInventory.getItem(8);
            if (slot8 == null || slot8.getType() == XMaterial.AIR.parseMaterial()) {
                playerInventory.setItem(8, replaceItem);
            } else {
                playerInventory.addItem(replaceItem);
            }
        }
    }

    public ItemStack getItemStack(XMaterial material, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        if (itemStack == null) {
            itemStack = XMaterial.BEDROCK.parseItem();
        }
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        itemMeta.addItemFlags(ItemFlag.values());

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getItemStack(XMaterial material, String displayName, String... lore) {
        return getItemStack(material, displayName, Arrays.asList(lore));
    }

    public void addItemStack(Inventory inventory, int position, XMaterial material, String displayName, List<String> lore) {
        ItemStack itemStack = getItemStack(material, displayName, lore);
        inventory.setItem(position, itemStack);
    }

    public void addItemStack(Inventory inventory, int position, XMaterial material, String displayName, String... lore) {
        addItemStack(inventory, position, material, displayName, Arrays.asList(lore));
    }

    public void addGlassPane(BuildSystem plugin, Player player, Inventory inventory, int position) {
        addItemStack(inventory, position, getColouredGlassPane(plugin, player), " ");
    }

    public ItemStack getSkull(String displayName, String skullOwner, List<String> lore) {
        ItemStack skull = plugin.getSkullCache().getCachedSkull(skullOwner);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setDisplayName(displayName);
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);

        skull.setItemMeta(skullMeta);
        return skull;
    }

    public ItemStack getSkull(String displayName, String skullOwner, String... lore) {
        return getSkull(displayName, skullOwner, Arrays.asList(lore));
    }

    public void addSkull(Inventory inventory, int position, String displayName, String skullOwner, List<String> lore) {
        inventory.setItem(position, getSkull(displayName, skullOwner, lore));
    }

    public void addSkull(Inventory inventory, int position, String displayName, String skullOwner, String... lore) {
        addSkull(inventory, position, displayName, skullOwner, Arrays.asList(lore));
    }

    public ItemStack getUrlSkull(String displayName, String url, List<String> lore) {
        ItemStack skull = ItemSkulls.getSkull(url);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setDisplayName(displayName);
        skullMeta.setLore(lore);
        skullMeta.addItemFlags(ItemFlag.values());
        skull.setItemMeta(skullMeta);

        return skull;
    }

    public ItemStack getUrlSkull(String displayName, String url, String... lore) {
        return getUrlSkull(displayName, url, Arrays.asList(lore));
    }

    public void addUrlSkull(Inventory inventory, int position, String displayName, String url, List<String> lore) {
        inventory.setItem(position, getUrlSkull(displayName, url, lore));
    }

    public void addUrlSkull(Inventory inventory, int position, String displayName, String url, String... lore) {
        addUrlSkull(inventory, position, displayName, url, Arrays.asList(lore));
    }

    public boolean checkIfValidClick(InventoryClickEvent event, String titleKey) {
        if (!event.getView().getTitle().equals(plugin.getString(titleKey))) {
            return false;
        }

        ItemStack itemStack = event.getCurrentItem();
        if ((itemStack == null) || (itemStack.getType() == Material.AIR) || (!itemStack.hasItemMeta())) {
            return false;
        }

        event.setCancelled(true);
        return true;
    }

    public void addWorldItem(Player player, Inventory inventory, int position, BuildWorld buildWorld) {
        String worldName = buildWorld.getName();
        String displayName = plugin.getString("world_item_title").replace("%world%", worldName);

        if (buildWorld.getMaterial() == XMaterial.PLAYER_HEAD) {
            addSkull(inventory, position, displayName, worldName, getLore(player, buildWorld));
        } else {
            addItemStack(inventory, position, buildWorld.getMaterial(), displayName, getLore(player, buildWorld));
        }
    }

    public void manageInventoryClick(InventoryClickEvent event, Player player, ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return;
        }

        int slot = event.getSlot();
        ItemMeta itemMeta = itemStack.getItemMeta();
        String displayName = itemMeta.getDisplayName();

        if (slot == 22) {
            if (displayName.equals(plugin.getString("world_navigator_no_worlds"))
                    || displayName.equals(plugin.getString("archive_no_worlds"))
                    || displayName.equals(plugin.getString("private_no_worlds"))) {
                return;
            }
        }

        if (slot >= 9 && slot <= 44) {
            BuildWorld buildWorld = plugin.getWorldManager().getBuildWorld(getWorldName(displayName));
            manageWorldItemClick(event, player, itemMeta, buildWorld);
        }

        if (slot >= 45 && slot <= 53) {
            if (itemStack.getType() != XMaterial.PLAYER_HEAD.parseMaterial()) {
                XSound.BLOCK_CHEST_OPEN.play(player);
                plugin.getNavigatorInventory().openInventory(player);
            }
        }
    }

    private void manageWorldItemClick(InventoryClickEvent event, Player player, ItemMeta itemMeta, BuildWorld buildWorld) {
        if (event.isLeftClick() || !player.hasPermission("buildsystem.edit")) {
            performNonEditClick(player, itemMeta);
            return;
        }

        if (buildWorld.isLoaded()) {
            playerManager.getSelectedWorld().put(player.getUniqueId(), buildWorld);
            XSound.BLOCK_CHEST_OPEN.play(player);
            plugin.getEditInventory().openInventory(player, buildWorld);
        } else {
            player.closeInventory();
            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            String subtitle = plugin.getString("world_not_loaded");
            Titles.sendTitle(player, "", subtitle);
        }
    }

    private void performNonEditClick(Player player, ItemMeta itemMeta) {
        playerManager.closeNavigator(player);
        teleport(player, getWorldName(itemMeta.getDisplayName()));
    }

    private void teleport(Player player, String worldName) {
        WorldManager worldManager = plugin.getWorldManager();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            return;
        }
        worldManager.teleport(player, buildWorld);
    }

    private String getWorldName(String input) {
        String template = plugin.getString("world_item_title").replace("%world%", "");
        return StringUtils.difference(template, input);
    }

    public List<BuildWorld> sortWorlds(Player player, WorldManager worldManager, BuildSystem plugin) {
        List<BuildWorld> buildWorlds = new ArrayList<>(worldManager.getBuildWorlds());
        Settings settings = plugin.getSettingsManager().getSettings(player);
        switch (settings.getWorldSort()) {
            default: // NAME_A_TO_Z
                buildWorlds.sort(Comparator.comparing(worldOne -> worldOne.getName().toLowerCase()));
                break;
            case NAME_Z_TO_A:
                buildWorlds.sort(Comparator.comparing(worldOne -> worldOne.getName().toLowerCase()));
                Collections.reverse(buildWorlds);
                break;
            case PROJECT_A_TO_Z:
                buildWorlds.sort(Comparator.comparing(worldOne -> worldOne.getProject().toLowerCase()));
                break;
            case PROJECT_Z_TO_A:
                buildWorlds.sort(Comparator.comparing(worldOne -> worldOne.getProject().toLowerCase()));
                Collections.reverse(buildWorlds);
                break;
            case NEWEST_FIRST:
                buildWorlds.sort(new CreationComparator().reversed());
                break;
            case OLDEST_FIRST:
                buildWorlds.sort(new CreationComparator());
                break;
        }
        return buildWorlds;
    }

    private List<String> getLore(Player player, BuildWorld buildWorld) {
        List<String> messageList = player.hasPermission("buildsystem.edit") ? plugin.getStringList("world_item_lore_edit") :
                plugin.getStringList("world_item_lore_normal");
        List<String> lore = new ArrayList<>();
        for (String line : messageList) {
            String replace = line.replace("%project%", buildWorld.getProject())
                    .replace("%permission%", buildWorld.getPermission())
                    .replace("%status%", buildWorld.getStatusName())
                    .replace("%creator%", buildWorld.getCreator())
                    .replace("%creation%", buildWorld.getFormattedCreationDate());

            if (!line.contains("%builders%")) {
                lore.add(replace);
            } else {
                ArrayList<String> builders = formatBuilders(buildWorld);
                for (int i = 0; i < builders.size(); i++) {
                    String builderString = builders.get(i).trim();
                    if (builderString.isEmpty()) {
                        continue;
                    }

                    if (i == 0) {
                        builderString = line.replace("%builders%", builderString);
                    }
                    if (i == builders.size() - 1) {
                        builderString = builderString.substring(0, builderString.length() - 1);
                    }
                    lore.add(builderString);
                }
            }
        }
        return lore;
    }

    private ArrayList<String> formatBuilders(BuildWorld buildWorld) {
        String template = plugin.getString("world_item_builders_builder_template");
        ArrayList<Builder> builders = new ArrayList<>();

        if (configValues.isCreatorIsBuilder()) {
            if (buildWorld.getCreator() != null && !buildWorld.getCreator().equals("-")) {
                builders.add(new Builder(buildWorld.getCreatorId(), buildWorld.getCreator()));
            }
        }

        builders.addAll(buildWorld.getBuilders());

        ArrayList<String> builderNames = new ArrayList<>();
        if (builders.isEmpty()) {
            String string = template.replace("%builder%", "-").trim();
            builderNames.add(string);
        } else {
            String string = "";
            int buildersInLine = 0;
            for (Builder builder : builders) {
                buildersInLine++;
                string = string.concat(template.replace("%builder%", builder.getName()));
                if (buildersInLine == 3) {
                    builderNames.add(string.trim());
                    buildersInLine = 0;
                    string = "";
                }
            }
            builderNames.add(string.trim());
        }

        builderNames.removeIf(String::isEmpty);
        return builderNames;
    }

    public void fillMultiInvWithGlass(BuildSystem plugin, Inventory inventory, Player player, Map<UUID, Integer> invIndex, int numOfPages) {
        for (int i = 0; i <= 8; i++) {
            addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 46; i <= 48; i++) {
            addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 50; i <= 52; i++) {
            addGlassPane(plugin, player, inventory, i);
        }

        if (numOfPages > 1 && invIndex.get(player.getUniqueId()) > 0) {
            addUrlSkull(inventory, 45, plugin.getString("gui_previous_page"), "https://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
        } else {
            addGlassPane(plugin, player, inventory, 45);
        }

        if (numOfPages > 1 && invIndex.get(player.getUniqueId()) < (numOfPages - 1)) {
            addUrlSkull(inventory, 53, plugin.getString("gui_next_page"), "https://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
        } else {
            addGlassPane(plugin, player, inventory, 53);
        }
    }

    public XMaterial getColouredGlass(BuildSystem plugin, Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        Settings settings = settingsManager.getSettings(player);

        Optional<XMaterial> glass = XMaterial.matchXMaterial(settings.getGlassColor().name() + "_STAINED_GLASS");
        return glass.orElse(XMaterial.BLACK_STAINED_GLASS);
    }

    public XMaterial getColouredGlassPane(BuildSystem plugin, Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        Settings settings = settingsManager.getSettings(player);

        Optional<XMaterial> glass = XMaterial.matchXMaterial(settings.getGlassColor().name() + "_STAINED_GLASS_PANE");
        return glass.orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
    }

    public XMaterial getCreateItem(WorldType worldType) {
        XMaterial material;
        switch (worldType) {
            case NORMAL:
                material = this.normalCreateItem;
                if (material == null) {
                    material = XMaterial.OAK_LOG;
                }
                break;
            case FLAT:
                material = this.flatCreateItem;
                if (material == null) {
                    material = XMaterial.GRASS_BLOCK;
                }
                break;
            case NETHER:
                material = this.netherCreateItem;
                if (material == null) {
                    material = XMaterial.NETHERRACK;
                }
                break;
            case END:
                material = this.endCreateItem;
                if (material == null) {
                    material = XMaterial.END_STONE;
                }
                break;
            case VOID:
                material = this.voidCreateItem;
                if (material == null) {
                    material = XMaterial.GLASS;
                }
                break;
            case CUSTOM:
                material = this.customCreateItem;
                if (material == null) {
                    material = XMaterial.FILLED_MAP;
                }
                break;
            default:
                try {
                    throw new UnexpectedEnumValueException(worldType.name());
                } catch (UnexpectedEnumValueException e) {
                    e.printStackTrace();
                    return null;
                }
        }
        return material;
    }

    public void setCreateItem(WorldType worldType, XMaterial material) {
        switch (worldType) {
            case NORMAL:
                this.normalCreateItem = material;
                break;
            case FLAT:
                this.flatCreateItem = material;
                break;
            case NETHER:
                this.netherCreateItem = material;
                break;
            case END:
                this.endCreateItem = material;
                break;
            case VOID:
                this.voidCreateItem = material;
                break;
            case CUSTOM:
                this.customCreateItem = material;
                break;
            default:
                try {
                    throw new UnexpectedEnumValueException(worldType.name());
                } catch (UnexpectedEnumValueException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Nullable
    public XMaterial getDefaultItem(WorldType worldType) {
        XMaterial material;
        switch (worldType) {
            case NORMAL:
                material = this.normalDefaultItem;
                if (material == null) {
                    material = XMaterial.OAK_LOG;
                }
                break;
            case FLAT:
                material = this.flatDefaultItem;
                if (material == null) {
                    material = XMaterial.GRASS_BLOCK;
                }
                break;
            case NETHER:
                material = this.netherDefaultItem;
                if (material == null) {
                    material = XMaterial.NETHERRACK;
                }
                break;
            case END:
                material = this.endDefaultItem;
                if (material == null) {
                    material = XMaterial.END_STONE;
                }
                break;
            case VOID:
                material = this.voidDefaultItem;
                if (material == null) {
                    material = XMaterial.GLASS;
                }
                break;
            case IMPORTED:
                material = this.importedDefaultItem;
                if (material == null) {
                    material = XMaterial.FURNACE;
                }
                break;
            default:
                try {
                    throw new UnexpectedEnumValueException(worldType.name());
                } catch (UnexpectedEnumValueException e) {
                    e.printStackTrace();
                    return null;
                }
        }
        return material;
    }

    public void setDefaultItem(WorldType worldType, XMaterial material) {
        switch (worldType) {
            case NORMAL:
                this.normalDefaultItem = material;
                break;
            case FLAT:
                this.flatDefaultItem = material;
                break;
            case NETHER:
                this.netherDefaultItem = material;
                break;
            case END:
                this.endDefaultItem = material;
                break;
            case VOID:
                this.voidDefaultItem = material;
                break;
            case IMPORTED:
                this.importedDefaultItem = material;
                break;
            default:
                try {
                    throw new UnexpectedEnumValueException(worldType.name());
                } catch (UnexpectedEnumValueException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public XMaterial getStatusItem(WorldStatus worldStatus) {
        XMaterial material;
        switch (worldStatus) {
            case NOT_STARTED:
                material = this.notStartedItem;
                if (material == null) {
                    material = XMaterial.RED_DYE;
                }
                break;
            case IN_PROGRESS:
                material = this.inProgressItem;
                if (material == null) {
                    material = XMaterial.ORANGE_DYE;
                }
                break;
            case ALMOST_FINISHED:
                material = this.almostFinishedItem;
                if (material == null) {
                    material = XMaterial.LIME_DYE;
                }
                break;
            case FINISHED:
                material = this.finishedItem;
                if (material == null) {
                    material = XMaterial.GREEN_DYE;
                }
                break;
            case ARCHIVE:
                material = this.archivedItem;
                if (material == null) {
                    material = XMaterial.CYAN_DYE;
                }
                break;
            case HIDDEN:
                material = this.hiddenItem;
                if (material == null) {
                    material = XMaterial.BONE_MEAL;
                }
                break;
            default:
                try {
                    throw new UnexpectedEnumValueException(worldStatus.name());
                } catch (UnexpectedEnumValueException e) {
                    e.printStackTrace();
                    return null;
                }
        }
        return material;
    }

    public void setStatusItem(WorldStatus worldStatus, XMaterial material) {
        switch (worldStatus) {
            case NOT_STARTED:
                this.notStartedItem = material;
                break;
            case IN_PROGRESS:
                this.inProgressItem = material;
                break;
            case ALMOST_FINISHED:
                this.almostFinishedItem = material;
                break;
            case FINISHED:
                this.finishedItem = material;
                break;
            case ARCHIVE:
                this.archivedItem = material;
                break;
            case HIDDEN:
                this.hiddenItem = material;
                break;
            default:
                try {
                    throw new UnexpectedEnumValueException(worldStatus.name());
                } catch (UnexpectedEnumValueException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void save() {
        this.setupConfig.saveCreateItem(WorldType.NORMAL, getCreateItem(WorldType.NORMAL));
        this.setupConfig.saveCreateItem(WorldType.FLAT, getCreateItem(WorldType.FLAT));
        this.setupConfig.saveCreateItem(WorldType.NETHER, getCreateItem(WorldType.NETHER));
        this.setupConfig.saveCreateItem(WorldType.END, getCreateItem(WorldType.END));
        this.setupConfig.saveCreateItem(WorldType.VOID, getCreateItem(WorldType.VOID));

        this.setupConfig.saveDefaultItem(WorldType.NORMAL, getDefaultItem(WorldType.NORMAL));
        this.setupConfig.saveDefaultItem(WorldType.FLAT, getDefaultItem(WorldType.FLAT));
        this.setupConfig.saveDefaultItem(WorldType.NETHER, getDefaultItem(WorldType.NETHER));
        this.setupConfig.saveDefaultItem(WorldType.END, getDefaultItem(WorldType.END));
        this.setupConfig.saveDefaultItem(WorldType.VOID, getDefaultItem(WorldType.VOID));
        this.setupConfig.saveDefaultItem(WorldType.IMPORTED, getDefaultItem(WorldType.IMPORTED));

        this.setupConfig.saveStatusItem(WorldStatus.NOT_STARTED, getStatusItem(WorldStatus.NOT_STARTED));
        this.setupConfig.saveStatusItem(WorldStatus.IN_PROGRESS, getStatusItem(WorldStatus.IN_PROGRESS));
        this.setupConfig.saveStatusItem(WorldStatus.ALMOST_FINISHED, getStatusItem(WorldStatus.ALMOST_FINISHED));
        this.setupConfig.saveStatusItem(WorldStatus.FINISHED, getStatusItem(WorldStatus.FINISHED));
        this.setupConfig.saveStatusItem(WorldStatus.ARCHIVE, getStatusItem(WorldStatus.ARCHIVE));
        this.setupConfig.saveStatusItem(WorldStatus.HIDDEN, getStatusItem(WorldStatus.HIDDEN));
    }

    public void loadTypes() {
        FileConfiguration configuration = setupConfig.getFile();
        if (configuration == null) {
            return;
        }

        ConfigurationSection configurationSection = configuration.getConfigurationSection("setup.type");
        if (configurationSection == null) {
            return;
        }

        Set<String> worldTypes = configurationSection.getKeys(false);
        if (worldTypes.isEmpty()) {
            return;
        }

        for (String worldType : worldTypes) {
            String createMaterialString = configuration.getString("setup.type." + worldType + ".create");
            if (createMaterialString != null) {
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(createMaterialString);
                xMaterial.ifPresent(material -> setCreateItem(WorldType.valueOf(worldType.toUpperCase()), material));
            }

            String defaultMaterialString = configuration.getString("setup.type." + worldType + ".default");
            if (defaultMaterialString != null) {
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(defaultMaterialString);
                xMaterial.ifPresent(material -> setDefaultItem(WorldType.valueOf(worldType.toUpperCase()), material));
            }
        }
    }

    public void loadStatus() {
        FileConfiguration configuration = setupConfig.getFile();
        if (configuration == null) {
            return;
        }

        ConfigurationSection configurationSection = configuration.getConfigurationSection("setup.status");
        if (configurationSection == null) {
            return;
        }

        Set<String> worldStatus = configurationSection.getKeys(false);
        if (worldStatus.isEmpty()) {
            return;
        }

        for (String status : worldStatus) {
            String statusString = configuration.getString("setup.status." + status);
            if (statusString != null) {
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(statusString);
                xMaterial.ifPresent(material -> setStatusItem(WorldStatus.valueOf(status.toUpperCase()), material));
            }
        }
    }

    private static class CreationComparator implements Comparator<BuildWorld> {

        @Override
        public int compare(BuildWorld buildWorld1, BuildWorld buildWorld2) {
            return Long.compare(buildWorld1.getCreationDate(), buildWorld2.getCreationDate());
        }
    }
}
