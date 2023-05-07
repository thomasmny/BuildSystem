/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.navigator.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.navigator.settings.WorldDisplay;
import com.eintosti.buildsystem.navigator.settings.WorldFilter;
import com.eintosti.buildsystem.navigator.settings.WorldSort;
import com.eintosti.buildsystem.settings.Settings;
import com.eintosti.buildsystem.settings.SettingsManager;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.util.PaginatedInventory;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.data.WorldStatus;
import com.eintosti.buildsystem.world.modification.CreateInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.AbstractMap;
import java.util.Set;

/**
 * @author einTosti
 */
public class FilteredWorldsInventory extends PaginatedInventory implements Listener {

    private static final int MAX_WORLDS = 36;

    private final BuildSystem plugin;
    private final InventoryUtil inventoryUtil;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    private final String inventoryName;
    private final String noWorldsText;
    private final Visibility visibility;
    private final Set<WorldStatus> validStatus;

    public FilteredWorldsInventory(BuildSystem plugin, String inventoryName, String noWorldsText, Visibility visibility, Set<WorldStatus> validStatus) {
        this.plugin = plugin;
        this.inventoryUtil = plugin.getInventoryUtil();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        this.inventoryName = inventoryName;
        this.noWorldsText = noWorldsText;
        this.visibility = visibility;
        this.validStatus = validStatus;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    protected Inventory createInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Messages.getString(inventoryName));

        int numOfPages = (numOfWorlds(player) / MAX_WORLDS) + (numOfWorlds(player) % MAX_WORLDS == 0 ? 0 : 1);
        inventoryUtil.fillMultiInvWithGlass(plugin, inventory, player, getInvIndex(player), numOfPages);

        addWorldSortItem(inventory, player);
        addWorldFilterItem(inventory, player);

        inventoryUtil.addUrlSkull(inventory, 52, Messages.getString("gui_previous_page"), "https://textures.minecraft.net/texture/86971dd881dbaf4fd6bcaa93614493c612f869641ed59d1c9363a3666a5fa6");
        inventoryUtil.addUrlSkull(inventory, 53, Messages.getString("gui_next_page"), "https://textures.minecraft.net/texture/f32ca66056b72863e98f7f32bd7d94c7a0d796af691c9ac3a9136331352288f9");

