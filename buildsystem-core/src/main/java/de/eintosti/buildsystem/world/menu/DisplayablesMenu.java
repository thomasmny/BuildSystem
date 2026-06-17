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
import de.eintosti.buildsystem.api.world.display.*;
import de.eintosti.buildsystem.api.world.display.WorldFilter.Mode;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.DisplayOrdering;
import de.eintosti.buildsystem.world.menu.CreateMenu.Page;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class DisplayablesMenu extends PaginatedMenu {

    private static final int MAX_WORLDS_PER_PAGE = 36;
    private static final int FIRST_WORD_SLOT = 9;
    private static final int LAST_WORLD_SLOT = 44;

    private static final int SLOT_NO_WORLDS = 22;
    private static final int SLOT_WORLD_SORT = 45;
    private static final int SLOT_WORLD_FILTER = 46;
    private static final int SLOT_CREATE_WORLD = 48;
    private static final int FIRST_CREATE_FOLDER_SLOT = 49;
    private static final int LAST_CREATE_FOLDER_SLOT = 50;
    private static final int SLOT_BACK = 51;
    private static final int SLOT_PREVIOUS_PAGE = 52;
    private static final int SLOT_NEXT_PAGE = 53;
    private static final int FIRST_BOTTOM_BAR_SLOT = 45;
    private static final int LAST_BOTTOM_BAR_SLOT = 53;

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

    private final @Nullable String noWorldsMessage;
    private @Nullable List<Displayable> cachedDisplayables;

    protected DisplayablesMenu(BuildSystemPlugin plugin, Player player, Options options) {
        super(plugin.getMessages(), 54, options.title());
        this.plugin = plugin;
        this.playerService = plugin.getPlayerService();
        this.settingsManager = plugin.getSettingsService();
        WorldServiceImpl worldService = plugin.getWorldService();
        this.folderStorage = worldService.getFolderStorage();
        this.worldStorage = worldService.getWorldStorage();
        this.player = player;
        this.category = options.category();
        this.noWorldsMessage = options.emptyMessage();
    }

    /**
     * The configuration of a {@link DisplayablesMenu}: the {@link NavigatorCategory category} whose folders and worlds it lists,
     * its title and "no worlds" message. A world is listed when {@link NavigatorCategoryRegistry#getCategoryForWorld(BuildWorld)}
     * resolves to this category, so visibility and status filtering are derived from the category rather than passed separately.
     *
     * @param category The navigator category whose folders/worlds are listed
     * @param title The inventory title
     * @param emptyMessage The message shown when nothing matches, or {@code null} to show nothing
     */
    public record Options(
            NavigatorCategory category,
            String title,
            @Nullable String emptyMessage) {

        /**
         * {@return a new {@link Builder}}
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Fluent builder for {@link Options}. {@code category} and {@code title} are required.
         */
        public static final class Builder {

            private @Nullable NavigatorCategory category;
            private @Nullable String title;
            private @Nullable String emptyMessage;

            private Builder() {}

            public Builder category(NavigatorCategory category) {
                this.category = category;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder emptyMessage(@Nullable String emptyMessage) {
                this.emptyMessage = emptyMessage;
                return this;
            }

            public Options build() {
                return new Options(
                        Objects.requireNonNull(category, "category"),
                        Objects.requireNonNull(title, "title"),
                        emptyMessage);
            }
        }
    }

    @Override
    protected int totalItems() {
        return cachedDisplayables != null ? cachedDisplayables.size() : 0;
    }

    @Override
    protected void populate(Player player) {
        this.cachedDisplayables = collectDisplayables();
        Inventory inv = getInventory();

        clearButtons();
        plugin.getMenuItems().fillWithGlass(inv, player);
        addWorldSortItem(inv);
        addWorldFilterItem(inv);
        addExtraItems(inv, player);
        register(SLOT_PREVIOUS_PAGE, previousPageButton(PREVIOUS_PAGE_SKULL_PROFILE, MAX_WORLDS_PER_PAGE));
        register(SLOT_NEXT_PAGE, nextPageButton(NEXT_PAGE_SKULL_PROFILE, MAX_WORLDS_PER_PAGE));

        for (int i = FIRST_WORD_SLOT; i <= LAST_WORLD_SLOT; i++) {
            inv.setItem(i, null);
        }

        if (cachedDisplayables.isEmpty() && noWorldsMessage != null) {
            ItemBuilder.skull(Profileable.detect(NO_WORLDS_SKULL_PROFILE))
                    .name(noWorldsMessage)
                    .into(inv, SLOT_NO_WORLDS);
        } else {
            registerPageItems(FIRST_WORD_SLOT, MAX_WORLDS_PER_PAGE, cachedDisplayables, this::displayableButton);
        }

        renderButtons(player);
    }

    private MenuButton displayableButton(Displayable displayable) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> displayable.addToInventory(inventory, slot, player))
                .onClick((player, event) -> {
                    if (displayable instanceof BuildWorld buildWorld) {
                        manageWorldItemClick(event, buildWorld);
                    } else if (displayable instanceof Folder folder) {
                        new FolderContentMenu(plugin, player, category, folder, this).open(player);
                    }
                })
                .build();
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
        displayables.sort(
                DisplayOrdering.withPriorities(worldDisplay.getWorldSort().getComparator()));
        return displayables;
    }

    @Unmodifiable
    protected Collection<Folder> collectFolders() {
        return folderStorage.getFolders().stream()
                .filter(folder -> folder.getCategory().equals(this.category))
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
        if (!plugin.getNavigatorCategoryRegistry()
                .getCategoryForWorld(buildWorld)
                .equals(this.category)) {
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

        ItemBuilder.of(XMaterial.BOOK)
                .name(plugin.getMessages().getString("world_sort_title", player))
                .lore(plugin.getMessages().getString(messageKey, player))
                .into(inventory, SLOT_WORLD_SORT);
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

        ItemBuilder.of(XMaterial.HOPPER)
                .name(plugin.getMessages().getString("world_filter_title", player))
                .lore(lore)
                .into(inventory, SLOT_WORLD_FILTER);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Page arrows are registered buttons; everything else is handled here. Ignore clicks outside this inventory.
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getInventory().getSize()) {
            return;
        }
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        Settings settings = settingsManager.getSettings(player);
        WorldDisplay worldDisplay = settings.getWorldDisplay();

        switch (slot) {
            case SLOT_WORLD_SORT -> {
                WorldSort currentSort = worldDisplay.getWorldSort();
                worldDisplay.setWorldSort(event.isLeftClick() ? currentSort.getNext() : currentSort.getPrevious());
                resetPage();
                open(player);
            }
            case SLOT_WORLD_FILTER -> handleFilterClick(event, worldDisplay);
            case SLOT_CREATE_WORLD -> {
                if (itemStack.getType() == XMaterial.PLAYER_HEAD.get()) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    beginWorldCreation();
                    return;
                }
                goBack(player, itemStack);
            }
            case FIRST_CREATE_FOLDER_SLOT, LAST_CREATE_FOLDER_SLOT -> {
                if (itemStack.getType() == XMaterial.PLAYER_HEAD.get()) {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    beginFolderCreation(player);
                    return;
                }
                goBack(player, itemStack);
            }
            case SLOT_BACK -> goBack(player, itemStack);
            default -> {
                if (slot >= FIRST_BOTTOM_BAR_SLOT && slot <= LAST_BOTTOM_BAR_SLOT) {
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
        new CreateMenu(plugin, Page.PREDEFINED, this.category.getPrimaryVisibility(), null, this.player)
                .open(this.player);
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
