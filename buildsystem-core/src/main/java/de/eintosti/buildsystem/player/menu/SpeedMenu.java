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
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SpeedMenu extends Menu {

    private static final int SLOT_SPEED_1 = 11;
    private static final int SLOT_SPEED_2 = 12;
    private static final int SLOT_SPEED_3 = 13;
    private static final int SLOT_SPEED_4 = 14;
    private static final int SLOT_SPEED_5 = 15;

    private static final String SKULL_SPEED_1 = "71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530";
    private static final String SKULL_SPEED_2 = "4cd9eeee883468881d83848a46bf3012485c23f75753b8fbe8487341419847";
    private static final String SKULL_SPEED_3 = "1d4eae13933860a6df5e8e955693b95a8c3b15c36b8b587532ac0996bc37e5";
    private static final String SKULL_SPEED_4 = "d2e78fb22424232dc27b81fbcb47fd24c1acf76098753f2d9c28598287db5";
    private static final String SKULL_SPEED_5 = "6d57e3bc88a65730e31a14e3f41e038a5ecf0891a6c243643b8e5476ae2";

    private final SettingsService settingsService;

    public SpeedMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("speed_title", player));
        this.settingsService = plugin.getSettingsService();
    }

    @Override
    protected void populate(Player player) {
        for (int i = 0; i <= 26; i++) {
            getInventory()
                    .setItem(i, ItemBuilder.glassPane(player, settingsService).build());
        }

        addSpeedItem(player, SLOT_SPEED_1, SKULL_SPEED_1, "speed_1");
        addSpeedItem(player, SLOT_SPEED_2, SKULL_SPEED_2, "speed_2");
        addSpeedItem(player, SLOT_SPEED_3, SKULL_SPEED_3, "speed_3");
        addSpeedItem(player, SLOT_SPEED_4, SKULL_SPEED_4, "speed_4");
        addSpeedItem(player, SLOT_SPEED_5, SKULL_SPEED_5, "speed_5");
    }

    private void addSpeedItem(Player player, int slot, String skullTexture, String nameKey) {
        getInventory()
                .setItem(
                        slot,
                        ItemBuilder.skull(Profileable.detect(skullTexture))
                                .name(messages.getString(nameKey, player))
                                .build());
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
            case SLOT_SPEED_1 -> setSpeed(player, 0.2f, 1);
            case SLOT_SPEED_2 -> setSpeed(player, 0.4f, 2);
            case SLOT_SPEED_3 -> setSpeed(player, 0.6f, 3);
            case SLOT_SPEED_4 -> setSpeed(player, 0.8f, 4);
            case SLOT_SPEED_5 -> setSpeed(player, 1.0f, 5);
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
