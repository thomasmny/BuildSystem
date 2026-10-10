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

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.Permissions;
import java.util.Map;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SpeedMenu extends ButtonMenu {

    record SpeedOption(String skullTexture, String nameKey, float speed, int displayNumber) {}

    /**
     * The single source of truth for the speed selection grid: each slot maps to the speed it sets. {@code speed} is the
     * raw flight/walk speed; {@code displayNumber} is the 1&ndash;5 value shown in the {@code %speed%} placeholder.
     */
    private static final Map<Integer, SpeedOption> SPEED_BY_SLOT = Map.ofEntries(
            Map.entry(11, new SpeedOption(SkullTextures.SPEED_1, "speed_1", 0.2f, 1)),
            Map.entry(12, new SpeedOption(SkullTextures.SPEED_2, "speed_2", 0.4f, 2)),
            Map.entry(13, new SpeedOption(SkullTextures.SPEED_3, "speed_3", 0.6f, 3)),
            Map.entry(14, new SpeedOption(SkullTextures.SPEED_4, "speed_4", 0.8f, 4)),
            Map.entry(15, new SpeedOption(SkullTextures.SPEED_5, "speed_5", 1.0f, 5)));

    private final SettingsService settingsService;

    public SpeedMenu(Messages messages, SettingsService settingsService, Player player) {
        super(messages, 27, messages.getString("speed_title", player));
        this.settingsService = settingsService;

        SPEED_BY_SLOT.forEach((slot, option) -> register(slot, speedButton(option)));
    }

    private MenuButton speedButton(SpeedOption option) {
        return MenuButton.builder()
                .permission(Permissions.SPEED)
                .render((player, inventory, slot) -> inventory.setItem(
                        slot,
                        ItemBuilder.skull(Profileable.detect(option.skullTexture()))
                                .name(messages.getString(option.nameKey(), player))
                                .build()))
                .onClick((player, event) -> {
                    setSpeed(player, option.speed(), option.displayNumber());
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
                    player.closeInventory();
                })
                .build();
    }

    /**
     * Closes without a message or sound, matching how this menu has always turned away a player who lacks
     * {@code buildsystem.speed}.
     */
    @Override
    protected void onPermissionDenied(Player player, InventoryClickEvent event) {
        player.closeInventory();
    }

    @Override
    protected void populate(Player player) {
        for (int i = 0; i <= 26; i++) {
            getInventory()
                    .setItem(
                            i,
                            ItemBuilder.of(MenuItems.glassPane(
                                            settingsService.getSettings(player).getDesignColor()))
                                    .name("§0")
                                    .build());
        }

        renderButtons(player);
    }

    private void setSpeed(Player player, float speed, int num) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            messages.sendMessage(player, "speed_set_flying", Placeholders.of("%speed%", num));
        } else {
            player.setWalkSpeed(speed);
            messages.sendMessage(player, "speed_set_walking", Placeholders.of("%speed%", num));
        }
    }
}
