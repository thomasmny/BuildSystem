package de.eintosti.buildsystem.inventory;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class EditInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public EditInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    private Inventory getInventory(Player player, World world) {
        Inventory inventory = Bukkit.createInventory(null, 54, plugin.getString("worldeditor_title"));
        fillGuiWithGlass(player, inventory);

        inventoryManager.addItemStack(inventory, 4, world.getMaterial(), plugin.getString("worldeditor_world_item").replace("%world%", world.getName()));

        addSettingsItem(inventory, 20, XMaterial.OAK_PLANKS, world.isBlockBreaking(), plugin.getString("worldeditor_blockbreaking_item"), plugin.getStringList("worldeditor_blockbreaking_lore"));
        addSettingsItem(inventory, 21, XMaterial.POLISHED_ANDESITE, world.isBlockPlacement(), plugin.getString("worldeditor_blockplacement_item"), plugin.getStringList("worldeditor_blockplacement_lore"));
        addSettingsItem(inventory, 22, XMaterial.SAND, world.isPhysics(), plugin.getString("worldeditor_physics_item"), plugin.getStringList("worldeditor_physics_lore"));
        addTimeItem(inventory, world);
        addSettingsItem(inventory, 24, XMaterial.TNT, world.isExplosions(), plugin.getString("worldeditor_explosions_item"), plugin.getStringList("worldeditor_explosions_lore"));
        inventoryManager.addItemStack(inventory, 29, XMaterial.DIAMOND_SWORD, plugin.getString("worldeditor_butcher_item"), plugin.getStringList("worldeditor_butcher_lore"));
        addBuildersItem(inventory, world, player);
        addSettingsItem(inventory, 31, XMaterial.ARMOR_STAND, world.isMobAI(), plugin.getString("worldeditor_mobai_item"), plugin.getStringList("worldeditor_mobai_lore"));
        addPrivateItem(inventory, world);
        addSettingsItem(inventory, 33, XMaterial.TRIPWIRE_HOOK, world.isBlockInteractions(), plugin.getString("worldeditor_blockinteractions_item"), plugin.getStringList("worldeditor_blockinteractions_lore"));
        inventoryManager.addItemStack(inventory, 38, XMaterial.FILLED_MAP, plugin.getString("worldeditor_gamerules_item"), plugin.getStringList("worldeditor_gamerules_lore"));
        inventoryManager.addItemStack(inventory, 39, inventoryManager.getStatusItem(world.getStatus()), plugin.getString("worldeditor_status_item"), getStatusLore(world));
        inventoryManager.addItemStack(inventory, 41, XMaterial.ANVIL, plugin.getString("worldeditor_project_item"), getProjectLore(world));
        inventoryManager.addItemStack(inventory, 42, XMaterial.PAPER, plugin.getString("worldeditor_permission_item"), getPermissionLore(world));

        return inventory;
    }

    public void openInventory(Player player, World world) {
        player.openInventory(getInventory(player, world));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addSettingsItem(Inventory inventory, int position, XMaterial material, boolean b, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
            itemMeta.setLore(lore);
            itemMeta.addItemFlags(ItemFlag.values());
        }
        itemStack.setItemMeta(itemMeta);
        if (b) itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        inventory.setItem(position, itemStack);
    }

    private void addTimeItem(Inventory inventory, World world) {
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());

        XMaterial xMaterial = XMaterial.WHITE_STAINED_GLASS;
        String value = plugin.getString("worldeditor_time_lore_unknown");
        World.Time time = getWorldTime(bukkitWorld);

        switch (time) {
            case SUNRISE:
                xMaterial = XMaterial.ORANGE_STAINED_GLASS;
                value = plugin.getString("worldeditor_time_lore_sunrise");
                break;
            case NOON:
                xMaterial = XMaterial.YELLOW_STAINED_GLASS;
                value = plugin.getString("worldeditor_time_lore_noon");
                break;
            case NIGHT:
                xMaterial = XMaterial.BLUE_STAINED_GLASS;
                value = plugin.getString("worldeditor_time_lore_night");
                break;
        }

        ArrayList<String> lore = new ArrayList<>();
        String finalValue = value;
        plugin.getStringList("worldeditor_time_lore").forEach(line -> lore.add(line.replace("%time%", finalValue)));

        inventoryManager.addItemStack(inventory, 23, xMaterial, plugin.getString("worldeditor_time_item"), lore);
    }

    public World.Time getWorldTime(org.bukkit.World bukkitWorld) {
        if (bukkitWorld == null) return World.Time.UNKNOWN;

        int worldTime = (int) bukkitWorld.getTime();
        int noonTime = plugin.getNoonTime();
        if (worldTime >= 0 && worldTime < noonTime) {
            return World.Time.SUNRISE;
        } else if (worldTime >= noonTime && worldTime < 13000) {
            return World.Time.NOON;
        } else {
            return World.Time.NIGHT;
        }
    }

    private void addBuildersItem(Inventory inventory, World world, Player player) {
        if ((world.getCreatorId() != null && world.getCreatorId().equals(player.getUniqueId()))
                || player.hasPermission("buildsystem.admin")) {
            addSettingsItem(inventory, 30, XMaterial.IRON_PICKAXE, world.isBuilders(), plugin.getString("worldeditor_builders_item"), plugin.getStringList("worldeditor_builders_lore"));
        } else {
            inventoryManager.addItemStack(inventory, 30, XMaterial.BARRIER, plugin.getString("worldeditor_builders_not_creator_item"), plugin.getStringList("worldeditor_builders_not_creator_lore"));
        }
    }

    private void addPrivateItem(Inventory inventory, World world) {
        XMaterial xMaterial = XMaterial.ENDER_EYE;
        List<String> lore = plugin.getStringList("worldeditor_visibility_lore_public");
        if (world.isPrivate()) {
            xMaterial = XMaterial.ENDER_PEARL;
            lore = plugin.getStringList("worldeditor_visibility_lore_private");
        }
        inventoryManager.addItemStack(inventory, 32, xMaterial, plugin.getString("worldeditor_visibility_item"), lore);
    }

    private List<String> getStatusLore(World world) {
        List<String> lore = new ArrayList<>();
        for (String s : plugin.getStringList("worldeditor_status_lore")) {
            lore.add(s.replace("%status%", world.getStatusName()));
        }
        return lore;
    }

    private List<String> getProjectLore(World world) {
        List<String> lore = new ArrayList<>();
        for (String s : plugin.getStringList("worldeditor_project_lore")) {
            lore.add(s.replace("%project%", world.getProject()));
        }
        return lore;
    }

    private List<String> getPermissionLore(World world) {
        List<String> lore = new ArrayList<>();
        for (String s : plugin.getStringList("worldeditor_permission_lore")) {
            lore.add(s.replace("%permission%", world.getPermission()));
        }
        return lore;
    }
}
