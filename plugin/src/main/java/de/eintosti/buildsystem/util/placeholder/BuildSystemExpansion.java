package de.eintosti.buildsystem.util.placeholder;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.World;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class BuildSystemExpansion extends PlaceholderExpansion {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public BuildSystemExpansion(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * For convenience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier() {
        return "buildsystem";
    }

    /**
     * This is the version of the expansion.
     * You don't have to use numbers, since it is set as a String.
     * <p>
     * For convenience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * We specify the value identifier in this method.
     * Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param player     A Player.
     * @param identifier A String containing the identifier/value.
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        String worldName = player.getWorld().getName();
        if (identifier.matches(".*_.*")) {
            String[] splitString = identifier.split("_");
            worldName = splitString[1];
            identifier = splitString[0];
        }

        World world = worldManager.getWorld(worldName);
        if (world == null) {
            return "-";
        }

        switch (identifier.toLowerCase()) {
            case "blockbreaking":
                return String.valueOf(world.isBlockBreaking());
            case "blockplacement":
                return String.valueOf(world.isBlockPlacement());
            case "builders":
                return plugin.getBuilders(world);
            case "buildersenabled":
                return String.valueOf(world.isBuilders());
            case "creation":
                return plugin.formatDate(world.getCreationDate());
            case "creator":
                return world.getCreator();
            case "creatorid":
                return String.valueOf(world.getCreatorId());
            case "explosions":
                return String.valueOf(world.isExplosions());
            case "loaded":
                return String.valueOf(world.isLoaded());
            case "material":
                return String.valueOf(world.getMaterial().parseMaterial());
            case "mobai":
                return String.valueOf(world.isMobAI());
            case "permission":
                return world.getPermission();
            case "private":
                return String.valueOf(world.isPrivate());
            case "project":
                return world.getProject();
            case "physics":
                return String.valueOf(world.isPhysics());
            case "spawn":
                return world.getCustomSpawn();
            case "status":
                return world.getStatusName();
            case "time":
                return plugin.getWorldTime(world);
            case "type":
                return world.getTypeName();
            case "world":
                return world.getName();
        }
        return null;
    }
}
