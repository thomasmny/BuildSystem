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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Lets a player assign one of the registered {@link BuildWorldStatus statuses} to a world. Each status is shown at the
 * slot the administrator gave it in the setup status editor, so the picker's appearance is fully configurable; a status
 * that is hidden or has no slot is omitted. Each entry is gated by its own permission.
 */
@NullMarked
public class StatusMenu extends ButtonMenu<MenuButton> {

    private final SettingsService settingsService;
    private final MenuItems menuItems;
    private final Menus menus;
    private final BuildWorld buildWorld;

    public StatusMenu(
            Messages messages,
            WorldStatusRegistry worldStatusRegistry,
            SettingsService settingsService,
            MenuItems menuItems,
            Menus menus,
            BuildWorld buildWorld,
            Player player) {
        super(
                messages,
                WorldStatusRegistryImpl.STATUS_MENU_SIZE,
                messages.getString("status_title", player, Map.entry("%world%", formatWorldName(buildWorld))));
        this.settingsService = settingsService;
        this.menuItems = menuItems;
        this.menus = menus;
        this.buildWorld = buildWorld;

        for (BuildWorldStatus status : worldStatusRegistry.getStatuses()) {
            int slot = status.getStatusSlot();
            if (!status.isShownInStatusMenu() || slot < 0 || slot >= WorldStatusRegistryImpl.STATUS_MENU_SIZE) {
                continue;
            }
            register(slot, statusButton(status));
        }
    }

    private static String formatWorldName(BuildWorld buildWorld) {
        String worldName = buildWorld.getName();
        if (worldName.length() > 17) {
            worldName = worldName.substring(0, 14) + "...";
        }
        return worldName;
    }

    private MenuButton statusButton(BuildWorldStatus status) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> {
                    XMaterial material = status.getIcon();
                    String displayName = ColorAPI.process(status.getStyledName());

                    if (!player.hasPermission(status.getPermission())) {
                        material = XMaterial.BARRIER;
                        displayName = "§c§m" + ChatColor.stripColor(displayName);
                    }

                    ItemBuilder.of(material)
                            .name(displayName)
                            .glow(buildWorld.getData().get(WorldDataKey.STATUS).equals(status))
                            .into(inventory, slot);
                })
                .onClick((player, event) -> {
                    if (!player.hasPermission(status.getPermission())) {
                        XSound.ENTITY_ITEM_BREAK.play(player);
                        return;
                    }

                    player.closeInventory();
                    buildWorld.getData().set(WorldDataKey.STATUS, status);
                    settingsService.forceUpdateSidebar(buildWorld);

                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    messages.sendMessage(
                            player,
                            "worlds_setstatus_set",
                            Map.entry("%world%", buildWorld.getName()),
                            Map.entry("%status%", ColorAPI.process(status.getStyledName())));
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        menuItems.fillAll(player, getInventory());
        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        if (event.getRawSlot() < 0 || event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        XSound.BLOCK_CHEST_OPEN.play(player);
        menus.openEdit(buildWorld, player);
    }
}
