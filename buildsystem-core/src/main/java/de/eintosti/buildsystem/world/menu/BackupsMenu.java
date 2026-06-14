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
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BackupsMenu extends Menu {

    private static final int SLOT_INFO = 4;
    private static final int FIRST_BACKUP_SLOT = 9;
    private static final int MAX_BACKUPS = 18;

    private final BuildSystemPlugin plugin;
    private final BackupServiceImpl backupService;
    private final BuildWorld buildWorld;
    private final List<Backup> backups = new ArrayList<>();

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
     * Loads the {@link Backup} for the world and adds them to the inventory on completion.
     *
     * @param player The player to display the backups to
     */
    private void loadBackups(Player player) {
        // Inventory and the backing list may only be touched on the main thread; the click handler reads both.
        backupService
                .getProfile(buildWorld)
                .listBackups()
                .thenAccept(loaded -> Bukkit.getScheduler().runTask(plugin, () -> {
                    backups.clear();
                    backups.addAll(loaded);

                    for (int i = 0; i < loaded.size(); i++) {
                        ItemBuilder.of(XMaterial.GRASS_BLOCK)
                                .name(messages.getString(
                                        "backups_backup_name",
                                        player,
                                        Map.entry(
                                                "%timestamp%",
                                                StringUtils.formatTime(
                                                        loaded.get(i).creationTime(),
                                                        plugin.getConfigService()
                                                                .current()
                                                                .settings()
                                                                .dateFormat()))))
                                .into(getInventory(), FIRST_BACKUP_SLOT + i);
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger()
                            .log(Level.SEVERE, "Failed to list backups for world: " + buildWorld.getName(), throwable);
                    return null;
                });
    }

    private int getBackupIntervalSeconds() {
        return plugin.getConfigService().current().world().backup().autoBackup().interval();
    }

    private String getDurationUntilBackup() {
        int timeSinceBackup = buildWorld.getData().getTimeSinceBackup();
        int secondsRemaining = Math.max(0, getBackupIntervalSeconds() - timeSinceBackup);
        return "%02d:%02d".formatted(secondsRemaining / 60, secondsRemaining % 60);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        int backupIndex = slot - FIRST_BACKUP_SLOT;
        if (slot >= FIRST_BACKUP_SLOT
                && slot < FIRST_BACKUP_SLOT + MAX_BACKUPS
                && backupIndex < backups.size()
                && itemStack.getType() == XMaterial.GRASS_BLOCK.get()) {
            Backup backup = backups.get(backupIndex);
            player.closeInventory();
            new BackupsConfirmationMenu(plugin, backup, player).open(player);
        }
    }
}
