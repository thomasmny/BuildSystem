package de.eintosti.buildsystem.navigator.inventory;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.navigator.settings.WorldDisplay;
import de.eintosti.buildsystem.navigator.settings.WorldFilter;
import de.eintosti.buildsystem.navigator.settings.WorldSort;
import de.eintosti.buildsystem.player.settings.Settings;
import de.eintosti.buildsystem.settings.SettingsManager;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.util.PaginatedInventory;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.display.Displayable;
import de.eintosti.buildsystem.world.display.Folder;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Inventory that displays filterable and sortable displayable items. Implements functional programming principles for state management and data transformation.
 */
public class FilteredDisplayablesInventory extends PaginatedInventory implements Listener {

    private static final int MAX_ITEMS = 36;
    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT = 44;

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;
    private final SettingsManager settingsManager;
    private final Logger logger;

    private final String inventoryName;
    private final String noItemsText;
    private final Stack<Folder> folderStack;
    private final List<Displayable> displayables;

    public FilteredDisplayablesInventory(BuildSystem plugin, String inventoryName, String noItemsText) {
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
        this.settingsManager = plugin.getSettingsManager();
        this.logger = plugin.getLogger();

        this.inventoryName = inventoryName;
        this.noItemsText = noItemsText;
        this.folderStack = new Stack<>();
        this.displayables = new ArrayList<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    protected Inventory createInventory(Player player) {
        try {
            Inventory inventory = Bukkit.createInventory(null, 54, Messages.getString(inventoryName, player));

            int numOfPages = calculateNumPages();
            inventoryUtils.fillMultiInvWithGlass(plugin, inventory, player, getInvIndex(player), numOfPages);

            addNavigationItems(inventory, player);

            return inventory;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create inventory", e);
            return Bukkit.createInventory(null, 54, "Error");
        }
    }

    private int calculateNumPages() {
        return (displayables.size() / MAX_ITEMS) + (displayables.size() % MAX_ITEMS == 0 ? 0 : 1);
    }

    private void addNavigationItems(Inventory inventory, Player player) {
        addWorldSortItem(inventory, player);
        addWorldFilterItem(inventory, player);
        addBackButton(inventory, player);
    }

    private void addBackButton(Inventory inventory, Player player) {
        if (!folderStack.isEmpty()) {
            inventoryUtils.addItemStack(inventory, 48, XMaterial.ARROW, Messages.getString("back_button", player));
        }
    }

    private void addWorldSortItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        WorldSort worldSort = settings.getWorldDisplay().getWorldSort();
        inventoryUtils.addItemStack(
                inventory,
                45,
                XMaterial.BOOK,
                Messages.getString("world_sort_title", player),
                worldSort.getItemLore(player)
        );
    }

    private void addWorldFilterItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        WorldFilter worldFilter = settings.getWorldDisplay().getWorldFilter();

        List<String> lore = new ArrayList<>();
        lore.add(Messages.getString(worldFilter.getMode().getLoreKey(), player));
        lore.addAll(Messages.getStringList("world_filter_lore", player));

