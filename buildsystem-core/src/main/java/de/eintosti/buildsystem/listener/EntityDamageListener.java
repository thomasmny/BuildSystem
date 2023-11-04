package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.world.BuildWorld;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {

    private final BuildSystem plugin;

    public EntityDamageListener(BuildSystem plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void handle(EntityDamageEvent event) {

        if (event.getEntityType() != EntityType.PLAYER) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.VOID) {
            ConfigValues configValues = plugin.getConfigValues();

            // Is feature enabled
            if (!configValues.isSaveFromDeath()) return;

            // Is port pack to spawn enabled or use reset height to 100 on y
            boolean teleportToMapSpawn = configValues.isTeleportToMapSpawn();

            // Location field
            Location teleportLoc;

            Location location = event.getEntity().getLocation();
            Player   player   = (Player) event.getEntity();

            // Check if situation is true
            if (location.getY() < configValues.getMinYHeight()) {
                if (teleportToMapSpawn) {

                    BuildWorld buildWorld = plugin.getWorldManager().getBuildWorld(player.getWorld());

                    // Idk if this could be null but case handled :)
                    if (buildWorld == null) {
                        teleportLoc = extracted(player);
                        player.teleport(teleportLoc);
                        XSound.ENTITY_CHICKEN_EGG.play(player);
                        return;
                    }

                    boolean customSpawnExist = buildWorld.getData().customSpawn().get() != null;

                    if (customSpawnExist) {
                        String customSpawn   = buildWorld.getData().customSpawn().get();
                        String[] spawnString = customSpawn.split(";");
                        teleportLoc = new Location(
                            player.getWorld(), Double.parseDouble(spawnString[0]), Double.parseDouble(spawnString[1]),
                            Double.parseDouble(spawnString[2]), Float.parseFloat(spawnString[3]),
                            Float.parseFloat(spawnString[4])
                        );
                    } else {
                        teleportLoc = extracted(player);
                    }

                } else {
                    teleportLoc = extracted(player);
                }
                player.teleport(teleportLoc);
                XSound.ENTITY_CHICKEN_EGG.play(player);
            }
        }
    }

    /**
     * The extracted function takes a player and returns the location of that player,
     * but with the Y coordinate increased by 100.
     *
     * @param player player Get the location of the player
     * @return A location object, so you can use it as a return value
     */
    private Location extracted(Player player) {
        Location clone = player.getLocation().clone();
        clone.setY(player.getLocation().getY() + 100);
        return clone;
    }
}
