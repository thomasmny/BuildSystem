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
package de.eintosti.buildsystem.listener.settings;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.listener.settings.SettingInteractionListener.SettingHandler;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class IronDoorHandler implements SettingHandler {

    @Override
    public boolean takes(PlayerInteractEvent event, Block block, Settings settings) {
        if (event.getHand() != EquipmentSlot.HAND
                || event.getPlayer().isSneaking()
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || !settings.isOpenTrapDoors()) {
            return false;
        }

        XMaterial material = XMaterial.matchXMaterial(block.getType());
        return material == XMaterial.IRON_DOOR || material == XMaterial.IRON_TRAPDOOR;
    }

    @Override
    public void handle(PlayerInteractEvent event, Block block) {
        event.setCancelled(true);
        Openable openable = (Openable) block.getBlockData();
        openable.setOpen(!openable.isOpen());
        block.setBlockData(openable);
    }
}
