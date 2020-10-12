package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

/**
 * @author einTosti
 */
public class PlayerInteractAtEntityListener implements Listener {
    private final static double MAX_HEIGH = 2.074631929397583;
    private final static double MIN_HEIGH = 1.4409877061843872;
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public PlayerInteractAtEntityListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        disableArchivedWorlds(player, event);
        if (!plugin.openNavigator.contains(player)) return;

        if (entity.getType() != EntityType.ARMOR_STAND) return;
        if (entity.getCustomName() == null) return;
        String customName = entity.getCustomName();
        if (!customName.contains(" × ")) return;
        event.setCancelled(true);

        Vector clickedPosition = event.getClickedPosition();
        if (clickedPosition.getY() > MIN_HEIGH && clickedPosition.getY() < MAX_HEIGH) {
            if (customName.startsWith(player.getName())) {
                String invType = customName.replace(player.getName() + " × ", "");

                switch (invType) {
                    case "§aWorld Navigator":
                        XSound.BLOCK_CHEST_OPEN.play(player);
                        plugin.getWorldsInventory().openInventory(player);
                        break;
                    case "§6World Archive":
                        XSound.BLOCK_CHEST_OPEN.play(player);
                        plugin.getArchiveInventory().openInventory(player);
                        break;
                    case "§bPrivate Worlds":
                        XSound.BLOCK_CHEST_OPEN.play(player);
                        plugin.getPrivateInventory().openInventory(player);
                        break;
                }
            }
        } else {
            ItemStack itemStack = player.getItemInHand();
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null) return;
            String displayName = itemMeta.getDisplayName();

            if (!displayName.equals(plugin.getString("barrier_item"))) return;

            event.setCancelled(true);
            plugin.getPlayerMoveListener().closeNavigator(player);
        }
    }

    private void disableArchivedWorlds(Player player, PlayerInteractAtEntityEvent event) {
        org.bukkit.World bukkitWorld = player.getWorld();
        World world = worldManager.getWorld(bukkitWorld.getName());

        if (world == null) return;
        if (world.getStatus() == WorldStatus.ARCHIVE) {
            if (!plugin.buildPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
