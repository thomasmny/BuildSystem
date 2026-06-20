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
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.Map;
import java.util.stream.IntStream;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DeleteMenu extends ButtonMenu<MenuButton> {

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_CANCEL = 15;

    private final BuildWorld buildWorld;

    public DeleteMenu(Messages messages, WorldServiceImpl worldService, BuildWorld buildWorld, Player player) {
        super(messages, 27, messages.getString("delete_title", player));
        this.buildWorld = buildWorld;

        register(
                SLOT_CONFIRM,
                MenuButton.builder()
                        .render((p, inventory, slot) -> ItemBuilder.of(XMaterial.LIME_DYE)
                                .name(messages.getString("delete_world_confirm", p))
                                .into(inventory, slot))
                        .onClick((p, event) -> {
                            XSound.ENTITY_PLAYER_LEVELUP.play(p);
                            p.closeInventory();
                            worldService.deleteWorld(p, buildWorld);
                        })
                        .build());
        register(
                SLOT_CANCEL,
                MenuButton.builder()
                        .render((p, inventory, slot) -> ItemBuilder.of(XMaterial.RED_DYE)
                                .name(messages.getString("delete_world_cancel", p))
                                .into(inventory, slot))
                        .onClick((p, event) -> {
                            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(p);
                            p.closeInventory();
                            messages.sendMessage(
                                    p, "worlds_delete_canceled", Map.entry("%world%", buildWorld.getName()));
                        })
                        .build());
    }

    @Override
    protected void populate(Player player) {
        final int[] greenSlots = {0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21};
        final int[] blackSlots = {4, 22};
        final int[] redSlots = {5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26};

        IntStream.of(greenSlots)
                .forEach(slot -> ItemBuilder.of(XMaterial.LIME_STAINED_GLASS_PANE)
                        .name("§f")
                        .into(getInventory(), slot));
        IntStream.of(blackSlots)
                .forEach(slot -> ItemBuilder.of(XMaterial.BLACK_STAINED_GLASS_PANE)
                        .name("§f")
                        .into(getInventory(), slot));
        IntStream.of(redSlots)
                .forEach(slot -> ItemBuilder.of(XMaterial.RED_STAINED_GLASS_PANE)
                        .name("§f")
                        .into(getInventory(), slot));

        ItemBuilder.of(XMaterial.FILLED_MAP)
                .name(messages.getString("delete_world_name", player, Map.entry("%world%", buildWorld.getName())))
                .lore(messages.getStringList("delete_world_name_lore", player))
                .into(getInventory(), 13);

        renderButtons(player);
    }
}
