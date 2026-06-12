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
package de.eintosti.buildsystem.world.modification;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import java.util.Map;
import java.util.stream.IntStream;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DeleteInventory extends Menu {

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;

    public DeleteInventory(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("delete_title", player));
        this.plugin = plugin;
        this.buildWorld = buildWorld;
    }

    @Override
    protected void populate(Player player) {
        final int[] greenSlots = {0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21};
        final int[] blackSlots = {4, 22};
        final int[] redSlots = {5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26};

        IntStream.of(greenSlots).forEach(slot -> getInventory().setItem(slot, InventoryUtils.createItem(XMaterial.LIME_STAINED_GLASS_PANE, "§f")));
        IntStream.of(blackSlots).forEach(slot -> getInventory().setItem(slot, InventoryUtils.createItem(XMaterial.BLACK_STAINED_GLASS_PANE, "§f")));
        IntStream.of(redSlots).forEach(slot -> getInventory().setItem(slot, InventoryUtils.createItem(XMaterial.RED_STAINED_GLASS_PANE, "§f")));

        getInventory().setItem(11, InventoryUtils.createItem(XMaterial.LIME_DYE,
                messages.getString("delete_world_confirm", player))
        );
        getInventory().setItem(13, InventoryUtils.createItem(XMaterial.FILLED_MAP,
                messages.getString("delete_world_name", player, Map.entry("%world%", buildWorld.getName())),
                messages.getStringList("delete_world_name_lore", player)
        ));
        getInventory().setItem(15, InventoryUtils.createItem(XMaterial.RED_DYE,
                messages.getString("delete_world_cancel", player))
        );
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 11 -> {
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                player.closeInventory();
                plugin.getWorldService().deleteWorld(player, buildWorld);
            }
            case 15 -> {
                XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
                player.closeInventory();
                messages.sendMessage(player, "worlds_delete_canceled", Map.entry("%world%", buildWorld.getName()));
            }
        }
    }
}
