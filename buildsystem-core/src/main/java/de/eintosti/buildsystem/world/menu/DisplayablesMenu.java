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
package de.eintosti.buildsystem.world.menu;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.*;
import de.eintosti.buildsystem.api.world.display.Displayable.DisplayableType;
import de.eintosti.buildsystem.api.world.display.WorldFilter.Mode;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.menu.InventoryUtils;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.menu.CreateMenu.Page;
import java.util.*;
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

@NullMarked
public abstract class DisplayablesMenu extends PaginatedMenu {

    private static final int MAX_WORLDS_PER_PAGE = 36;
    private static final int FIRST_WORD_SLOT = 9;
    private static final int LAST_WORLD_SLOT = 44;

    private static final String PREVIOUS_PAGE_SKULL_PROFILE =
            "86971dd881dbaf4fd6bcaa93614493c612f869641ed59d1c9363a3666a5fa6";
    private static final String NEXT_PAGE_SKULL_PROFILE =
            "f32ca66056b72863e98f7f32bd7d94c7a0d796af691c9ac3a9136331352288f9";
    private static final String NO_WORLDS_SKULL_PROFILE =
            "2e3f50ba62cbda3ecf5479b62fedebd61d76589771cc19286bf2745cd71e47c6";

    protected final BuildSystemPlugin plugin;
    protected final PlayerServiceImpl playerService;
    protected final SettingsService settingsManager;
    protected final FolderStorageImpl folderStorage;
    protected final WorldStorageImpl worldStorage;

    protected final Player player;
    protected final NavigatorCategory category;
    protected final Visibility requiredVisibility;
    protected final Set<BuildWorldStatus> validStatuses;

    private final @Nullable String noWorldsMessage;
    private @Nullable List<Displayable> cachedDisplayables;

    protected DisplayablesMenu(
            BuildSystemPlugin plugin,
            Player player,
            NavigatorCategory category,
            String inventoryTitle,
            @Nullable String noWorldsMessage,
            Visibility requiredVisibility,
            Set<BuildWorldStatus> validStatuses) {
        super(plugin.getMessages(), 54, inventoryTitle);
        this.plugin = plugin;
        this.playerService = plugin.getPlayerService();
        this.settingsManager = plugin.getSettingsService();
        WorldServiceImpl worldService = plugin.getWorldService();
        this.folderStorage = worldService.getFolderStorage();
        this.worldStorage = worldService.getWorldStorage();
        this.player = player;
        this.category = category;
        this.noWorldsMessage = noWorldsMessage;
        this.requiredVisibility = requiredVisibility;
        this.validStatuses = validStatuses;
    }

    @Override
    protected int totalItems() {
        return cachedDisplayables != null ? cachedDisplayables.size() : 0;
    }

    @Override
    protected void populate(Player player) {
        this.cachedDisplayables = collectDisplayables();
        Inventory inv = getInventory();

        plugin.getMenuItems().fillWithGlass(inv, player);
        addWorldSortItem(inv);
        addWorldFilterItem(inv);
        addExtraItems(inv, player);
        inv.setItem(
                52,
                InventoryUtils.createSkull(
                        plugin.getMessages().getString("gui_previous_page", player),
                        Profileable.detect(PREVIOUS_PAGE_SKULL_PROFILE)));
        inv.setItem(
                53,
                InventoryUtils.createSkull(
                        plugin.getMessages().getString("gui_next_page", player),
                        Profileable.detect(NEXT_PAGE_SKULL_PROFILE)));

        for (int i = FIRST_WORD_SLOT; i <= LAST_WORLD_SLOT; i++) {
            inv.setItem(i, null);
        }

        if (cachedDisplayables.isEmpty() && noWorldsMessage != null) {
            inv.setItem(22, InventoryUtils.createSkull(noWorldsMessage, Profileable.detect(NO_WORLDS_SKULL_PROFILE)));
            return;
        }

        int startIndex = page() * MAX_WORLDS_PER_PAGE;
        int endIndex = Math.min(startIndex + MAX_WORLDS_PER_PAGE, cachedDisplayables.size());
        int currentSlot = FIRST_WORD_SLOT;
        for (int i = startIndex; i < endIndex; i++) {
            if (currentSlot > LAST_WORLD_SLOT) {
                break;
            }
            cachedDisplayables.get(i).addToInventory(inv, currentSlot++, player);
        }
    }

    protected void addExtraItems(Inventory inventory, Player player) {}

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

    @Unmodifiable
    protected Collection<Folder> collectFolders() {
        return folderStorage.getFolders().stream()
                .filter(folder -> folder.getCategory() == this.category)
                .filter(folder -> !folder.hasParent())
                .filter(folder -> folder.canView(this.player))
                .toList();
    }

    protected Collection<BuildWorld> collectWorlds() {
        return worldStorage.getBuildWorlds();
    }

    @Unmodifiable
    protected Collection<BuildWorld> filterWorlds(Collection<BuildWorld> buildWorlds, WorldDisplay worldDisplay) {
        return buildWorlds.stream()
                .filter(this::isWorldValidForDisplay)
                .filter(worldDisplay.getWorldFilter().apply())
                .toList();
    }

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

