package de.eintosti.buildsystem.inventory;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SettingsManager;
import de.eintosti.buildsystem.object.navigator.NavigatorType;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * @author einTosti
 */
public class SettingsInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;

    public SettingsInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, plugin.getString("settings_title"));
        fillGuiWithGlass(player, inventory);

        Settings settings = settingsManager.getSettings(player);
        addDesignItem(inventory, player);
        addClearInventoryItem(inventory, player);
        addSettingsItem(inventory, 13, XMaterial.DIAMOND_AXE, settings.isDisableInteract(), plugin.getString("settings_disableinteract_item"), plugin.getStringList("settings_disableinteract_lore"));
        addSettingsItem(inventory, 14, XMaterial.ENDER_EYE, settings.isHidePlayers(), plugin.getString("settings_hideplayers_item"), plugin.getStringList("settings_hideplayers_lore"));
        addSettingsItem(inventory, 15, XMaterial.OAK_SIGN, settings.isInstantPlaceSigns(), plugin.getString("settings_instantplacesigns_item"), plugin.getStringList("settings_instantplacesigns_lore"));
        addSettingsItem(inventory, 20, XMaterial.SLIME_BLOCK, settings.isKeepNavigator(), plugin.getString("settings_keep_navigator_item"), plugin.getStringList("settings_keep_navigator_lore"));
        addSettingsItem(inventory, 21, plugin.getNavigatorItem(), settings.getNavigatorType().equals(NavigatorType.NEW), plugin.getString("settings_new_navigator_item"), plugin.getStringList("settings_new_navigator_lore"));
        addSettingsItem(inventory, 22, XMaterial.GOLDEN_CARROT, settings.isNightVision(), plugin.getString("settings_nightvision_item"), plugin.getStringList("settings_nightvision_lore"));
        addSettingsItem(inventory, 23, XMaterial.BRICKS, settings.isNoClip(), plugin.getString("settings_no_clip_item"), plugin.getStringList("settings_no_clip_lore"));
        addSettingsItem(inventory, 24, XMaterial.IRON_TRAPDOOR, settings.isTrapDoor(), plugin.getString("settings_open_trapdoors_item"), plugin.getStringList("settings_open_trapdoors_lore"));
        addSettingsItem(inventory, 29, XMaterial.FERN, settings.isPlacePlants(), plugin.getString("settings_placeplants_item"), plugin.getStringList("settings_placeplants_lore"));
        addSettingsItem(inventory, 30, XMaterial.PAPER, settings.isScoreboard(), plugin.isScoreboard() ? plugin.getString("settings_scoreboard_item") : plugin.getString("settings_scoreboard_disabled_item"),
                plugin.isScoreboard() ? plugin.getStringList("settings_scoreboard_lore") : plugin.getStringList("settings_scoreboard_disabled_lore"));
        addSettingsItem(inventory, 31, XMaterial.SMOOTH_STONE_SLAB, settings.isSlabBreaking(), plugin.getString("settings_slab_breaking_item"), plugin.getStringList("settings_slab_breaking_lore"));
        addSettingsItem(inventory, 32, XMaterial.MAGMA_CREAM, settings.isSpawnTeleport(), plugin.getString("settings_spawnteleport_item"), plugin.getStringList("settings_spawnteleport_lore"));
        addWorldSortItem(inventory, player);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
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

    private void addClearInventoryItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        XMaterial xMaterial = settings.isClearInventory() ? XMaterial.MINECART : XMaterial.CHEST_MINECART;
        addSettingsItem(inventory, 12, xMaterial, settings.isClearInventory(), plugin.getString("settings_clear_inventory_item"), plugin.getStringList("settings_clear_inventory_lore"));
    }

    private void addDesignItem(Inventory inventory, Player player) {
        ItemStack itemStack = inventoryManager.getItemStack(inventoryManager.getColouredGlass(plugin, player), plugin.getString("settings_change_design_item"));
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.setLore(plugin.getStringList("settings_change_design_lore"));
        }
        itemStack.setItemMeta(itemMeta);
        itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

        inventory.setItem(11, itemStack);
    }

    private void addWorldSortItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);

        String url;
        List<String> lore;
        switch (settings.getWorldSort()) {
            default: //NAME_A_TO_Z
                url = "http://textures.minecraft.net/texture/a67d813ae7ffe5be951a4f41f2aa619a5e3894e85ea5d4986f84949c63d7672e";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_name_az");
                break;
            case NAME_Z_TO_A:
                url = "http://textures.minecraft.net/texture/90582b9b5d97974b11461d63eced85f438a3eef5dc3279f9c47e1e38ea54ae8d";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_name_za");
                break;
            case PROJECT_A_TO_Z:
                url = "http://textures.minecraft.net/texture/2ac58b1a3b53b9481e317a1ea4fc5eed6bafca7a25e741a32e4e3c2841278c";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_project_az");
                break;
            case PROJECT_Z_TO_A:
                url = "http://textures.minecraft.net/texture/4e91200df1cae51acc071f85c7f7f5b8449d39bb32f363b0aa51dbc85d133e";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_project_za");
                break;
            case NEWEST_FIRST:
                url = "http://textures.minecraft.net/texture/71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530";
                lore = plugin.getStringList("settings_worldsort_lore_date_newest");
                break;
            case OLDEST_FIRST:
                url = "http://textures.minecraft.net/texture/e67caf7591b38e125a8017d58cfc6433bfaf84cd499d794f41d10bff2e5b840";
                lore = plugin.getStringList("settings_worldsort_lore_date_oldest");
                break;
        }

        ItemStack itemStack = inventoryManager.getUrlSkull(plugin.getString("settings_worldsort_item"), url);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setLore(lore);
        }
        itemStack.setItemMeta(itemMeta);
        inventory.setItem(33, itemStack);
    }
}
