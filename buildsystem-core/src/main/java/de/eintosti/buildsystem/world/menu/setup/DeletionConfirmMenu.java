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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuContext;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.FeedbackSound;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * A reusable yes/no confirmation, used before a destructive action (e.g. deleting a custom status or category). Shows
 * the consequences in the centre and a confirm/cancel pair; either choice runs the supplied callback.
 *
 * <p>Only those two buttons act. A confirmation has to be deliberate, so nothing else in the menu is clickable — do not
 * add a filler or border shortcut here.
 */
@NullMarked
public class DeletionConfirmMenu extends ButtonMenu {

    private static final int INVENTORY_SIZE = 27;
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_INFO = 13;
    private static final int SLOT_CANCEL = 15;

    public DeletionConfirmMenu(
            MenuContext context,
            Player player,
            String infoName,
            List<String> infoLore,
            Runnable onConfirm,
            Runnable onCancel) {
        super(context, INVENTORY_SIZE, context.messages().getString("setup_confirm_title", player));

        register(SLOT_CONFIRM, createChoiceButton(SkullTextures.CONFIRM, "setup_confirm_yes", onConfirm));
        register(SLOT_CANCEL, createChoiceButton(SkullTextures.CANCEL, "setup_confirm_no", onCancel));
        register(SLOT_INFO, createInfoButton(infoName, infoLore));
    }

    private MenuButton createChoiceButton(String texture, String nameKey, Runnable action) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(Profileable.detect(texture))
                        .name(messages.getString(nameKey, player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    FeedbackSound.CLICK.play(player);
                    action.run();
                })
                .build();
    }

    private MenuButton createInfoButton(String infoName, List<String> infoLore) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(Material.PAPER)
                        .name(infoName)
                        .lore(infoLore)
                        .into(inventory, slot))
                .build();
    }

    @Override
    protected void populate(Player player) {
        menuItems.fillAll(player, getInventory());
        renderButtons(player);
    }
}
