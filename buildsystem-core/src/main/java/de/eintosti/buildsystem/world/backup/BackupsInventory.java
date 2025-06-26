/*
 * Copyright (c) 2023-2025, Thomas Meaney
 * All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.eintosti.buildsystem.world.backup;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.config.Config.World;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.util.inventory.BuildSystemHolder;
import de.eintosti.buildsystem.util.inventory.BuildSystemInventory;
import de.eintosti.buildsystem.util.inventory.BuildWorldHolder;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class BackupsInventory extends BuildSystemInventory {

    private static final int FIRST_BACKUP_SLOT = 9;

    private final BackupService backupService;

    public BackupsInventory(BuildSystemPlugin plugin) {
        this.backupService = plugin.getBackupService();
    }

    public void openBackupsInventory(Player player, BuildWorld buildWorld) {
        player.openInventory(getBackupsInventory(player, buildWorld));
    }

    private Inventory getBackupsInventory(Player player, BuildWorld buildWorld) {
        List<Backup> backups = backupService.getProfile(buildWorld).listBackups().join();
        Inventory inventory = new BackupsHolder(this, buildWorld, player, backups).getInventory();

        for (int i = 0; i <= 8; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }

        inventory.setItem(4, InventoryUtils.createItem(XMaterial.OAK_HANGING_SIGN,
                Messages.getString("backups_information_name", player),
                Messages.getStringList("backups_information_lore", player,
                        Map.entry("%interval%", World.Backup.backupInterval / 60),
                        Map.entry("%remaining%", getDurationUntilBackup(buildWorld))
                )
        ));

        for (int i = 0; i < backups.size(); i++) {
            inventory.setItem(FIRST_BACKUP_SLOT + i, InventoryUtils.createItem(XMaterial.GRASS_BLOCK,
                    Messages.getString("backups_backup_name", player,
                            Map.entry("%timestamp%", StringUtils.formatTime(backups.get(i).creationTime()))
                    )
            ));
        }

        for (int i = 18; i <= 26; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }

        return inventory;
    }

    private String getDurationUntilBackup(BuildWorld buildWorld) {
        int timeUntilBackup = World.Backup.backupInterval;
        int timeSinceBackup = buildWorld.getData().timeSinceBackup().get();

        Date date = new Date((timeUntilBackup - timeSinceBackup) * 1000L);
        DateFormat formatter = new SimpleDateFormat("mm:ss");
        return formatter.format(date);
    }

    public void openConfirmationInventory(Player player, Backup backup) {
        player.openInventory(getConfirmationInventory(player, backup));
    }

    private Inventory getConfirmationInventory(Player player, Backup backup) {
        Inventory inventory = new ConfirmationHolder(this, backup, player).getInventory();

        for (int slot : new int[]{0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21}) {
            inventory.setItem(slot, InventoryUtils.createItem(XMaterial.LIME_STAINED_GLASS_PANE, "§a"));
        }
        for (int slot : new int[]{4, 13, 22}) {
            inventory.setItem(slot, InventoryUtils.createItem(XMaterial.BLACK_STAINED_GLASS_PANE, "§0"));
        }
        for (int slot : new int[]{5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26}) {
            inventory.setItem(slot, InventoryUtils.createItem(XMaterial.RED_STAINED_GLASS_PANE, "§c"));
        }

        inventory.setItem(11, InventoryUtils.createItem(XMaterial.LIME_DYE,
                Messages.getString("restore_backup_confirm_name", player),
                Messages.getStringList("restore_backup_confirm_lore", player,
                        Map.entry("%timestamp%", StringUtils.formatTime(backup.creationTime()))
                )
        ));
        inventory.setItem(15, InventoryUtils.createItem(XMaterial.RED_DYE,
                Messages.getString("restore_backup_cancel_name", player)
        ));
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof BackupsHolder backupsHolder) {
            event.setCancelled(true);

            int slot = event.getSlot();
            if (slot >= FIRST_BACKUP_SLOT && slot <= 17) {
                Backup backup = backupsHolder.getBackups().get(slot - FIRST_BACKUP_SLOT);
                if (backup == null) {
                    return;
                }

                player.closeInventory();
                openConfirmationInventory(player, backup);
            }
        }

        if (holder instanceof ConfirmationHolder confirmationHolder) {
            event.setCancelled(true);

            switch (event.getSlot()) {
                case 11 -> {
                    player.closeInventory();
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    Backup backup = confirmationHolder.getBackup();
                    backup.owner().restoreBackup(backup, player);
                }
                case 15 -> {
                    player.closeInventory();
                    player.playSound(player, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1f);
                }
            }
        }
    }

    private static class BackupsHolder extends BuildWorldHolder {

        private final List<Backup> backups;

        public BackupsHolder(BuildSystemInventory inventory, BuildWorld buildWorld, Player player, List<Backup> backups) {
            super(inventory, buildWorld, 27, Messages.getString("backups_title", player));
            this.backups = backups;
        }

        public List<Backup> getBackups() {
            return backups;
        }
    }

    private static class ConfirmationHolder extends BuildSystemHolder {

        private final Backup backup;

        public ConfirmationHolder(BuildSystemInventory inventory, Backup backup, Player player) {
            super(inventory, 27, Messages.getString("restore_backup_title", player));
            this.backup = backup;
        }

        public Backup getBackup() {
            return backup;
        }
    }
}
