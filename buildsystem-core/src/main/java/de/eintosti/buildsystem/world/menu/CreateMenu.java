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
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
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

    private static final String PREDEFINED_TAB_PROFILE =
            "2cdc0feb7001e2c10fd5066e501b87e3d64793092b85a50c856d962f8be92c78";
    private static final String GENERATOR_TAB_PROFILE =
            "b2f79016cad84d1ae21609c4813782598e387961be13c15682752f126dce7a";
    private static final String TEMPLATE_TAB_PROFILE =
            "d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c";

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

    private final MenuItems menuItems;
    private final Menus menus;
    private final WorldServiceImpl worldService;
    private final CustomizableIcons customizableIcons;
    private final File dataFolder;
    private final Page currentPage;
    private final boolean createPrivateWorld;

    private final @Nullable Folder folder;

    private int numTemplates = 0;

    private File @Nullable [] templateFiles;

    public CreateMenu(
            Messages messages,
            MenuItems menuItems,
            Menus menus,
            WorldServiceImpl worldService,
            CustomizableIcons customizableIcons,
            File dataFolder,
            Page initialPage,
            Visibility visibility,
            @Nullable Folder folder,
            Player player) {
        super(messages, 45, messages.getString("create_title", player));
        this.menuItems = menuItems;
        this.menus = menus;
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

        registerTab(Page.PREDEFINED, PREDEFINED_TAB_PROFILE, "create_predefined_worlds");
        registerTab(Page.GENERATOR, GENERATOR_TAB_PROFILE, "create_generators");
        registerTab(Page.TEMPLATES, TEMPLATE_TAB_PROFILE, "create_templates");

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
                            XSound.ENTITY_CHICKEN_EGG.play(player);
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
                    XMaterial material = canCreate ? customizableIcons.getIcon(worldType) : XMaterial.BARRIER;
                    String displayName = messages.getString(PREDEFINED_MESSAGE_KEYS.get(worldType), player);
                    if (!canCreate) {
                        displayName = "§c§m" + ChatColor.stripColor(displayName);
                    }
                    ItemBuilder itemBuilder = ItemBuilder.of(material).name(displayName);
                    if (canCreate) {
                        itemBuilder.lore(messages.getString("create_predefined_seed_lore", player));
                    }
                    itemBuilder.into(inventory, slot);
                })
                .onClick((player, event) -> {
                    if (!canCreateType(player, worldType)) {
                        XSound.ENTITY_ITEM_BREAK.play(player);
                        return;
                    }
                    worldService.startWorldNameInput(
                            player, worldType, null, createPrivateWorld, event.isShiftClick(), folder);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                })
                .build();
    }

    private static boolean canCreateType(Player player, BuildWorldType worldType) {
        return player.hasPermission(
                "buildsystem.create.type." + worldType.name().toLowerCase(Locale.ROOT));
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
                                    p, BuildWorldType.CUSTOM, null, createPrivateWorld, false, folder);
                            XSound.ENTITY_CHICKEN_EGG.play(p);
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

        menuItems.fillRange(player, getInventory(), FIRST_TEMPLATE_SLOT, SLOT_TEMPLATE_NEXT_PAGE);

        if (numTemplates == 0) {
            ItemStack barrier = ItemBuilder.of(XMaterial.BARRIER)
                    .name(messages.getString("create_no_templates", player))
                    .build();
            for (int i = FIRST_PREDEFINED_SLOT; i <= LAST_PREDEFINED_SLOT; i++) {
                getInventory().setItem(i, barrier);
            }
            return;
        }

        registerPageItems(
                FIRST_TEMPLATE_SLOT, MAX_TEMPLATES, Arrays.asList(templateFiles), f -> templateButton(f.getName()));
    }

    private MenuButton templateButton(String rawTemplateName) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.FILLED_MAP)
                        .name(messages.getString("create_template", player, Map.entry("%template%", rawTemplateName)))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    // Template names are dynamic and cannot be pre-registered in plugin.yml, so default-allow is
                    // emulated: a template is permitted unless an admin has explicitly denied its specific node.
                    String templateNode = "buildsystem.create.template." + rawTemplateName;
                    if (player.isPermissionSet(templateNode) && !player.hasPermission(templateNode)) {
                        XSound.ENTITY_ITEM_BREAK.play(player);
                        return;
                    }

                    ItemStack itemStack = event.getCurrentItem();
                    if (itemStack == null || itemStack.getItemMeta() == null) {
                        return;
                    }

                    worldService.startWorldNameInput(
                            player,
                            BuildWorldType.TEMPLATE,
                            itemStack.getItemMeta().getDisplayName(),
                            createPrivateWorld,
                            false,
                            folder);
                })
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

        /**
         * Gets the {@link Page} for the given slot.
         *
         * @param slot The clicked slot
         * @return The {@link Page} for the given slot, or {@code null} if the slot does not correspond to a page
         */
        public static @Nullable Page of(int slot) {
            return Arrays.stream(values())
                    .filter(value -> value.slot == slot)
                    .findFirst()
                    .orElse(null);
        }

        public int getSlot() {
            return slot;
        }
    }
}
