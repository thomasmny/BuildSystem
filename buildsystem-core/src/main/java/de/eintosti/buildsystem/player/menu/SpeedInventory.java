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
package de.eintosti.buildsystem.player.menu;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.menu.Menu;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SpeedInventory extends Menu {

    private final SettingsService settingsService;

    public SpeedInventory(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("speed_title", player));
        this.settingsService = plugin.getSettingsService();
    }

    @Override
    protected void populate(Player player) {
        for (int i = 0; i <= 26; i++) {
            getInventory().setItem(i, ItemBuilder.glassPane(player, settingsService).build());
        }

        getInventory().setItem(11, ItemBuilder.skull(Profileable.detect("71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530"))
                .name(messages.getString("speed_1", player)).build());
        getInventory().setItem(12, ItemBuilder.skull(Profileable.detect("4cd9eeee883468881d83848a46bf3012485c23f75753b8fbe8487341419847"))
                .name(messages.getString("speed_2", player)).build());
        getInventory().setItem(13, ItemBuilder.skull(Profileable.detect("1d4eae13933860a6df5e8e955693b95a8c3b15c36b8b587532ac0996bc37e5"))
                .name(messages.getString("speed_3", player)).build());
        getInventory().setItem(14, ItemBuilder.skull(Profileable.detect("d2e78fb22424232dc27b81fbcb47fd24c1acf76098753f2d9c28598287db5"))
                .name(messages.getString("speed_4", player)).build());
        getInventory().setItem(15, ItemBuilder.skull(Profileable.detect("6d57e3bc88a65730e31a14e3f41e038a5ecf0891a6c243643b8e5476ae2"))
                .name(messages.getString("speed_5", player)).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("buildsystem.speed")) {
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 11 -> setSpeed(player, 0.2f, 1);
            case 12 -> setSpeed(player, 0.4f, 2);
            case 13 -> setSpeed(player, 0.6f, 3);
            case 14 -> setSpeed(player, 0.8f, 4);
            case 15 -> setSpeed(player, 1.0f, 5);
            default -> {
                return;
            }
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.closeInventory();
    }

    private void setSpeed(Player player, float speed, int num) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            messages.sendMessage(player, "speed_set_flying", Map.entry("%speed%", num));
        } else {
            player.setWalkSpeed(speed);
            messages.sendMessage(player, "speed_set_walking", Map.entry("%speed%", num));
        }
    }
}
