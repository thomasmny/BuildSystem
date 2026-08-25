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
package de.eintosti.buildsystem.command;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.util.Permissions;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SkullCommand extends CommandBase {

    private final MenuItems menuItems;

    public SkullCommand(Messages messages, Logger logger, MenuItems menuItems) {
        super(messages, logger, true);
        this.menuItems = menuItems;
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        if (!requirePermission(player, Permissions.SKULL)) {
            return;
        }

        switch (args.length) {
            case 0 ->
                addSkull(
                        player,
                        "§b" + player.getName(),
                        Profileable.of(player),
                        () -> messages.sendMessage(
                                player, "skull_player_received", Placeholders.of("%player%", player.getName())));
            case 1 -> {
                String identifier = args[0];
                if (identifier.length() > 16) {
                    addSkull(
                            player,
                            messages.getString("custom_skull_item", player),
                            Profileable.detect(identifier),
                            () -> messages.sendMessage(player, "skull_custom_received"));
                } else {
                    addSkull(
                            player,
                            "§b" + identifier,
                            Profileable.detect(identifier),
                            () -> messages.sendMessage(
                                    player, "skull_player_received", Placeholders.of("%player%", identifier)));
                }
            }
            default -> messages.sendMessage(player, "skull_usage");
        }
    }

    /**
     * Resolving a player skull asks Mojang for the skin, so the head is built off the main thread and handed over once
     * it arrives — the player may well have logged off by then. {@code onGiven} runs only when the head really lands in
     * the inventory, so the player is never told about a skull they did not get.
     */
    private void addSkull(Player player, String displayName, Profileable profileable, Runnable onGiven) {
        menuItems.resolveHeadAsync(profileable, null, displayName, List.of(), skull -> {
            if (player.isOnline() && player.getInventory().addItem(skull).isEmpty()) {
                onGiven.run();
            }
        });
    }
}
