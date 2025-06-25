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
package de.eintosti.buildsystem.world.creation;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.util.inventory.BuildSystemHolder;
import de.eintosti.buildsystem.util.inventory.BuildSystemInventory;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import de.eintosti.buildsystem.util.inventory.PaginatedInventory;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreateInventory extends PaginatedInventory {

    private static final int MAX_TEMPLATES = 5;

    private final BuildSystemPlugin plugin;
    private final WorldServiceImpl worldService;

    private int numTemplates = 0;
    private Visibility visibility;
    private Folder folder;
    private boolean createPrivateWorld;

    public CreateInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldService = plugin.getWorldService();
    }

    public void openInventory(Player player, Page page, Visibility visibility, @Nullable Folder folder) {
        this.visibility = visibility;
        this.createPrivateWorld = visibility == Visibility.PRIVATE;
        this.folder = folder;

        if (page == Page.TEMPLATES) {
            addTemplates(player, page);
            player.openInventory(inventories[getInvIndex(player)]);
        } else {
            player.openInventory(getBaseInventory(player, page));
        }
    }

    private void addTemplates(Player player, Page page) {
        File[] templateFiles = new File(plugin.getDataFolder() + File.separator + "templates").listFiles((file) -> file.isDirectory() && !file.isHidden());

        this.numTemplates = templateFiles != null ? templateFiles.length : 0;
        int numPages = calculateNumPages(numTemplates, MAX_TEMPLATES);

        int index = 0;
        Inventory inventory = getBaseInventory(player, page);
        this.inventories = new Inventory[numPages];
        this.inventories[index] = inventory;

        if (numTemplates == 0) {
            ItemStack barrier = InventoryUtils.createItem(XMaterial.BARRIER, Messages.getString("create_no_templates", player));
            for (int i = 29; i <= 33; i++) {
                inventory.setItem(i, barrier);
            }
            return;
        }

        if (templateFiles == null) {
            return;
        }

        int columnTemplate = 29, maxColumnTemplate = 33;
        for (File templateFile : templateFiles) {
            inventory.setItem(columnTemplate++,
                    InventoryUtils.createItem(XMaterial.FILLED_MAP, Messages.getString("create_template", player,
                            Map.entry("%template%", templateFile.getName()))
                    )
            );

            if (columnTemplate > maxColumnTemplate) {
                columnTemplate = 29;
                inventory = getBaseInventory(player, page);
                inventories[++index] = inventory;
            }
        }
    }

    private Inventory getBaseInventory(Player player, Page page) {
        Inventory inventory = new CreateInventoryHolder(this, player, page).getInventory();
        fillGuiWithGlass(player, inventory, page);

        addPageItem(inventory, page, Page.PREDEFINED, InventoryUtils.createSkull(Messages.getString("create_predefined_worlds", player), Profileable.detect("2cdc0feb7001e2c10fd5066e501b87e3d64793092b85a50c856d962f8be92c78")));
        addPageItem(inventory, page, Page.GENERATOR, InventoryUtils.createSkull(Messages.getString("create_generators", player), Profileable.detect("b2f79016cad84d1ae21609c4813782598e387961be13c15682752f126dce7a")));
        addPageItem(inventory, page, Page.TEMPLATES, InventoryUtils.createSkull(Messages.getString("create_templates", player), Profileable.detect("d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c")));

        switch (page) {
            case PREDEFINED:
                addPredefinedWorldItem(player, inventory, 29, BuildWorldType.NORMAL, Messages.getString("create_normal_world", player));
                addPredefinedWorldItem(player, inventory, 30, BuildWorldType.FLAT, Messages.getString("create_flat_world", player));
                addPredefinedWorldItem(player, inventory, 31, BuildWorldType.NETHER, Messages.getString("create_nether_world", player));
                addPredefinedWorldItem(player, inventory, 32, BuildWorldType.END, Messages.getString("create_end_world", player));
                addPredefinedWorldItem(player, inventory, 33, BuildWorldType.VOID, Messages.getString("create_void_world", player));
                break;
            case GENERATOR:
                inventory.setItem(31, InventoryUtils.createSkull(Messages.getString("create_generators_create_world", player), Profileable.detect("3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716")));
                break;
            case TEMPLATES:
                // Template stuff is done during inventory open
                break;
        }

        return inventory;
    }

    private void addPageItem(Inventory inventory, Page currentPage, Page page, ItemStack itemStack) {
        if (currentPage == page) {
            itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);
        }
        inventory.setItem(page.getSlot(), itemStack);
    }

    private void addPredefinedWorldItem(Player player, Inventory inventory, int position, BuildWorldType worldType, String displayName) {
        XMaterial material = plugin.getCustomizableIcons().getIcon(worldType);

        if (!player.hasPermission("buildsystem.create.type." + worldType.name().toLowerCase(Locale.ROOT))) {
            material = XMaterial.BARRIER;
            displayName = "§c§m" + ChatColor.stripColor(displayName);
        }

        inventory.setItem(position, InventoryUtils.createItem(material, displayName));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory, Page page) {
        for (int i = 0; i <= 28; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
        for (int i = 34; i <= 44; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }

        switch (page) {
            case GENERATOR:
                InventoryUtils.addGlassPane(player, inventory, 29);
                InventoryUtils.addGlassPane(player, inventory, 30);
                InventoryUtils.addGlassPane(player, inventory, 32);
                InventoryUtils.addGlassPane(player, inventory, 33);
                break;
            case TEMPLATES:
                inventory.setItem(38, InventoryUtils.createSkull(Messages.getString("gui_previous_page", player), Profileable.detect("f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2")));
                inventory.setItem(42, InventoryUtils.createSkull(Messages.getString("gui_next_page", player), Profileable.detect("d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158")));
                break;
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CreateInventoryHolder holder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        CreateInventory.Page newPage = Page.of(slot);
        if (newPage != null) {
            openInventory(player, newPage, this.visibility, this.folder);
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return;
        }

        switch (holder.getPage()) {
            case PREDEFINED: {
                BuildWorldType worldType = switch (slot) {
                    case 29 -> BuildWorldType.NORMAL;
                    case 30 -> BuildWorldType.FLAT;
                    case 31 -> BuildWorldType.NETHER;
                    case 32 -> BuildWorldType.END;
                    case 33 -> BuildWorldType.VOID;
                    default -> null;
                };

                if (worldType == null || !player.hasPermission("buildsystem.create.type." + worldType.name().toLowerCase(Locale.ROOT))) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }

                worldService.startWorldNameInput(player, worldType, null, this.createPrivateWorld, this.folder);
                XSound.ENTITY_CHICKEN_EGG.play(player);
                break;
            }

            case GENERATOR: {
                if (slot == 31) {
                    worldService.startWorldNameInput(player, BuildWorldType.CUSTOM, null, this.createPrivateWorld, this.folder);
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
                    case FILLED_MAP:
                        this.worldService.startWorldNameInput(player, BuildWorldType.TEMPLATE, itemStack.getItemMeta().getDisplayName(), this.createPrivateWorld, this.folder);
                        break;
                    case PLAYER_HEAD:
                        if (slot == 38 && !decrementInv(player, numTemplates, MAX_TEMPLATES)) {
                            return;
                        } else if (slot == 42 && !incrementInv(player, numTemplates, MAX_TEMPLATES)) {
                            return;
                        }
                        openInventory(player, CreateInventory.Page.TEMPLATES, this.visibility, this.folder);
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
        @Nullable
        public static Page of(int slot) {
            return Arrays.stream(values())
                    .filter(value -> value.slot == slot)
                    .findFirst()
                    .orElse(null);
        }

        public int getSlot() {
            return slot;
        }
    }

    private static class CreateInventoryHolder extends BuildSystemHolder {

        private final Page page;

        public CreateInventoryHolder(BuildSystemInventory inventory, Player player, @NotNull Page page) {
            super(inventory, 45, Messages.getString("create_title", player));
            this.page = page;
        }

        @NotNull
        public Page getPage() {
            return page;
        }
    }
}