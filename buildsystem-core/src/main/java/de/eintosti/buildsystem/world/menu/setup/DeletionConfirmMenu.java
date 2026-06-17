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
package de.eintosti.buildsystem.world.menu.setup;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * A reusable yes/no confirmation, used before a destructive action (e.g. deleting a custom status or category). Shows
 * the consequences in the centre and a confirm/cancel pair; either choice runs the supplied callback.
 */
@NullMarked
public class DeletionConfirmMenu extends ButtonMenu<MenuButton> {

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_CANCEL = 15;

    private final BuildSystemPlugin plugin;
    private final Runnable onCancel;

    public DeletionConfirmMenu(
            BuildSystemPlugin plugin,
            Player player,
            String infoName,
            List<String> infoLore,
            Runnable onConfirm,
            Runnable onCancel) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("setup_confirm_title", player));
        this.plugin = plugin;
        this.onCancel = onCancel;

        register(SLOT_CONFIRM, choiceButton(XMaterial.LIME_DYE, "setup_confirm_yes", onConfirm));
        register(SLOT_CANCEL, choiceButton(XMaterial.RED_DYE, "setup_confirm_no", onCancel));
        register(
                SLOT_INFO,
                MenuButton.builder()
                        .render((p, inventory, slot) -> ItemBuilder.of(XMaterial.PAPER)
                                .name(infoName)
                                .lore(infoLore)
                                .into(inventory, slot))
                        .build());
    }

    private MenuButton choiceButton(XMaterial material, String nameKey, Runnable action) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(material)
                        .name(messages.getString(nameKey, player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    action.run();
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillAll(player, getInventory());
        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        if (event.getRawSlot() < 0 || event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        XSound.BLOCK_CHEST_OPEN.play(player);
        onCancel.run();
    }
}
