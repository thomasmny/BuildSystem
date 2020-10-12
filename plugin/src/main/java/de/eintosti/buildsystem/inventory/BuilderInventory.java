package de.eintosti.buildsystem.inventory;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.object.world.Builder;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author einTosti
 */
public class BuilderInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    private final Map<UUID, Integer> invIndex;
    private int numBuilders;

    private Inventory[] inventories;

    public BuilderInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.invIndex = new HashMap<>();
    }

    private Inventory createInventory(World world, Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, plugin.getString("worldeditor_builders_title"));
        fillGuiWithGlass(inventory, player);

        addCreatorInfoItem(inventory, world);
        addBuilderAddItem(inventory, world, player);

        return inventory;
    }

    private void addCreatorInfoItem(Inventory inventory, World world) {
        if (world.getCreator() == null || world.getCreator().equalsIgnoreCase("-")) {
            inventoryManager.addItemStack(inventory, 4, XMaterial.BARRIER, plugin.getString("worldeditor_builders_no_creator_item"));
        } else {
            inventoryManager.addSkull(inventory, 4, plugin.getString("worldeditor_builders_creator_item"),
                    world.getCreator(), plugin.getString("worldeditor_builders_creator_lore").replace("%creator%", world.getCreator()));
        }
    }

    private void addBuilderAddItem(Inventory inventory, World world, Player player) {
        if ((world.getCreatorId() != null && world.getCreatorId().equals(player.getUniqueId()))
                || player.hasPermission("buildsystem.admin")) {
            inventoryManager.addUrlSkull(inventory, 22, plugin.getString("worldeditor_builders_add_builder_item"), "http://textures.minecraft.net/texture/3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 22);
        }
    }

    private void addItems(World world, Player player) {
        ArrayList<Builder> builders = world.getBuilders();
        this.numBuilders = builders.size();
        int numInventories = (numBuilders % 9 == 0 ? numBuilders : numBuilders + 1) != 0 ? (numBuilders % 9 == 0 ? numBuilders : numBuilders + 1) : 1;

        int index = 0;

        Inventory inventory = createInventory(world, player);

        inventories = new Inventory[numInventories];
        inventories[index] = inventory;

        int columnSkull = 9, maxColumnSkull = 17;
        for (Builder builder : builders) {
            String builderName = builder.getName();
            inventoryManager.addSkull(inventory, columnSkull++, plugin.getString("worldeditor_builders_builder_item").replace("%builder%", builderName),
                    builderName, plugin.getStringList("worldeditor_builders_builder_lore"));
            if (columnSkull > maxColumnSkull) {
                columnSkull = 9;
                inventory = createInventory(world, player);
                inventories[++index] = inventory;
            }
        }
    }

    public Inventory getInventory(World world, Player player) {
        addItems(world, player);
        if (getInvIndex(player) == null) {
            setInvIndex(player, 0);
        }
        return inventories[getInvIndex(player)];
    }

    private void fillGuiWithGlass(Inventory inventory, Player player) {
        for (int i = 0; i <= 8; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 19; i <= 25; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }

        int numOfPages = (numBuilders / 9) + (numBuilders % 9 == 0 ? 0 : 1);
        int invIndex = getInvIndex(player);

        if (numOfPages > 1 && invIndex > 0) {
            inventoryManager.addUrlSkull(inventory, 18, plugin.getString("gui_previous_page"), "http://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 18);
        }

        if (numOfPages > 1 && invIndex < (numOfPages - 1)) {
            inventoryManager.addUrlSkull(inventory, 26, plugin.getString("gui_next_page"), "http://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 26);
        }
    }

    public Integer getInvIndex(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!invIndex.containsKey(playerUUID)) {
            invIndex.put(playerUUID, 0);
        }
        return invIndex.get(playerUUID);
    }

    public void setInvIndex(Player player, int index) {
        invIndex.put(player.getUniqueId(), index);
    }

    public void incrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) + 1);
    }

    public void decrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) - 1);
    }
}
