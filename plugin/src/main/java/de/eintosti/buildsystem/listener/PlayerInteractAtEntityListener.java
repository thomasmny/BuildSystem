package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.World;
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
    private final static double MAX_HEIGHT = 2.074631929397583;
    private final static double MIN_HEIGHT = 1.4409877061843872;
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
        if (clickedPosition.getY() > MIN_HEIGHT && clickedPosition.getY() < MAX_HEIGHT) {
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
        World bukkitWorld = player.getWorld();
        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());

        if (buildWorld == null) return;
        if (buildWorld.getStatus() != WorldStatus.ARCHIVE) return;

        if (!plugin.buildPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
