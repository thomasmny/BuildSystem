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
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.FeedbackSound;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldNameInputOptions;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CreateMenu extends PaginatedMenu {

    private static final int MAX_TEMPLATES = 5;

    private static final int FIRST_PREDEFINED_SLOT = 29;
    private static final int LAST_PREDEFINED_SLOT = 33;
    private static final int SLOT_GENERATOR_CREATE = 31;
    private static final int SLOT_TEMPLATE_PREVIOUS_PAGE = 28;
    private static final int SLOT_TEMPLATE_NEXT_PAGE = 34;
    private static final int FIRST_TEMPLATE_SLOT = 29;

    private static final Map<Integer, BuildWorldType> PREDEFINED_SLOTS = Map.of(
            29, BuildWorldType.NORMAL,
            30, BuildWorldType.FLAT,
            31, BuildWorldType.NETHER,
            32, BuildWorldType.END,
            33, BuildWorldType.VOID);

    private static final Map<BuildWorldType, String> PREDEFINED_MESSAGE_KEYS = Map.of(
            BuildWorldType.NORMAL, "create_normal_world",
            BuildWorldType.FLAT, "create_flat_world",
            BuildWorldType.NETHER, "create_nether_world",
            BuildWorldType.END, "create_end_world",
            BuildWorldType.VOID, "create_void_world");

    private final WorldServiceImpl worldService;
    private final CustomizableIcons customizableIcons;
    private final File dataFolder;
    private final Page currentPage;
    private final boolean createPrivateWorld;

    private final @Nullable Folder folder;

    private int numTemplates = 0;

    private File @Nullable [] templateFiles;

    public CreateMenu(
            MenuContext context,
            WorldServiceImpl worldService,
            CustomizableIcons customizableIcons,
            File dataFolder,
            Page initialPage,
            Visibility visibility,
            @Nullable Folder folder,
            Player player) {
        super(context, 45, context.messages().getString("create_title", player));
        this.worldService = worldService;
        this.customizableIcons = customizableIcons;
        this.dataFolder = dataFolder;
        this.currentPage = initialPage;
        this.createPrivateWorld = visibility == Visibility.ADDED_PLAYERS;
        this.folder = folder;
    }

    @Override
    protected int totalItems() {
        return currentPage == Page.TEMPLATES ? numTemplates : 0;
    }

    @Override
    protected void populate(Player player) {
        clearButtons();
        menuItems.fillRange(player, getInventory(), 0, 29);
        menuItems.fillRange(player, getInventory(), 34, 45);

        registerTab(Page.PREDEFINED, SkullTextures.PREDEFINED_WORLDS_TAB, "create_predefined_worlds");
        registerTab(Page.GENERATOR, SkullTextures.GENERATORS_TAB, "create_generators");
        registerTab(Page.TEMPLATES, SkullTextures.TEMPLATES_TAB, "create_templates");

        switch (currentPage) {
            case PREDEFINED -> registerPredefined();
            case GENERATOR -> registerGenerator(player);
            case TEMPLATES -> registerTemplates(player);
        }

        renderButtons(player);
    }

    private void registerTab(Page page, String skullTexture, String nameKey) {
        register(
                page.getSlot(),
                MenuButton.builder()
                        .render((player, inventory, slot) -> ItemBuilder.skull(Profileable.detect(skullTexture))
                                .name(messages.getString(nameKey, player))
                                .glow(currentPage == page)
                                .into(inventory, slot))
                        .onClick((player, event) -> {
                            Visibility visibility = createPrivateWorld ? Visibility.ADDED_PLAYERS : Visibility.EVERYONE;
                            menus.openCreate(page, visibility, folder, player);
                            FeedbackSound.PAGE.play(player);
                        })
                        .build());
    }

    private void registerPredefined() {
        PREDEFINED_SLOTS.forEach((slot, worldType) -> register(slot, predefinedButton(worldType)));
    }

    private MenuButton predefinedButton(BuildWorldType worldType) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    boolean canCreate = canCreateType(player, worldType);
                    Material material = canCreate ? customizableIcons.getIcon(worldType) : Material.BARRIER;
                    String displayName = messages.getString(PREDEFINED_MESSAGE_KEYS.get(worldType), player);
                    if (!canCreate) {
                        displayName = "%s%s%s"
                                .formatted(ChatColor.RED, ChatColor.STRIKETHROUGH, ChatColor.stripColor(displayName));
                    }
                    ItemBuilder itemBuilder = ItemBuilder.of(material).name(displayName);
                    if (canCreate) {
                        itemBuilder.lore(messages.getString("create_predefined_seed_lore", player));
                    }
                    itemBuilder.into(inventory, slot);
                })
                .usableBy(player -> canCreateType(player, worldType))
                .onClick((player, event) -> {
                    worldService.startWorldNameInput(
                            player,
                            worldType,
                            null,
                            new WorldNameInputOptions(createPrivateWorld, event.isShiftClick()),
                            folder);
                    FeedbackSound.CLICK.play(player);
                })
                .build();
    }

    private static boolean canCreateType(Player player, BuildWorldType worldType) {
        return player.hasPermission(Permissions.createType(worldType.name()));
    }

    /**
     * Template names are dynamic and cannot be pre-registered in {@code plugin.yml}, so default-allow is emulated: a
     * template is permitted unless an admin has explicitly denied its specific node.
     */
    private static boolean isTemplateAllowed(Player player, String rawTemplateName) {
        String templateNode = Permissions.createTemplate(rawTemplateName);
        return !player.isPermissionSet(templateNode) || player.hasPermission(templateNode);
    }

    /**
     * Plays the deny sound but keeps the menu open, so a click on a barred world type or template does not eject the
     * player from the creation flow.
     */
    @Override
    protected void onPermissionDenied(Player player, InventoryClickEvent event) {
        FeedbackSound.REFUSE.play(player);
    }

    private void registerGenerator(Player player) {
        for (int slot = FIRST_PREDEFINED_SLOT; slot <= LAST_PREDEFINED_SLOT; slot++) {
            if (slot != SLOT_GENERATOR_CREATE) {
                menuItems.addGlassPane(player, getInventory(), slot);
            }
        }
        register(
                SLOT_GENERATOR_CREATE,
                MenuButton.builder()
                        .render((p, inventory, slot) -> ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                                .name(messages.getString("create_generators_create_world", p))
                                .into(inventory, slot))
                        .onClick((p, event) -> {
                            worldService.startWorldNameInput(
                                    p,
                                    BuildWorldType.CUSTOM,
                                    null,
                                    new WorldNameInputOptions(createPrivateWorld, false),
                                    folder);
                            FeedbackSound.CLICK.play(p);
                        })
                        .build());
    }

    private void registerTemplates(Player player) {
        register(SLOT_TEMPLATE_PREVIOUS_PAGE, previousPageButton(SkullTextures.PREVIOUS_PAGE, MAX_TEMPLATES));
        register(SLOT_TEMPLATE_NEXT_PAGE, nextPageButton(SkullTextures.NEXT_PAGE, MAX_TEMPLATES));

        // Listed once per menu instance so page flips do not repeat directory I/O on the main thread.
        if (this.templateFiles == null) {
            this.templateFiles = FileUtils.resolve(dataFolder, "templates")
                    .toFile()
                    .listFiles(file -> file.isDirectory() && !file.isHidden());
        }
        this.numTemplates = templateFiles != null ? templateFiles.length : 0;

        if (numTemplates == 0) {
            ItemStack barrier = ItemBuilder.of(Material.BARRIER)
                    .name(messages.getString("create_no_templates", player))
                    .build();
            for (int i = FIRST_PREDEFINED_SLOT; i <= LAST_PREDEFINED_SLOT; i++) {
                getInventory().setItem(i, barrier);
            }
            return;
        }

        registerPageItems(
                FIRST_TEMPLATE_SLOT, MAX_TEMPLATES, Arrays.asList(templateFiles), f -> templateButton(f.getName()));
        // Unused template slots show glass; the template buttons render over it.
        menuItems.fillRange(player, getInventory(), FIRST_TEMPLATE_SLOT, SLOT_TEMPLATE_NEXT_PAGE);
    }

    private MenuButton templateButton(String rawTemplateName) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(Material.FILLED_MAP)
                        .name(messages.getString(
                                "create_template", player, Placeholders.of("%template%", rawTemplateName)))
                        .into(inventory, slot))
                .usableBy(player -> isTemplateAllowed(player, rawTemplateName))
                .onClick((player, event) -> worldService.startWorldNameInput(
                        player,
                        BuildWorldType.TEMPLATE,
                        rawTemplateName,
                        new WorldNameInputOptions(createPrivateWorld, false),
                        folder))
                .build();
    }

    public enum Page {
        PREDEFINED(12),
        GENERATOR(13),
        TEMPLATES(14);

        private final int slot;

        Page(int slot) {
            this.slot = slot;
        }

        public int getSlot() {
            return slot;
        }
    }
}