        inventoryUtils.addItemStack(
                inventory,
                46,
                XMaterial.HOPPER,
                Messages.getString("world_filter_title", player),
                lore
        );
    }

    public void openInventory(Player player) {
        try {
            addDisplayables(player);
            player.openInventory(getInventory(player));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to open inventory for player: " + player.getName(), e);
            player.sendMessage(Messages.getString("error_opening_inventory", player));
        }
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = createInventory(player);

        if (displayables.isEmpty()) {
            inventoryUtils.addItemStack(
                    inventory,
                    22,
                    XMaterial.BARRIER,
                    Messages.getString(noItemsText, player)
            );
            return inventory;
        }

        addDisplayablesToInventory(inventory, player);
        return inventory;
    }

    private void addDisplayablesToInventory(Inventory inventory, Player player) {
        int page = getInvIndex(player);
        int startIndex = page * MAX_ITEMS;
        int endIndex = Math.min(startIndex + MAX_ITEMS, displayables.size());

        IntStream.range(startIndex, endIndex)
                .forEach(i -> {
                    int slot = FIRST_SLOT + (i - startIndex);
                    if (slot <= LAST_SLOT) {
                        Displayable displayable = displayables.get(i);
                        inventory.setItem(slot, displayable.asItemStack(player));
                    }
                });
    }

    private void addDisplayables(Player player) {
        displayables.clear();
        if (folderStack.isEmpty()) {
            displayables.addAll(plugin.getWorldManager().getRootDisplayables());
        } else {
            displayables.addAll(folderStack.peek().getContents());
        }

        // Apply filtering and sorting
        Settings settings = settingsManager.getSettings(player);
        WorldDisplay worldDisplay = settings.getWorldDisplay();
        WorldFilter filter = worldDisplay.getWorldFilter();
        WorldSort sort = worldDisplay.getWorldSort();

        // Filter displayables based on the filter mode and text
        displayables.removeIf(displayable -> {
            String name = displayable.getName().toLowerCase();
            String filterText = filter.getText().toLowerCase();
            switch (filter.getMode()) {
                case CONTAINS:
                    return !name.contains(filterText);
                case STARTS_WITH:
                    return !name.startsWith(filterText);
                case MATCHES:
                    return !name.matches(filterText);
                default: // NONE
                    return false;
            }
        });

        // Sort displayables based on the sort mode
        displayables.sort((d1, d2) -> {
            String name1 = d1.getName().toLowerCase();
            String name2 = d2.getName().toLowerCase();
            return sort == WorldSort.NAME_A_TO_Z ? name1.compareTo(name2) : name2.compareTo(name1);
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtils.checkIfValidClick(event, inventoryName)) {
            return;
        }

        try {
            handleInventoryClick(event);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling inventory click", e);
            Player player = (Player) event.getWhoClicked();
            player.sendMessage(Messages.getString("error_processing_click", player));
        }
    }

    private void handleInventoryClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);
        WorldDisplay worldDisplay = settings.getWorldDisplay();

        switch (event.getSlot()) {
            case 45:
                handleSortClick(event, worldDisplay);
                break;
            case 46:
                handleFilterClick(event, player, worldDisplay);
                break;
            case 48:
                handleBackClick(player);
                break;
            case 52:
                handlePreviousPageClick(player);
                break;
            case 53:
                handleNextPageClick(player);
                break;
            default:
                handleDisplayableClick(event, player);
                break;
        }
    }

    private void handleSortClick(InventoryClickEvent event, WorldDisplay worldDisplay) {
        WorldSort newSort = event.isLeftClick()
                ? worldDisplay.getWorldSort().getNext()
                : worldDisplay.getWorldSort().getPrevious();
        worldDisplay.setWorldSort(newSort);
        openInventory((Player) event.getWhoClicked());
    }

    private void handleFilterClick(InventoryClickEvent event, Player player, WorldDisplay worldDisplay) {
        WorldFilter worldFilter = worldDisplay.getWorldFilter();
        WorldFilter.Mode currentMode = worldFilter.getMode();

        if (event.isShiftClick()) {
            worldFilter.setMode(WorldFilter.Mode.NONE);
            worldFilter.setText("");
            setInvIndex(player, 0);
            openInventory(player);
        } else if (event.isLeftClick()) {
            new PlayerChatInput(plugin, player, "world_filter_title", input -> {
                worldFilter.setText(input.replace("\"", ""));
                setInvIndex(player, 0);
                openInventory(player);
            });
        } else if (event.isRightClick()) {
            worldFilter.setMode(currentMode.getNext());
            setInvIndex(player, 0);
            openInventory(player);
        }
    }

    private void handleBackClick(Player player) {
        if (!folderStack.isEmpty()) {
            exitFolder();
            openInventory(player);
        }
    }

    private void handlePreviousPageClick(Player player) {
        if (decrementInv(player, displayables.size(), MAX_ITEMS)) {
            openInventory(player);
        }
    }

    private void handleNextPageClick(Player player) {
        if (incrementInv(player, displayables.size(), MAX_ITEMS)) {
            openInventory(player);
        }
    }

    private void handleDisplayableClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot >= FIRST_SLOT && slot < LAST_SLOT) {
            int index = (getInvIndex(player) * MAX_ITEMS) + (slot - FIRST_SLOT);
            if (index < displayables.size()) {
                Displayable displayable = displayables.get(index);
                if (displayable instanceof Folder) {
                    enterFolder((Folder) displayable);
                    openInventory(player);
                } else if (displayable instanceof BuildWorld) {
                    handleBuildWorldClick(player, (BuildWorld) displayable, event);
                }
            }
        }
    }

    private void handleBuildWorldClick(Player player, BuildWorld buildWorld, InventoryClickEvent event) {
        if (event.isLeftClick()) {
            plugin.getWorldManager().teleport(player, buildWorld);
        } else if (event.isRightClick()) {
            plugin.getWorldManager().openWorldSettingsInventory(player, buildWorld);
        }
    }

    public void enterFolder(Folder folder) {
        folderStack.push(folder);
    }

    public void exitFolder() {
        if (!folderStack.isEmpty()) {
            folderStack.pop();
        }
    }
} 