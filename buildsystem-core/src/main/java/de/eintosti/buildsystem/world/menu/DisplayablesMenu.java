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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.WorldDisplay;
import de.eintosti.buildsystem.api.world.display.WorldFilter;
import de.eintosti.buildsystem.api.world.display.WorldFilter.Mode;
import de.eintosti.buildsystem.api.world.display.WorldSort;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.DisplayOrdering;
import de.eintosti.buildsystem.world.menu.CreateMenu.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class DisplayablesMenu extends PaginatedMenu {

    protected static final int MAX_WORLDS_PER_PAGE = 36;
    protected static final int FIRST_WORLD_SLOT = 9;

    protected static final int SLOT_NO_WORLDS = 22;
    protected static final int SLOT_WORLD_SORT = 45;
    protected static final int SLOT_WORLD_FILTER = 46;
    protected static final int SLOT_CREATE_WORLD = 48;
    protected static final int FIRST_CREATE_FOLDER_SLOT = 49;
    protected static final int LAST_CREATE_FOLDER_SLOT = 50;
    protected static final int SLOT_PREVIOUS_PAGE = 52;
    protected static final int SLOT_NEXT_PAGE = 53;
    protected static final int FIRST_BOTTOM_BAR_SLOT = 45;
    protected static final int LAST_BOTTOM_BAR_SLOT = 53;

    protected final PlayerServiceImpl playerService;
    protected final SettingsService settingsManager;
    protected final FolderStorageImpl folderStorage;
    protected final WorldStorageImpl worldStorage;

    private final Prompts prompts;
    private final NavigatorService navigatorService;

    protected final Player player;
    protected final NavigatorCategory category;

    private final @Nullable String noWorldsMessage;
    private @Nullable List<Displayable> cachedDisplayables;

    private final DisplayBar displayBar = new DisplayBar();

    protected DisplayablesMenu(DisplayablesContext context, Player player, Options options) {
        super(context.menu(), 54, options.title());
        this.playerService = context.playerService();
        this.settingsManager = context.settingsService();
        WorldServiceImpl worldService = context.worldService();
        this.folderStorage = worldService.getFolderStorage();
        this.worldStorage = worldService.getWorldStorage();
        this.prompts = context.prompts();
        this.navigatorService = context.navigatorService();
        this.player = player;
        this.category = options.category();
        this.noWorldsMessage = options.emptyMessage();
    }

    /**
     * The configuration of a {@link DisplayablesMenu}: the {@link NavigatorCategory category} whose folders and worlds it lists,
     * its title and "no worlds" message. A world is listed in this category when the category
     * {@link NavigatorCategory#groups groups} the world's visibility and status, so a world shown by several overlapping
     * categories appears in each; the filtering is derived from the category rather than passed separately.
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

    /**
     * Recomputes {@link #cachedDisplayables} from the current folder/world storage state. Called on every {@link #open},
     * so navigating to or reopening this menu always reflects the latest data.
     *
     * <p>A page flip does not go through here: {@link PaginatedMenu}'s page-arrow buttons call {@link #populate} directly,
     * which re-renders the already-collected list for the new page instead of recollecting and re-filtering everything.
     */
    private void refreshData() {
        this.cachedDisplayables = collectDisplayables();
    }

    @Override
    public void open(Player player) {
        refreshData();
        super.open(player);
    }

    @Override
    protected void populate(Player player) {
        List<Displayable> displayables = cachedDisplayables != null ? cachedDisplayables : List.of();
        Inventory inv = getInventory();

        clearButtons();
        menuItems.fillWithGlass(inv, player);
        register(
                SLOT_WORLD_SORT,
                MenuButton.builder()
                        .render((p, inventory, slot) -> displayBar.renderSort(inventory))
                        .onClick((p, event) -> displayBar.handleSortClick(event, worldDisplay(p)))
                        .build());
        register(
                SLOT_WORLD_FILTER,
                MenuButton.builder()
                        .render((p, inventory, slot) -> displayBar.renderFilter(inventory))
                        .onClick((p, event) -> displayBar.handleFilterClick(event, worldDisplay(p)))
                        .build());
        registerCreateButtons(player);
        register(SLOT_PREVIOUS_PAGE, previousPageButton(SkullTextures.PREVIOUS_PAGE, MAX_WORLDS_PER_PAGE));
        register(SLOT_NEXT_PAGE, nextPageButton(SkullTextures.NEXT_PAGE, MAX_WORLDS_PER_PAGE));

        registerPageItems(FIRST_WORLD_SLOT, MAX_WORLDS_PER_PAGE, displayables, this::displayableButton);
        if (displayables.isEmpty() && noWorldsMessage != null) {
            ItemBuilder.skull(Profileable.detect(SkullTextures.NO_WORLDS))
                    .name(noWorldsMessage)
                    .into(inv, SLOT_NO_WORLDS);
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
                        menus.openFolderContent(category, folder, this, player);
                    }
                })
                .build();
    }

    /**
     * Which create buttons the bottom bar offers the player.
     */
    protected record CreateButtons(boolean world, boolean folder) {
        static final CreateButtons NONE = new CreateButtons(false, false);
    }

    /**
     * {@return which create buttons the bottom bar offers the player} None by default.
     */
    protected CreateButtons createButtons(Player player) {
        return CreateButtons.NONE;
    }

    private void registerCreateButtons(Player player) {
        CreateButtons offered = createButtons(player);
        if (offered.world()) {
            register(
                    SLOT_CREATE_WORLD,
                    createButton(SkullTextures.ADD_ITEM, "world_navigator_create_world", p -> beginWorldCreation()));
        }
        if (offered.folder()) {
            // With the create-world button hidden, centre the lone folder button instead of leaving it off to the side.
            register(
                    offered.world() ? LAST_CREATE_FOLDER_SLOT : FIRST_CREATE_FOLDER_SLOT,
                    createButton(
                            SkullTextures.CREATE_FOLDER, "world_navigator_create_folder", this::beginFolderCreation));
        }
    }

    private MenuButton createButton(String profile, String nameKey, Consumer<Player> onCreate) {
        return MenuButton.builder()
                .render((p, inventory, slot) -> ItemBuilder.skull(Profileable.detect(profile))
                        .name(messages.getString(nameKey, p))
                        .into(inventory, slot))
                .onClick((p, event) -> {
                    p.playSound(p, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
                    onCreate.accept(p);
                })
                .build();
    }

    private WorldDisplay worldDisplay(Player player) {
        return settingsManager.getSettings(player).getWorldDisplay();
    }

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
        // A world shows in every category that groups it (overlapping categories each list it), not just its primary
        // resolved category.
        if (!this.category.groups(
                buildWorld.getData().get(WorldDataKey.VISIBILITY),
                buildWorld.getData().get(WorldDataKey.STATUS).getId())) {
            return false;
        }
        if (!buildWorld.getPermissions().canEnter(this.player)) {
            return false;
        }
        return WorldNames.bukkitWorld(buildWorld.getName()) != null || !buildWorld.isLoaded();
    }

    /**
     * Sort, filter, the create buttons and the page arrows are registered buttons; any other click in the bottom bar
     * goes back.
     */
    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot >= FIRST_BOTTOM_BAR_SLOT && slot <= LAST_BOTTOM_BAR_SLOT) {
            player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1f, 1f);
            returnToPreviousInventory();
        }
    }

    protected void beginWorldCreation() {
        menus.openCreate(Page.PREDEFINED, this.category.getPrimaryVisibility(), null, this.player);
    }

    private void beginFolderCreation(Player player) {
        player.closeInventory();
        prompts.prompt(player)
                .title("enter_folder_name")
                .sanitizeName("worlds_folder_creation_invalid_characters", "worlds_folder_creation_name_bank")
                .onCancel(() -> open(player))
                .request(folderName -> {
                    if (folderStorage.folderExists(folderName)) {
                        messages.sendMessage(player, "worlds_folder_exists");
                        return;
                    }

                    Folder folder = createFolder(folderName);
                    messages.sendMessage(
                            player, "worlds_folder_created", Placeholders.of("%folder%", folder.getName()));
                    open(player);
                });
    }

    protected Folder createFolder(String folderName) {
        return this.folderStorage.createFolder(folderName, this.category, Builder.of(this.player));
    }

    protected void returnToPreviousInventory() {
        menus.openNavigator(this.player);
    }

    private void manageWorldItemClick(InventoryClickEvent event, BuildWorld buildWorld) {
        Player player = (Player) event.getWhoClicked();
        if (event.isLeftClick()
                || !buildWorld.getPermissions().canPerformCommand(player, WorldsArgument.EDIT.getPermission())) {
            navigatorService.closeNewNavigator(player);
            buildWorld.getTeleporter().teleport(player);
            return;
        }

        menus.openEdit(buildWorld, player);
    }

    /**
     * The sort and filter items in the bottom bar of every {@link DisplayablesMenu}: their rendering reflects the
     * player's current {@link WorldDisplay} settings, and clicking them cycles or edits those settings before
     * refreshing the menu. Kept as an inner class, rather than threading callbacks through a standalone one, since
     * every operation here ends by calling back into the enclosing menu's {@link #resetPage()} and {@link #open}.
     */
    private final class DisplayBar {

        void renderSort(Inventory inventory) {
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

            ItemBuilder.of(Material.BOOK)
                    .name(messages.getString("world_sort_title", player))
                    .lore(messages.getString(messageKey, player))
                    .into(inventory, SLOT_WORLD_SORT);
        }

        void renderFilter(Inventory inventory) {
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
            lore.add(messages.getString(loreKey, player, Placeholders.of("%text%", worldFilter.getText())));
            lore.addAll(messages.getStringList("world_filter_lore", player));

            ItemBuilder.of(Material.HOPPER)
                    .name(messages.getString("world_filter_title", player))
                    .lore(lore)
                    .into(inventory, SLOT_WORLD_FILTER);
        }

        void handleSortClick(InventoryClickEvent event, WorldDisplay worldDisplay) {
            WorldSort currentSort = worldDisplay.getWorldSort();
            worldDisplay.setWorldSort(event.isLeftClick() ? currentSort.getNext() : currentSort.getPrevious());
            resetPage();
            open(player);
        }

        void handleFilterClick(InventoryClickEvent event, WorldDisplay worldDisplay) {
            WorldFilter worldFilter = worldDisplay.getWorldFilter();
            Mode currentMode = worldFilter.getMode();

            if (event.isShiftClick()) {
                worldFilter.setMode(Mode.NONE);
                worldFilter.setText("");
            } else if (event.isLeftClick()) {
                player.closeInventory();
                prompts.prompt(player)
                        .title("world_filter_title")
                        .onCancel(() -> open(player))
                        .request(input -> {
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
    }
}
