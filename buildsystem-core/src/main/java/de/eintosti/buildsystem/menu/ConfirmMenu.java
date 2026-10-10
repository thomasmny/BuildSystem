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
package de.eintosti.buildsystem.menu;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.i18n.Messages;
import java.util.function.Consumer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A yes/no confirmation before a destructive action: green panes and the confirm item on the left, red panes and the
 * cancel item on the right, and an optional info item in the middle. Either choice closes the menu first.
 */
@NullMarked
public class ConfirmMenu extends ButtonMenu {

    private static final int[] GREEN_SLOTS = {0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21};
    private static final int[] RED_SLOTS = {5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26};
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_CANCEL = 15;

    private final @Nullable ItemStack info;

    /**
     * One side of the menu: the item shown and what clicking it does after the menu closes.
     *
     * @param item The item shown
     * @param action Runs with the clicking player
     */
    public record Choice(ItemStack item, Consumer<Player> action) {}

    /**
     * @param confirm The confirm item and action
     * @param confirmPermission The permission the confirm click needs, or {@code null} for none
     * @param cancel The cancel item and action
     * @param info The item shown in the middle, or {@code null} for a black pane
     */
    public ConfirmMenu(
            Messages messages,
            String title,
            Choice confirm,
            @Nullable String confirmPermission,
            Choice cancel,
            @Nullable ItemStack info) {
        super(messages, 27, title);
        this.info = info;

        register(
                SLOT_CONFIRM,
                MenuButton.builder()
                        .render((player, inventory, slot) -> inventory.setItem(slot, confirm.item()))
                        .permission(confirmPermission)
                        .onClick((player, event) -> {
                            player.closeInventory();
                            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                            confirm.action().accept(player);
                        })
                        .build());
        register(
                SLOT_CANCEL,
                MenuButton.builder()
                        .render((player, inventory, slot) -> inventory.setItem(slot, cancel.item()))
                        .onClick((player, event) -> {
                            player.closeInventory();
                            player.playSound(player, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1f);
                            cancel.action().accept(player);
                        })
                        .build());
    }

    @Override
    protected void populate(Player player) {
        for (int slot : GREEN_SLOTS) {
            ItemBuilder.of(XMaterial.LIME_STAINED_GLASS_PANE).name("§f").into(getInventory(), slot);
        }
        for (int slot : new int[] {4, SLOT_INFO, 22}) {
            ItemBuilder.of(XMaterial.BLACK_STAINED_GLASS_PANE).name("§f").into(getInventory(), slot);
        }
        for (int slot : RED_SLOTS) {
            ItemBuilder.of(XMaterial.RED_STAINED_GLASS_PANE).name("§f").into(getInventory(), slot);
        }
        if (info != null) {
            getInventory().setItem(SLOT_INFO, info);
        }

        renderButtons(player);
    }
}
