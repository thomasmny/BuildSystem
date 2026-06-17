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

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.player.menu.SettingsMenu;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class NavigatorMenu extends ButtonMenu<MenuButton> {

    private static final int SLOT_WORLDS = 11;
    private static final int SLOT_ARCHIVE = 12;
    private static final int SLOT_PRIVATE = 13;
    private static final int SLOT_SETTINGS = 15;

    private static final String SETTINGS_SKULL_PROFILE =
            "1cba7277fc895bf3b673694159864b83351a4d14717e476ebda1c3bf38fcf37";

    private final BuildSystemPlugin plugin;

    public NavigatorMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("old_navigator_title", player));
        this.plugin = plugin;

        register(
                SLOT_WORLDS,
                navButton(
                        Profileable.detect(SkullTextures.WORLD_NAVIGATOR),
                        "old_navigator_world_navigator",
                        p -> new PublicWorldsMenu(plugin, p).open(p)));
        register(
                SLOT_ARCHIVE,
                navButton(
                        Profileable.detect(SkullTextures.WORLD_ARCHIVE),
                        "old_navigator_world_archive",
                        p -> new ArchivedWorldsMenu(plugin, p).open(p)));
        register(
                SLOT_PRIVATE,
                navButton(
                        Profileable.of(player),
                        "old_navigator_private_worlds",
                        p -> new PrivateWorldsMenu(plugin, p).open(p)));
        register(
                SLOT_SETTINGS,
                MenuButton.builder()
                        .render((p, inventory, slot) -> ItemBuilder.skull(Profileable.detect(SETTINGS_SKULL_PROFILE))
                                .name(messages.getString("old_navigator_settings", p))
                                .into(inventory, slot))
                        .onClick((p, event) -> {
                            if (!p.hasPermission("buildsystem.settings")) {
                                XSound.ENTITY_ITEM_BREAK.play(p);
                                return;
                            }
                            new SettingsMenu(plugin, p).open(p);
                            XSound.ENTITY_CHICKEN_EGG.play(p);
                        })
                        .build());
    }

    private MenuButton navButton(Profileable icon, String nameKey, Consumer<Player> open) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(icon)
                        .name(messages.getString(nameKey, player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    open.accept(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillRange(player, getInventory(), 0, 27);
        renderButtons(player);
    }
}
