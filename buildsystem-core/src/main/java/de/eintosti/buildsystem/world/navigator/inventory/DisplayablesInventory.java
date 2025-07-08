/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.world.navigator.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Displayable.DisplayableType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldDisplay;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldFilter;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldFilter.Mode;
import de.eintosti.buildsystem.api.world.navigator.settings.WorldSort;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter.WorldsArgument;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.inventory.BuildSystemHolder;
import de.eintosti.buildsystem.util.inventory.InventoryManager;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.util.inventory.PaginatedInventory;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.CreateInventory;
import de.eintosti.buildsystem.world.creation.CreateInventory.Page;
import de.eintosti.buildsystem.world.modification.EditInventory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An abstract inventory class for displaying {@link Displayable} objects such as {@link Folder}s and {@link BuildWorld}s. This class is designed to be instantiated once per player
 * to manage their specific inventory view.
 */
@NullMarked
public abstract class DisplayablesInventory extends PaginatedInventory {

    private static final int MAX_WORLDS_PER_PAGE = 36;
    private static final int FIRST_WORD_SLOT = 9;
    private static final int LAST_WORLD_SLOT = 44;

    private static final String PREVIOUS_PAGE_SKULL_PROFILE = "86971dd881dbaf4fd6bcaa93614493c612f869641ed59d1c9363a3666a5fa6";
    private static final String NEXT_PAGE_SKULL_PROFILE = "f32ca66056b72863e98f7f32bd7d94c7a0d796af691c9ac3a9136331352288f9";
    private static final String NO_WORLDS_SKULL_PROFILE = "2e3f50ba62cbda3ecf5479b62fedebd61d76589771cc19286bf2745cd71e47c6";

    protected final BuildSystemPlugin plugin;
    protected final InventoryManager inventoryManager;
    protected final PlayerServiceImpl playerService;
    protected final SettingsManager settingsManager;
    protected final FolderStorageImpl folderStorage;
    protected final WorldStorageImpl worldStorage;

    protected final Player player;
    protected final NavigatorCategory category;
    protected final Visibility requiredVisibility;
    protected final Set<BuildWorldStatus> validStatuses;
    private final String inventoryTitle;
    @Nullable
    private final String noWorldsMessage;

    @Nullable
    private List<Displayable> cachedDisplayables;
    @Nullable
    private Inventory[] generatedInventories;

    /**
     * Constructs a new {@link DisplayablesInventory} for a specific player.
     *
     * @param plugin             The plugin instance
     * @param player             The player for whom this inventory is created
     * @param category           The category of the inventory, used for organizing folders
     * @param inventoryTitle     The inventory's title
     * @param noWorldsMessage    The "no worlds" message
     * @param requiredVisibility The required visibility for worlds to be displayed
     * @param validStatuses      The set of valid statuses for worlds to be displayed
     */
    protected DisplayablesInventory(
            BuildSystemPlugin plugin,
            Player player,
            NavigatorCategory category,
            String inventoryTitle,
            @Nullable String noWorldsMessage,
            Visibility requiredVisibility,
            Set<BuildWorldStatus> validStatuses
    ) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.playerService = plugin.getPlayerService();
        this.settingsManager = plugin.getSettingsManager();
        WorldServiceImpl worldService = plugin.getWorldService();
        this.folderStorage = worldService.getFolderStorage();
        this.worldStorage = worldService.getWorldStorage();

