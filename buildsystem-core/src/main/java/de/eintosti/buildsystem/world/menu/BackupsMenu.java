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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackupsMenu extends ButtonMenu<MenuButton> {

    private static final int SLOT_INFO = 4;
    private static final int FIRST_BACKUP_SLOT = 9;
    private static final int MAX_BACKUPS = 18;

    private final BuildSystemPlugin plugin;
    private final BackupServiceImpl backupService;
    private final BuildWorld buildWorld;

    public BackupsMenu(BuildSystemPlugin plugin, BuildWorld buildWorld, Player player) {
        super(plugin.getMessages(), 36, plugin.getMessages().getString("backups_title", player));
        this.plugin = plugin;
        this.backupService = plugin.getBackupService();
        this.buildWorld = buildWorld;
    }

    @Override
    protected void populate(Player player) {
        plugin.getMenuItems().fillRange(player, getInventory(), 0, 9);

        ItemBuilder.of(XMaterial.OAK_HANGING_SIGN)
                .name(messages.getString("backups_information_name", player))
                .lore(messages.getStringList(
                        "backups_information_lore",
                        player,
                        Map.entry("%interval%", getBackupIntervalSeconds() / 60),
                        Map.entry("%remaining%", getDurationUntilBackup())))
                .into(getInventory(), SLOT_INFO);

        plugin.getMenuItems().fillRange(player, getInventory(), 27, 36);

        loadBackups(player);
    }

    /**
     * Loads the {@link Backup}s for the world and, once they arrive, registers a button per backup and renders them.
     * The backups load asynchronously, so the button registry is (re)built on the main thread in the completion
     * callback rather than at construction.
     *
     * @param player The player to display the backups to
     */
    private void loadBackups(Player player) {
        // The registry and inventory may only be touched on the main thread; hop back before mutating them.
        backupService
                .getProfile(buildWorld)
                .listBackups()
                .thenAccept(loaded -> Bukkit.getScheduler().runTask(plugin, () -> {
                    clearButtons();
                    for (int i = 0; i < loaded.size() && i < MAX_BACKUPS; i++) {
                        register(FIRST_BACKUP_SLOT + i, backupButton(loaded.get(i)));
                    }
                    renderButtons(player);
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger()
                            .log(Level.SEVERE, "Failed to list backups for world: " + buildWorld.getName(), throwable);
                    return null;
                });
    }

    private MenuButton backupButton(Backup backup) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.of(XMaterial.GRASS_BLOCK)
                        .name(messages.getString(
                                "backups_backup_name",
                                player,
                                Map.entry(
                                        "%timestamp%",
                                        StringUtils.formatTime(
                                                backup.creationTime(),
                                                plugin.getConfigService()
                                                        .current()
                                                        .settings()
                                                        .dateFormat()))))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    player.closeInventory();
                    new BackupsConfirmationMenu(plugin, backup, player).open(player);
                })
                .build();
    }

    private int getBackupIntervalSeconds() {
        return plugin.getConfigService().current().world().backup().autoBackup().interval();
    }

    private String getDurationUntilBackup() {
        int timeSinceBackup = buildWorld.getData().getTimeSinceBackup();
        int secondsRemaining = Math.max(0, getBackupIntervalSeconds() - timeSinceBackup);
        return "%02d:%02d".formatted(secondsRemaining / 60, secondsRemaining % 60);
    }
}
