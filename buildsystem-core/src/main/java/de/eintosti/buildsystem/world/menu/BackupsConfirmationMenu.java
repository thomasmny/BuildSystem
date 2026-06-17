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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.util.StringUtils;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackupsConfirmationMenu extends ButtonMenu<MenuButton> {

    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_CANCEL = 15;

    private final Backup backup;
    private final String dateFormat;

    public BackupsConfirmationMenu(BuildSystemPlugin plugin, Backup backup, Player player) {
        super(plugin.getMessages(), 27, plugin.getMessages().getString("restore_backup_title", player));
        this.backup = backup;
        this.dateFormat = plugin.getConfigService().current().settings().dateFormat();

        register(
                SLOT_CONFIRM,
                MenuButton.builder()
                        .render((p, inventory, slot) -> ItemBuilder.of(XMaterial.LIME_DYE)
                                .name(messages.getString("restore_backup_confirm_name", p))
                                .lore(messages.getStringList(
                                        "restore_backup_confirm_lore",
                                        p,
                                        Map.entry(
                                                "%timestamp%",
                                                StringUtils.formatTime(backup.creationTime(), dateFormat))))
                                .into(inventory, slot))
                        .onClick((p, event) -> {
                            p.closeInventory();
                            XSound.ENTITY_PLAYER_LEVELUP.play(p);
                            backup.owner().restoreBackup(backup, p);
                        })
                        .build());
        register(
                SLOT_CANCEL,
                MenuButton.builder()
                        .render((p, inventory, slot) -> ItemBuilder.of(XMaterial.RED_DYE)
                                .name(messages.getString("restore_backup_cancel_name", p))
                                .into(inventory, slot))
                        .onClick((p, event) -> {
                            p.closeInventory();
                            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(p);
                        })
                        .build());
    }

    @Override
    protected void populate(Player player) {
        for (int slot : new int[] {0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21}) {
            ItemBuilder.of(XMaterial.LIME_STAINED_GLASS_PANE).name("§a").into(getInventory(), slot);
        }
        for (int slot : new int[] {4, 13, 22}) {
            ItemBuilder.of(XMaterial.BLACK_STAINED_GLASS_PANE).name("§0").into(getInventory(), slot);
        }
        for (int slot : new int[] {5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26}) {
            ItemBuilder.of(XMaterial.RED_STAINED_GLASS_PANE).name("§c").into(getInventory(), slot);
        }

        renderButtons(player);
    }
}
