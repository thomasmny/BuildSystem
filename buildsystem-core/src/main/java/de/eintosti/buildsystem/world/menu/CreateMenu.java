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

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.menu.InventoryUtils;
import de.eintosti.buildsystem.menu.PaginatedMenu;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CreateMenu extends PaginatedMenu {

    private static final int MAX_TEMPLATES = 5;

    private final BuildSystemPlugin plugin;
    private final WorldServiceImpl worldService;
    private final Page currentPage;
    private final boolean createPrivateWorld;

    private final @Nullable Folder folder;

    private int numTemplates = 0;

    private File @Nullable [] templateFiles;

    // Maps the slot a template item occupies on the current page to its raw directory name, so click handling can
    // resolve the unformatted template name for permission checks without parsing the display string.
    private final Map<Integer, String> templateSlots = new HashMap<>();

    public CreateMenu(
            BuildSystemPlugin plugin, Page initialPage, Visibility visibility, @Nullable Folder folder, Player player) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("create_title", player));
        this.plugin = plugin;
        this.worldService = plugin.getWorldService();
        this.currentPage = initialPage;
        this.createPrivateWorld = visibility == Visibility.PRIVATE;
        this.folder = folder;
    }

    @Override
    protected int totalItems() {
        return currentPage == Page.TEMPLATES ? numTemplates : 0;
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillRange(player, getInventory(), 0, 29);
        plugin.getMenuItems().fillRange(player, getInventory(), 34, 45);

        addPageItem(
                Page.PREDEFINED,
                InventoryUtils.createSkull(
                        messages.getString("create_predefined_worlds", player),
                        Profileable.detect("2cdc0feb7001e2c10fd5066e501b87e3d64793092b85a50c856d962f8be92c78")));
        addPageItem(
                Page.GENERATOR,
                InventoryUtils.createSkull(
                        messages.getString("create_generators", player),
                        Profileable.detect("b2f79016cad84d1ae21609c4813782598e387961be13c15682752f126dce7a")));
        addPageItem(
                Page.TEMPLATES,
                InventoryUtils.createSkull(
                        messages.getString("create_templates", player),
                        Profileable.detect("d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c")));

        switch (currentPage) {
            case PREDEFINED -> populatePredefined(player);
            case GENERATOR -> populateGenerator(player);
            case TEMPLATES -> populateTemplates(player);
        }
    }

    private void addPageItem(Page page, ItemStack itemStack) {
        if (currentPage == page) {
            itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);
        }
        getInventory().setItem(page.getSlot(), itemStack);
    }

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

    private void populatePredefined(Player player) {
        PREDEFINED_SLOTS.forEach((slot, worldType) -> addPredefinedWorldItem(
                player, slot, worldType, messages.getString(PREDEFINED_MESSAGE_KEYS.get(worldType), player)));
    }

    private void addPredefinedWorldItem(Player player, int position, BuildWorldType worldType, String displayName) {
        XMaterial material = plugin.getCustomizableIcons().getIcon(worldType);

        if (!canCreateType(player, worldType)) {
            material = XMaterial.BARRIER;
            displayName = "§c§m" + ChatColor.stripColor(displayName);
        }

        getInventory().setItem(position, InventoryUtils.createItem(material, displayName));
    }

    private static boolean canCreateType(Player player, BuildWorldType worldType) {
        return player.hasPermission(
                "buildsystem.create.type." + worldType.name().toLowerCase(Locale.ROOT));
    }

    private void populateGenerator(Player player) {
        plugin.getMenuItems().addGlassPane(player, getInventory(), 29);
        plugin.getMenuItems().addGlassPane(player, getInventory(), 30);
        plugin.getMenuItems().addGlassPane(player, getInventory(), 32);
        plugin.getMenuItems().addGlassPane(player, getInventory(), 33);
        getInventory()
                .setItem(
                        31,
                        InventoryUtils.createSkull(
                                messages.getString("create_generators_create_world", player),
                                Profileable.detect(SkullTextures.ADD_ITEM)));
    }

    private void populateTemplates(Player player) {
        getInventory()
                .setItem(
                        28,
                        InventoryUtils.createSkull(
                                messages.getString("gui_previous_page", player),
                                Profileable.detect(SkullTextures.PREVIOUS_PAGE)));
        getInventory()
                .setItem(
                        34,
                        InventoryUtils.createSkull(
                                messages.getString("gui_next_page", player),
                                Profileable.detect(SkullTextures.NEXT_PAGE)));

        // Listed once per menu instance so page flips do not repeat directory I/O on the main thread
        if (this.templateFiles == null) {
            this.templateFiles = FileUtils.resolve(plugin.getDataFolder(), "templates")
                    .toFile()
                    .listFiles((file) -> file.isDirectory() && !file.isHidden());
        }
        File[] templateFiles = this.templateFiles;

        this.numTemplates = templateFiles != null ? templateFiles.length : 0;

        plugin.getMenuItems().fillRange(player, getInventory(), 29, 34);

        if (numTemplates == 0) {
            ItemStack barrier =
                    InventoryUtils.createItem(XMaterial.BARRIER, messages.getString("create_no_templates", player));
            for (int i = 29; i <= 33; i++) {
                getInventory().setItem(i, barrier);
            }
            return;
        }

        if (templateFiles == null) {
            return;
        }

        this.templateSlots.clear();
        int startIndex = page() * MAX_TEMPLATES;
        for (int i = 0; i < MAX_TEMPLATES && startIndex + i < templateFiles.length; i++) {
            String rawTemplateName = templateFiles[startIndex + i].getName();
            int slot = 29 + i;
            this.templateSlots.put(slot, rawTemplateName);
            getInventory()
                    .setItem(
                            slot,
                            InventoryUtils.createItem(
                                    XMaterial.FILLED_MAP,
                                    messages.getString(
                                            "create_template", player, Map.entry("%template%", rawTemplateName))));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        Page newPage = Page.of(slot);
        if (newPage != null) {
            Visibility vis = createPrivateWorld ? Visibility.PRIVATE : Visibility.PUBLIC;
            new CreateMenu(plugin, newPage, vis, folder, player).open(player);
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return;
        }

        switch (currentPage) {
            case PREDEFINED: {
                BuildWorldType worldType = PREDEFINED_SLOTS.get(slot);
                if (worldType == null || !canCreateType(player, worldType)) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }

                worldService.startWorldNameInput(player, worldType, null, this.createPrivateWorld, this.folder);
                XSound.ENTITY_CHICKEN_EGG.play(player);
                break;
            }

            case GENERATOR: {
                if (slot == 31) {
                    worldService.startWorldNameInput(
                            player, BuildWorldType.CUSTOM, null, this.createPrivateWorld, this.folder);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                }
                break;
            }

            case TEMPLATES: {
                ItemStack itemStack = event.getCurrentItem();
                if (itemStack == null) {
                    return;
                }

                XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
                switch (xMaterial) {
                    case FILLED_MAP: {
                        // Template names are dynamic and cannot be pre-registered in plugin.yml, so default-allow is
                        // emulated: a template is permitted unless an admin has explicitly denied its specific node.
                        String rawTemplateName = this.templateSlots.get(slot);
                        String templateNode = "buildsystem.create.template." + rawTemplateName;
                        boolean allowed = rawTemplateName != null
                                && (!player.isPermissionSet(templateNode) || player.hasPermission(templateNode));
                        if (!allowed) {
                            XSound.ENTITY_ITEM_BREAK.play(player);
                            return;
                        }
                        this.worldService.startWorldNameInput(
                                player,
                                BuildWorldType.TEMPLATE,
                                itemStack.getItemMeta().getDisplayName(),
                                this.createPrivateWorld,
                                this.folder);
                        break;
                    }
                    case PLAYER_HEAD:
                        if (slot == 28) {
                            if (!previousPage(player, MAX_TEMPLATES)) {
                                return;
                            }
                        } else if (slot == 34) {
                            if (!nextPage(player, MAX_TEMPLATES)) {
                                return;
                            }
                        }
                        populateTemplates(player);
                        break;
                    default:
                        return;
                }
                break;
            }
        }
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