        this.player = player;
        this.category = category;
        this.inventoryTitle = inventoryTitle;
        this.noWorldsMessage = noWorldsMessage;
        this.requiredVisibility = requiredVisibility;
        this.validStatuses = validStatuses;
        generatedInventories = new Inventory[0];
    }

    /**
     * Opens the inventory for the associated player.
     */
    public void openInventory() {
        if (this.generatedInventories.length == 0) {
            initializeInventories();
        }

        Inventory inventory = generatedInventories[getInvIndex(player)];
        this.inventoryManager.registerInventoryHandler(inventory, this);
        player.openInventory(inventory);
    }

    /**
     * Initializes all inventory pages based on the current player settings and world data.
     */
    private void initializeInventories() {
        this.cachedDisplayables = collectDisplayables();
        int numDisplayableObjects = this.cachedDisplayables.size();

        int numPages = calculateNumPages(numDisplayableObjects, MAX_WORLDS_PER_PAGE);
        this.generatedInventories = new Inventory[numPages];

        for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
            Inventory currentPage = createBaseInventoryPage(this.inventoryTitle);
            this.generatedInventories[pageIndex] = currentPage;

            if (numDisplayableObjects == 0 && this.noWorldsMessage != null) {
                currentPage.setItem(22, InventoryUtils.createSkull(this.noWorldsMessage, Profileable.detect(NO_WORLDS_SKULL_PROFILE)));
                continue;
            }

            int startItemIndex = pageIndex * MAX_WORLDS_PER_PAGE;
            int endItemIndex = Math.min(startItemIndex + MAX_WORLDS_PER_PAGE, numDisplayableObjects);

            int currentSlot = FIRST_WORD_SLOT;
            for (int i = startItemIndex; i < endItemIndex; i++) {
                if (currentSlot > LAST_WORLD_SLOT) {
                    break;
                }
                this.cachedDisplayables.get(i).addToInventory(currentPage, currentSlot++, player);
            }
        }
    }

    /**
     * Creates a single inventory page with common navigation and setting items.
     *
     * @return A new inventory instance.
     */
    protected Inventory createBaseInventoryPage(String inventoryTitle) {
        Inventory inventory = new DisplayablesInventoryHolder(inventoryTitle).getInventory();
        InventoryUtils.fillWithGlass(inventory, player);

        addWorldSortItem(inventory);
        addWorldFilterItem(inventory);

        inventory.setItem(52, InventoryUtils.createSkull(Messages.getString("gui_previous_page", player), Profileable.detect(PREVIOUS_PAGE_SKULL_PROFILE)));
        inventory.setItem(53, InventoryUtils.createSkull(Messages.getString("gui_next_page", player), Profileable.detect(NEXT_PAGE_SKULL_PROFILE)));

        return inventory;
    }

    /**
     * Collects all {@link Displayable} items (i.e. {@link Folder}s and {@link BuildWorld}s) that should be shown to the player.
     * <p>
     * The method applies the following logic:
     * <ul>
     *  <li>Fetches all {@link BuildWorld}s visible to the player based on world access rights and filter settings.</li>
     *  <li>Sorts the filtered worlds according to the player's selected sort order.</li>
     *  <li>If a world is assigned to a {@link Folder}, it is excluded from display and the folder is shown instead.</li>
     *  <li>If a world is not assigned to any folder, it is displayed directly.</li>
     *  <li>All folders are listed first, followed by all standalone worlds.</li>
     * </ul>
     * This ensures that:
     * <ul>
     *  <li>Worlds are never shown twice (both directly and via their folder).</li>
     *  <li>Folder grouping takes display priority over individual worlds.</li>
     * </ul>
     *
     * @return A list of {@link Displayable} items to be presented in the UI, sorted with folders first.
     */
    protected List<Displayable> collectDisplayables() {
        WorldDisplay worldDisplay = settingsManager.getSettings(player).getWorldDisplay();

        Collection<Folder> folders = collectFolders();
        List<BuildWorld> standaloneWorlds = filterWorlds(collectWorlds(), worldDisplay).stream()
                .filter(buildWorld -> !buildWorld.isAssignedToFolder())
                .toList();

        List<Displayable> displayables = new ArrayList<>();
        displayables.addAll(folders);
        displayables.addAll(standaloneWorlds);
        displayables.sort(worldDisplay.getWorldSort().getComparator());
        return displayables;
    }

    /**
     * Collects all {@link Folder}s that belong to the specified category and do not have a parent folder.
     *
     * @return A collection of root folders in the specified category
     */
    @Unmodifiable
    protected Collection<Folder> collectFolders() {
        return folderStorage.getFolders().stream()
                .filter(folder -> folder.getCategory() == this.category)
                .filter(folder -> !folder.hasParent())
                .filter(folder -> folder.canView(this.player))
                .toList();
    }

    /**
     * Collects all potential {@link BuildWorld}s that be displayed to the player before filtering them based on visibility and status.
     */
    protected Collection<BuildWorld> collectWorlds() {
        return worldStorage.getBuildWorlds();
    }

    /**
     * Filters the given collection of {@link BuildWorld}s based on their visibility and status, as well as the player's world display settings.
     *
     * @param buildWorlds  The collection of build worlds to filter
     * @param worldDisplay The world display settings for the player
     * @return A collection of filtered and sorted {@link BuildWorld}s that are valid for display to the player
     */
    @Unmodifiable
    protected Collection<BuildWorld> filterWorlds(Collection<BuildWorld> buildWorlds, WorldDisplay worldDisplay) {
        return buildWorlds.stream()
                .filter(this::isWorldValidForDisplay)
                .filter(worldDisplay.getWorldFilter().apply())
                .toList();
    }

    /**
     * Checks if a given {@link BuildWorld} is valid for display based on visibility, status, and player permissions.
     *
     * @param buildWorld The world to check.
     * @return {@code true} if the world should be displayed, {@link false} otherwise
     */
    private boolean isWorldValidForDisplay(BuildWorld buildWorld) {
        WorldData worldData = buildWorld.getData();
        if (!this.worldStorage.isCorrectVisibility(worldData.privateWorld().get(), this.requiredVisibility)) {
            return false;
        }

        if (!this.validStatuses.contains(worldData.status().get())) {
            return false;
        }

        if (!buildWorld.getPermissions().canEnter(this.player)) {
            return false;
        }

        return Bukkit.getWorld(buildWorld.getName()) != null || !buildWorld.isLoaded();
    }

    /**
     * Adds the world sorting item to the inventory.
     *
     * @param inventory The inventory to add the item to
     */
    private void addWorldSortItem(Inventory inventory) {
        Settings settings = settingsManager.getSettings(player);
        WorldSort worldSort = settings.getWorldDisplay().getWorldSort();

        String messageKey = switch (worldSort) {
            case NAME_A_TO_Z -> "world_sort_name_az";
            case NAME_Z_TO_A -> "world_sort_name_za";
            case PROJECT_A_TO_Z -> "world_sort_project_az";
            case PROJECT_Z_TO_A -> "world_sort_project_za";
            case STATUS_NOT_STARTED -> "world_sort_status_not_started";
            case STATUS_FINISHED -> "world_sort_status_finished";
            case NEWEST_FIRST -> "world_sort_date_newest";
            case OLDEST_FIRST -> "world_sort_date_oldest";
        };

        inventory.setItem(45, InventoryUtils.createItem(XMaterial.BOOK, Messages.getString("world_sort_title", player), Messages.getString(messageKey, player)));
    }

    /**
     * Adds the world filter item to the inventory.
     *
     * @param inventory The inventory to add the item to
     */
    private void addWorldFilterItem(Inventory inventory) {
        Settings settings = settingsManager.getSettings(player);
        WorldFilter worldFilter = settings.getWorldDisplay().getWorldFilter();

        String loreKey = switch (worldFilter.getMode()) {
            case NONE -> "world_filter_mode_none";
            case STARTS_WITH -> "world_filter_mode_starts_with";
            case CONTAINS -> "world_filter_mode_contains";
            case MATCHES -> "world_filter_mode_matches";
        };

        List<String> lore = new ArrayList<>();
        lore.add(Messages.getString(loreKey, player, Map.entry("%text%", worldFilter.getText())));
        lore.addAll(Messages.getStringList("world_filter_lore", player));

        inventory.setItem(46, InventoryUtils.createItem(XMaterial.HOPPER, Messages.getString("world_filter_title", player), lore));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DisplayablesInventoryHolder)) {
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);
        WorldDisplay worldDisplay = settings.getWorldDisplay();

        switch (event.getSlot()) {
            case 45: // Sort
                Function<WorldSort, WorldSort> newSortFunction = event.isLeftClick() ? this::getNextSort : this::getPreviousSort;
                WorldSort newSort = newSortFunction.apply(worldDisplay.getWorldSort());
                worldDisplay.setWorldSort(newSort);
                updateAndReopenInventory();
                return;
            case 46: // Filter
                handleFilterClick(event, worldDisplay);
                return;
            case 48: // Create world
                if (itemStack.getType() == XMaterial.PLAYER_HEAD.get()) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    beginWorldCreation();
                    return;
                }
                break;
            case 49: // Create folder (archive)
            case 50: // Create folder (not archive)
                if (itemStack.getType() == XMaterial.PLAYER_HEAD.get()) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    player.closeInventory(); // Close to allow chat input
                    new PlayerChatInput(plugin, player, "enter_folder_name", input -> {
                        if (StringCleaner.hasInvalidNameCharacters(input)) {
                            Messages.sendMessage(player, "worlds_folder_creation_invalid_characters");
                        }

                        String sanitizedName = StringCleaner.sanitize(input);
                        if (sanitizedName.isEmpty()) {
                            Messages.sendMessage(player, "worlds_folder_creation_name_bank");
                            return;
                        }

                        if (folderStorage.folderExists(sanitizedName)) {
                            Messages.sendMessage(player, "worlds_folder_exists");
                            return;
                        }

                        Folder folder = createFolder(sanitizedName);
                        Messages.sendMessage(player, "worlds_folder_created", Map.entry("%folder%", folder.getName()));

                        initializeInventories();
                        openInventory();
                    });
                    return;
                }
                break;
            case 52: // Previous page
                if (decrementInv(player, cachedDisplayables.size(), MAX_WORLDS_PER_PAGE)) {
                    openInventory();
                }
                return;
            case 53: // Next page
                if (incrementInv(player, cachedDisplayables.size(), MAX_WORLDS_PER_PAGE)) {
                    openInventory();
                }
                return;
        }

        if (event.getSlot() >= FIRST_WORD_SLOT && event.getSlot() <= LAST_WORLD_SLOT) {
            handleDisplayableItemClick(event, itemStack);
        } else if (event.getSlot() >= 45 && event.getSlot() <= 53 && itemStack.getType() != XMaterial.PLAYER_HEAD.get()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            returnToPreviousInventory();
        }
    }

    protected void beginWorldCreation() {
        new CreateInventory(plugin, this.requiredVisibility).openInventory(this.player, Page.PREDEFINED);
    }

    protected Folder createFolder(String folderName) {
        return this.folderStorage.createFolder(folderName, this.category, Builder.of(this.player));
    }

    /**
     * Sends the player back to the previous inventory.
     */
    protected void returnToPreviousInventory() {
        new NavigatorInventory(plugin).openInventory(this.player);
    }

    /**
     * Handles clicks on the filter item, managing mode changes, and text input.
     *
     * @param event        The InventoryClickEvent.
     * @param worldDisplay The player's WorldDisplay settings.
     */
    private void handleFilterClick(InventoryClickEvent event, WorldDisplay worldDisplay) {
        WorldFilter worldFilter = worldDisplay.getWorldFilter();
        Mode currentMode = worldFilter.getMode();

        if (event.isShiftClick()) {
            worldFilter.setMode(Mode.NONE);
            worldFilter.setText("");
        } else if (event.isLeftClick()) {
            player.closeInventory();
            new PlayerChatInput(plugin, player, "world_filter_title", input -> {
                worldFilter.setText(input.replace("\"", ""));
                openInventory();
            });
            return;
        } else if (event.isRightClick()) {
            worldFilter.setMode(currentMode.getNext());
        }

        updateAndReopenInventory();
    }

    /**
     * Handles clicks on {@link Displayable} items (worlds or folders) within the inventory.
     *
     * @param event       The click event
     * @param clickedItem The item that was clicked
     */
    private void handleDisplayableItemClick(InventoryClickEvent event, ItemStack clickedItem) {
        ItemMeta itemMeta = clickedItem.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return;
        }

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        String displayableType = pdc.get(InventoryUtils.DISPLAYABLE_TYPE_KEY, PersistentDataType.STRING);
        String displayableName = pdc.get(InventoryUtils.DISPLAYABLE_NAME_KEY, PersistentDataType.STRING);
        if (displayableType == null || displayableName == null) {
            return;
        }

        switch (DisplayableType.valueOf(displayableType)) {
            case BUILD_WORLD -> {
                BuildWorld buildWorld = worldStorage.getBuildWorld(displayableName);
                if (buildWorld == null) {
                    plugin.getLogger().warning("Unable to find world with name: " + displayableName);
                    return;
                }
                manageWorldItemClick(event, buildWorld);
            }
            case FOLDER -> {
                Folder folder = folderStorage.getFolder(displayableName);
                if (folder == null) {
                    plugin.getLogger().warning("Unable to find folder with name: " + displayableName);
                    return;
                }
                new FolderContentInventory(plugin, player, category, folder, this, requiredVisibility, validStatuses).openInventory();
            }
        }
    }

    /**
     * Updates the inventory content and reopens it for the player. This is typically called after a setting (sort, filter) has changed.
     */
    private void updateAndReopenInventory() {
        resetInvIndex(player);
        initializeInventories();
        openInventory();
    }

    /**
     * Gets the next {@link WorldSort} order in the cycle.
     *
     * @param currentSort The current sort
     * @return The next sort in the sequence
     */
    private WorldSort getNextSort(WorldSort currentSort) {
        return switch (currentSort) {
            case NEWEST_FIRST -> WorldSort.OLDEST_FIRST;
            case OLDEST_FIRST -> WorldSort.NAME_A_TO_Z;
            case PROJECT_A_TO_Z -> WorldSort.PROJECT_Z_TO_A;
            case PROJECT_Z_TO_A -> WorldSort.STATUS_NOT_STARTED;
            case STATUS_NOT_STARTED -> WorldSort.STATUS_FINISHED;
            case STATUS_FINISHED -> WorldSort.NEWEST_FIRST;
            case NAME_A_TO_Z -> WorldSort.NAME_Z_TO_A;
            case NAME_Z_TO_A -> WorldSort.PROJECT_A_TO_Z;
        };
    }

    /**
     * Gets the previous {@link WorldSort} order in the cycle.
     *
     * @param currentSort The current sort
     * @return The previous sort in the sequence.
     */
    public WorldSort getPreviousSort(WorldSort currentSort) {
        return switch (currentSort) {
            case NEWEST_FIRST -> WorldSort.STATUS_FINISHED;
            case OLDEST_FIRST -> WorldSort.NEWEST_FIRST;
            case PROJECT_A_TO_Z -> WorldSort.NAME_Z_TO_A;
            case PROJECT_Z_TO_A -> WorldSort.PROJECT_A_TO_Z;
            case STATUS_NOT_STARTED -> WorldSort.PROJECT_Z_TO_A;
            case STATUS_FINISHED -> WorldSort.STATUS_NOT_STARTED;
            case NAME_A_TO_Z -> WorldSort.OLDEST_FIRST;
            case NAME_Z_TO_A -> WorldSort.NAME_A_TO_Z;
        };
    }

    /**
     * Manages the click action for an item representing a {@link BuildWorld}.
     *
     * @param event      The InventoryClickEvent.
     * @param buildWorld The BuildWorld associated with the clicked item.
     */
    private void manageWorldItemClick(InventoryClickEvent event, BuildWorld buildWorld) {
        if (event.isLeftClick() || !buildWorld.getPermissions().canPerformCommand(player, WorldsArgument.EDIT.getPermission())) {
            performNonEditClick(buildWorld);
            return;
        }

        if (buildWorld.isLoaded()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            new EditInventory(plugin).openInventory(player, buildWorld);
        } else {
            player.closeInventory();
            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            player.sendTitle(" ", Messages.getString("world_not_loaded", player), 5, 70, 20);
        }
    }

    /**
     * Performs a non-edit action for a {@link BuildWorld} item click (typically teleportation).
     *
     * @param buildWorld The BuildWorld to act upon.
     */
    private void performNonEditClick(BuildWorld buildWorld) {
        playerService.closeNewNavigator(player);
        buildWorld.getTeleporter().teleport(player);
    }

    private static class DisplayablesInventoryHolder extends BuildSystemHolder {

        public DisplayablesInventoryHolder(String inventoryTitle) {
            super(54, inventoryTitle);
        }
    }
}