    private void addWorldSortItem(Inventory inventory) {
        Settings settings = settingsManager.getSettings(player);
        WorldSort worldSort = settings.getWorldDisplay().getWorldSort();

        String messageKey =
                switch (worldSort) {
                    case NAME_A_TO_Z -> "world_sort_name_az";
                    case NAME_Z_TO_A -> "world_sort_name_za";
                    case PROJECT_A_TO_Z -> "world_sort_project_az";
                    case PROJECT_Z_TO_A -> "world_sort_project_za";
                    case STATUS_NOT_STARTED -> "world_sort_status_not_started";
                    case STATUS_FINISHED -> "world_sort_status_finished";
                    case NEWEST_FIRST -> "world_sort_date_newest";
                    case OLDEST_FIRST -> "world_sort_date_oldest";
                };

        inventory.setItem(
                45,
                InventoryUtils.createItem(
                        XMaterial.BOOK,
                        plugin.getMessages().getString("world_sort_title", player),
                        plugin.getMessages().getString(messageKey, player)));
    }

    private void addWorldFilterItem(Inventory inventory) {
        Settings settings = settingsManager.getSettings(player);
        WorldFilter worldFilter = settings.getWorldDisplay().getWorldFilter();

        String loreKey =
                switch (worldFilter.getMode()) {
                    case NONE -> "world_filter_mode_none";
                    case STARTS_WITH -> "world_filter_mode_starts_with";
                    case CONTAINS -> "world_filter_mode_contains";
                    case MATCHES -> "world_filter_mode_matches";
                };

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessages().getString(loreKey, player, Map.entry("%text%", worldFilter.getText())));
        lore.addAll(plugin.getMessages().getStringList("world_filter_lore", player));

        inventory.setItem(
                46,
                InventoryUtils.createItem(
                        XMaterial.HOPPER, plugin.getMessages().getString("world_filter_title", player), lore));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);
        WorldDisplay worldDisplay = settings.getWorldDisplay();

        switch (event.getSlot()) {
            case 45 -> {
                WorldSort currentSort = worldDisplay.getWorldSort();
                worldDisplay.setWorldSort(event.isLeftClick() ? currentSort.getNext() : currentSort.getPrevious());
                resetPage();
                open(player);
            }
            case 46 -> handleFilterClick(event, worldDisplay);
            case 48 -> {
                if (itemStack.getType() == XMaterial.PLAYER_HEAD.get()) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    beginWorldCreation();
                    return;
                }
                goBack(player, itemStack);
            }
            case 49, 50 -> {
                if (itemStack.getType() == XMaterial.PLAYER_HEAD.get()) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    beginFolderCreation(player);
                    return;
                }
                goBack(player, itemStack);
            }
            case 51 -> goBack(player, itemStack);
            case 52 -> {
                if (previousPage(player, MAX_WORLDS_PER_PAGE)) {
                    populate(player);
                }
            }
            case 53 -> {
                if (nextPage(player, MAX_WORLDS_PER_PAGE)) {
                    populate(player);
                }
            }
            default -> {
                if (event.getSlot() >= FIRST_WORD_SLOT && event.getSlot() <= LAST_WORLD_SLOT) {
                    handleDisplayableItemClick(event, itemStack);
                } else if (event.getSlot() >= 45 && event.getSlot() <= 53) {
                    goBack(player, itemStack);
                }
            }
        }
    }

    private void goBack(Player player, ItemStack itemStack) {
        if (itemStack.getType() != XMaterial.PLAYER_HEAD.get()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            returnToPreviousInventory();
        }
    }

    protected void beginWorldCreation() {
        new CreateMenu(plugin, Page.PREDEFINED, this.requiredVisibility, null, this.player).open(this.player);
    }

    private void beginFolderCreation(Player player) {
        player.closeInventory();
        PlayerChatInput.requestSanitizedName(
                plugin,
                player,
                "enter_folder_name",
                "worlds_folder_creation_invalid_characters",
                "worlds_folder_creation_name_bank",
                folderName -> {
                    if (folderStorage.folderExists(folderName)) {
                        plugin.getMessages().sendMessage(player, "worlds_folder_exists");
                        return;
                    }

                    Folder folder = createFolder(folderName);
                    plugin.getMessages()
                            .sendMessage(player, "worlds_folder_created", Map.entry("%folder%", folder.getName()));
                    open(player);
                });
    }

    protected Folder createFolder(String folderName) {
        return this.folderStorage.createFolder(folderName, this.category, Builder.of(this.player));
    }

    protected void returnToPreviousInventory() {
        new NavigatorMenu(plugin, this.player).open(this.player);
    }

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
                resetPage();
                open(player);
            });
            return;
        } else if (event.isRightClick()) {
            worldFilter.setMode(currentMode.getNext());
        }

        resetPage();
        open(player);
    }

    private void handleDisplayableItemClick(InventoryClickEvent event, ItemStack clickedItem) {
        ItemMeta itemMeta = clickedItem.getItemMeta();
        if (itemMeta == null || !itemMeta.hasDisplayName()) {
            return;
        }

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        String displayableType = pdc.get(plugin.getMenuItems().displayableTypeKey, PersistentDataType.STRING);
        String displayableName = pdc.get(plugin.getMenuItems().displayableNameKey, PersistentDataType.STRING);
        if (displayableType == null || displayableName == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
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
                new FolderContentMenu(plugin, player, category, folder, this, requiredVisibility, validStatuses)
                        .open(player);
            }
        }
    }

    private void manageWorldItemClick(InventoryClickEvent event, BuildWorld buildWorld) {
        Player player = (Player) event.getWhoClicked();
        if (event.isLeftClick()
                || !buildWorld.getPermissions().canPerformCommand(player, WorldsArgument.EDIT.getPermission())) {
            plugin.getNavigatorService().closeNewNavigator(player);
            buildWorld.getTeleporter().teleport(player);
            return;
        }

        if (buildWorld.isLoaded()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            new EditMenu(plugin, buildWorld, player).open(player);
        } else {
            player.closeInventory();
            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            player.sendTitle(" ", plugin.getMessages().getString("world_not_loaded", player), 5, 70, 20);
        }
    }
}