        return inventory;
    }

    /**
     * Gets the amount of worlds that are to be displayed in the inventory.
     *
     * @param player The player to show the inventory to
     * @return The amount of worlds
     */
    private int numOfWorlds(Player player) {
        return (int) worldManager.getBuildWorlds().stream()
                .filter(buildWorld -> isValidWorld(player, buildWorld))
                .count();
    }

    /**
     * Gets the visibility of the worlds that will be displayed.
     *
     * @return The visibility of the worlds that will be displayed
     */
    protected Visibility getVisibility() {
        return visibility;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private Inventory getInventory(Player player) {
        addWorlds(player);
        return inventories[getInvIndex(player)];
    }

    private void addWorlds(Player player) {
        int numWorlds = numOfWorlds(player);
        int numInventories = (numWorlds % MAX_WORLDS == 0 ? numWorlds : numWorlds + 1) != 0 ? (numWorlds % MAX_WORLDS == 0 ? numWorlds : numWorlds + 1) : 1;

        inventories = new Inventory[numInventories];
        Inventory inventory = createInventory(player);

        int index = 0;
        inventories[index] = inventory;
        if (numWorlds == 0) {
            inventoryUtil.addUrlSkull(inventory, 22, Messages.getString(noWorldsText), "2e3f50ba62cbda3ecf5479b62fedebd61d76589771cc19286bf2745cd71e47c6");
            return;
        }

        int columnWorld = 9, maxColumnWorld = 44;
        for (BuildWorld buildWorld : inventoryUtil.getDisplayOrder(worldManager, plugin.getSettingsManager().getSettings(player))) {
            if (isValidWorld(player, buildWorld)) {
                inventoryUtil.addWorldItem(player, inventory, columnWorld++, buildWorld);
            }

            if (columnWorld > maxColumnWorld) {
                columnWorld = 9;
                inventory = createInventory(player);
                inventories[++index] = inventory;
            }
        }
    }

    private void addWorldSortItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        WorldSort worldSort = settings.getWorldDisplay().getWorldSort();
        inventoryUtil.addItemStack(inventory, 45, XMaterial.CHEST, Messages.getString("world_sort_title"), worldSort.getItemLore());
    }

    private void addWorldFilterItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        WorldFilter worldFilter = settings.getWorldDisplay().getWorldFilter();

        inventoryUtil.addItemStack(inventory, 46, XMaterial.HOPPER, Messages.getString("world_filter_title"),
                Messages.getString(worldFilter.getMode().getLoreKey(), new AbstractMap.SimpleEntry<>("%text%", worldFilter.getText()))
        );
    }

    /**
     * Gets if the world should be shown to the player in the navigator.
     * <p>
     * The following logic is applied to determine the above:
     * <ul>
     *   <li>Does the player have the admin bypass permission?</li>
     *   <li>Is the player the creator of the world?</li>
     *   <li>Has the player been added as a builder?</li>
     *   <li>Does the player have the permission to see the world?</li>
     * </ul>
     *
     * @param player     The player who the world will be shown to
     * @param buildWorld The world to show
     * @return {@code true} if the world should be shown to the player in the navigator, {@code false} otherwise
     */
    private boolean isValidWorld(Player player, BuildWorld buildWorld) {
        if (!worldManager.isCorrectVisibility(buildWorld, visibility)) {
            return false;
        }

        if (!validStatus.contains(buildWorld.getStatus())) {
            return false;
        }

        if (player.hasPermission(BuildSystem.ADMIN_PERMISSION)) {
            return true;
        }

        if (buildWorld.isCreator(player) || buildWorld.isBuilder(player)) {
            return true;
        }

        String worldPermission = buildWorld.getPermission();
        if (!worldPermission.equalsIgnoreCase("-") && !player.hasPermission(worldPermission)) {
            return false;
        }

        return Bukkit.getWorld(buildWorld.getName()) != null || !buildWorld.isLoaded();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtil.checkIfValidClick(event, inventoryName)) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);
        WorldDisplay worldDisplay = settings.getWorldDisplay();

        switch (event.getSlot()) {
            case 45:
                WorldSort newSort = event.isLeftClick() ? worldDisplay.getWorldSort().getNext() : worldDisplay.getWorldSort().getPrevious();
                worldDisplay.setWorldSort(newSort);
                openInventory(player);
                return;
            case 46:
                if (event.isLeftClick()) {
                    WorldFilter filter = worldDisplay.getWorldFilter();
                    if (filter.getMode() != WorldFilter.Mode.ALL) {
                        new PlayerChatInput(plugin, player, "world_filter_title", input -> {
                            filter.setText(input);
                            openInventory(player);
                        });
                    }
                } else if (event.isRightClick()) {
                    WorldFilter.Mode currentMode = worldDisplay.getWorldFilter().getMode();
                    WorldFilter.Mode newMode = event.isLeftClick() ? currentMode.getNext() : currentMode.getPrevious();
                    worldDisplay.getWorldFilter().setMode(newMode);
                    openInventory(player);
                }
                return;
            case 49:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                plugin.getCreateInventory().openInventory(player, CreateInventory.Page.PREDEFINED, visibility);
                return;
            case 52:
                if (decrementInv(player, numOfWorlds(player), MAX_WORLDS)) {
                    openInventory(player);
                }
                return;
            case 53:
                if (incrementInv(player, numOfWorlds(player), MAX_WORLDS)) {
                    openInventory(player);
                }
                return;
        }

        inventoryUtil.manageInventoryClick(event, player, itemStack);
    }

    public enum Visibility {
        PRIVATE,
        PUBLIC,
        IGNORE;

        public static Visibility matchVisibility(boolean isPrivateWorld) {
            return isPrivateWorld ? PRIVATE : PUBLIC;
        }
    }
}