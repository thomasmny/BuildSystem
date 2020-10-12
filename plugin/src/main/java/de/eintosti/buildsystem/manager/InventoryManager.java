package de.eintosti.buildsystem.manager;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.world.Builder;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldStatus;
import de.eintosti.buildsystem.object.world.WorldType;
import de.eintosti.buildsystem.util.config.SetupConfig;
import de.eintosti.buildsystem.util.external.ItemSkulls;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * @author einTosti
 */
public class InventoryManager {
    private final int version = Integer.parseInt(Bukkit.getServer()
            .getClass()
            .getPackage()
            .getName()
            .split("\\.")[3].replaceAll("[^0-9]", ""));
    private final BuildSystem plugin;
    private final SetupConfig setupConfig;

    private XMaterial normalCreateItem;
    private XMaterial flatCreateItem;
    private XMaterial netherCreateItem;
    private XMaterial endCreateItem;
    private XMaterial voidCreateItem;

    private XMaterial normalDefaultItem;
    private XMaterial flatDefaultItem;
    private XMaterial netherDefaultItem;
    private XMaterial endDefaultItem;
    private XMaterial voidDefaultItem;
    private XMaterial importedDefaultItem;

    private XMaterial notStartedItem;
    private XMaterial inProgressItem;
    private XMaterial almostFinishedItem;
    private XMaterial finishedItem;
    private XMaterial archivedItem;
    private XMaterial hiddenItem;

    public InventoryManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.setupConfig = new SetupConfig(plugin);
    }

    public ItemStack getItemStack(XMaterial material, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem(true);
        if (itemStack == null) itemStack = XMaterial.BEDROCK.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        itemMeta.addItemFlags(ItemFlag.values());

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getItemStack(XMaterial material, String displayName, String... lore) {
        return getItemStack(material, displayName, Arrays.asList(lore));
    }

    public void addItemStack(Inventory inventory, int position, XMaterial material, String displayName, List<String> lore) {
        ItemStack itemStack = getItemStack(material, displayName, lore);
        inventory.setItem(position, itemStack);
    }

    public void addItemStack(Inventory inventory, int position, XMaterial material, String displayName, String... lore) {
        addItemStack(inventory, position, material, displayName, Arrays.asList(lore));
    }

    public void addGlassPane(BuildSystem plugin, Player player, Inventory inventory, int position) {
        addItemStack(inventory, position, getColouredGlassPane(plugin, player), " ");
    }

    @SuppressWarnings("deprecation")
    public ItemStack getSkull(String displayName, String skullOwner, List<String> lore) {
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem(true);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setOwner(skullOwner);
        skullMeta.setDisplayName(displayName);
        skullMeta.setLore(lore);
        skull.setItemMeta(skullMeta);

        skull.setItemMeta(skullMeta);
        return skull;
    }

    public ItemStack getSkull(String displayName, String skullOwner, String... lore) {
        return getSkull(displayName, skullOwner, Arrays.asList(lore));
    }

    public void addSkull(Inventory inventory, int position, String displayName, String skullOwner, List<String> lore) {
        inventory.setItem(position, getSkull(displayName, skullOwner, lore));
    }

    public void addSkull(Inventory inventory, int position, String displayName, String skullOwner, String... lore) {
        addSkull(inventory, position, displayName, skullOwner, Arrays.asList(lore));
    }

    public ItemStack getUrlSkull(String displayName, String url, List<String> lore) {
        ItemStack skull = ItemSkulls.getSkull(url);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        skullMeta.setDisplayName(displayName);
        skullMeta.setLore(lore);
        skullMeta.addItemFlags(ItemFlag.values());
        skull.setItemMeta(skullMeta);

        return skull;
    }

    public ItemStack getUrlSkull(String displayName, String url, String... lore) {
        return getUrlSkull(displayName, url, Arrays.asList(lore));
    }

    public void addUrlSkull(Inventory inventory, int position, String displayName, String url, List<String> lore) {
        inventory.setItem(position, getUrlSkull(displayName, url, lore));
    }

    public void addUrlSkull(Inventory inventory, int position, String displayName, String url, String... lore) {
        addUrlSkull(inventory, position, displayName, url, Arrays.asList(lore));
    }

    public void addWorldItem(Player player, Inventory inventory, int position, World world) {
        ItemStack itemStack = world.getMaterial().parseItem(true);
        if (itemStack == null) itemStack = XMaterial.BEDROCK.parseItem();

        if (world.getMaterial() == XMaterial.PLAYER_HEAD) {
            addWorldSkull(player, inventory, position, world);
            return;
        } else {
            ItemMeta itemMeta = itemStack.getItemMeta();

            itemMeta.setDisplayName(plugin.getString("world_item_title").replace("%world%", world.getName()));
            itemMeta.setLore(getLore(player, world));
            itemMeta.addItemFlags(ItemFlag.values());

            itemStack.setItemMeta(itemMeta);
        }

        inventory.setItem(position, itemStack);
    }

    @SuppressWarnings("deprecation")
    public void addWorldSkull(Player player, Inventory inventory, int position, World world) {
        ItemStack itemStack = XMaterial.PLAYER_HEAD.parseItem(true);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

        skullMeta.setDisplayName(plugin.getString("world_item_title").replace("%world%", world.getName()));
        skullMeta.setLore(getLore(player, world));
        skullMeta.setOwner(world.getName());
        skullMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(skullMeta);

        inventory.setItem(position, itemStack);
    }

    public List<World> sortWorlds(Player player, WorldManager worldManager, BuildSystem plugin) {
        List<World> worlds = new ArrayList<>(worldManager.getWorlds());
        Settings settings = plugin.getSettingsManager().getSettings(player);
        switch (settings.getWorldSort()) {
            default: // NAME_A_TO_Z
                worlds.sort(Comparator.comparing(worldOne -> worldOne.getName().toLowerCase()));
                break;
            case NAME_Z_TO_A:
                worlds.sort(Comparator.comparing(worldOne -> worldOne.getName().toLowerCase()));
                Collections.reverse(worlds);
                break;
            case PROJECT_A_TO_Z:
                worlds.sort(Comparator.comparing(worldOne -> worldOne.getProject().toLowerCase()));
                break;
            case PROJECT_Z_TO_A:
                worlds.sort(Comparator.comparing(worldOne -> worldOne.getProject().toLowerCase()));
                Collections.reverse(worlds);
                break;
            case NEWEST_FIRST:
                worlds.sort(new CreationComparator().reversed());
                break;
            case OLDEST_FIRST:
                worlds.sort(new CreationComparator());
                break;
        }
        return worlds;
    }

    private List<String> getLore(Player player, World world) {
        List<String> messageList = player.hasPermission("buildsystem.edit") ? plugin.getStringList("world_item_lore_edit") :
                plugin.getStringList("world_item_lore_normal");
        List<String> lore = new ArrayList<>();
        for (String line : messageList) {
            String replace = line.replace("%project%", world.getProject())
                    .replace("%permission%", world.getPermission())
                    .replace("%status%", world.getStatusName())
                    .replace("%creator%", world.getCreator())
                    .replace("%creation%", plugin.formatDate(world.getCreationDate()));

            if (!line.contains("%builders%")) {
                lore.add(replace);
            } else {
                ArrayList<String> builders = formatBuilders(world);
                for (int i = 0; i < builders.size(); i++) {
                    String builderString = builders.get(i).trim();
                    if (builderString.isEmpty()) continue;

                    if (i == 0) {
                        builderString = line.replace("%builders%", builderString);
                    }
                    if (i == builders.size() - 1) {
                        builderString = builderString.substring(0, builderString.length() - 1);
                    }
                    lore.add(builderString);
                }
            }
        }
        return lore;
    }

    private ArrayList<String> formatBuilders(World world) {
        String template = plugin.getString("world_item_builders_builder_template");
        ArrayList<Builder> builders = new ArrayList<>();

        if (plugin.isCreatorIsBuilder()) {
            if (world.getCreator() != null && !world.getCreator().equals("-")) {
                builders.add(new Builder(world.getCreatorId(), world.getCreator()));
            }
        }
        builders.addAll(world.getBuilders());

        ArrayList<String> builderNames = new ArrayList<>();
        if (builders.isEmpty()) {
            String string = template.replace("%builder%", "-").trim();
            builderNames.add(string);
        } else {
            String string = "";
            int buildersInLine = 0;
            for (Builder builder : builders) {
                buildersInLine++;
                string = string.concat(template.replace("%builder%", builder.getName()));
                if (buildersInLine == 3) {
                    builderNames.add(string.trim());
                    buildersInLine = 0;
                    string = "";
                }
            }
            builderNames.add(string.trim());
        }

        builderNames.removeIf(String::isEmpty);
        return builderNames;
    }

    public void fillMultiInvWithGlass(BuildSystem plugin, Inventory inventory, Player player, Map<UUID, Integer> invIndex, int numOfPages) {
        for (int i = 0; i <= 8; i++) {
            addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 46; i <= 48; i++) {
            addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 50; i <= 52; i++) {
            addGlassPane(plugin, player, inventory, i);
        }

        if (numOfPages > 1 && invIndex.get(player.getUniqueId()) > 0) {
            addUrlSkull(inventory, 45, plugin.getString("gui_previous_page"), "http://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
        } else {
            addGlassPane(plugin, player, inventory, 45);
        }

        if (numOfPages > 1 && invIndex.get(player.getUniqueId()) < (numOfPages - 1)) {
            addUrlSkull(inventory, 53, plugin.getString("gui_next_page"), "http://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
        } else {
            addGlassPane(plugin, player, inventory, 53);
        }
    }

    public XMaterial getColouredGlass(BuildSystem plugin, Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        Settings settings = settingsManager.getSettings(player);
        switch (settings.getGlassColor()) {
            case RED:
                return XMaterial.RED_STAINED_GLASS;
            case ORANGE:
                return XMaterial.ORANGE_STAINED_GLASS;
            case YELLOW:
                return XMaterial.YELLOW_STAINED_GLASS;
            case PINK:
                return XMaterial.PINK_STAINED_GLASS;
            case MAGENTA:
                return XMaterial.MAGENTA_STAINED_GLASS;
            case PURPLE:
                return XMaterial.PURPLE_STAINED_GLASS;
            case BROWN:
                return XMaterial.BROWN_STAINED_GLASS;
            case LIME:
                return XMaterial.LIME_STAINED_GLASS;
            case GREEN:
                return XMaterial.GREEN_STAINED_GLASS;
            case BLUE:
                return XMaterial.BLUE_STAINED_GLASS;
            case CYAN:
                return XMaterial.CYAN_STAINED_GLASS;
            case LIGHT_BLUE:
                return XMaterial.LIGHT_BLUE_STAINED_GLASS;
            case WHITE:
                return XMaterial.WHITE_STAINED_GLASS;
            case LIGHT_GREY:
                return XMaterial.LIGHT_GRAY_STAINED_GLASS;
            case GREY:
                return XMaterial.GRAY_STAINED_GLASS;
            default:
                return XMaterial.BLACK_STAINED_GLASS;
        }
    }

    public XMaterial getColouredGlassPane(BuildSystem plugin, Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        Settings settings = settingsManager.getSettings(player);
        switch (settings.getGlassColor()) {
            case RED:
                return XMaterial.RED_STAINED_GLASS_PANE;
            case ORANGE:
                return XMaterial.ORANGE_STAINED_GLASS_PANE;
            case YELLOW:
                return XMaterial.YELLOW_STAINED_GLASS_PANE;
            case PINK:
                return XMaterial.PINK_STAINED_GLASS_PANE;
            case MAGENTA:
                return XMaterial.MAGENTA_STAINED_GLASS_PANE;
            case PURPLE:
                return XMaterial.PURPLE_STAINED_GLASS_PANE;
            case BROWN:
                return XMaterial.BROWN_STAINED_GLASS_PANE;
            case LIME:
                return XMaterial.LIME_STAINED_GLASS_PANE;
            case GREEN:
                return XMaterial.GREEN_STAINED_GLASS_PANE;
            case BLUE:
                return XMaterial.BLUE_STAINED_GLASS_PANE;
            case CYAN:
                return XMaterial.CYAN_STAINED_GLASS_PANE;
            case LIGHT_BLUE:
                return XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE;
            case WHITE:
                return XMaterial.WHITE_STAINED_GLASS_PANE;
            case LIGHT_GREY:
                return XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE;
            case GREY:
                return XMaterial.GRAY_STAINED_GLASS_PANE;
            default:
                return XMaterial.BLACK_STAINED_GLASS_PANE;
        }
    }

    public String selectedWorld(Player player) {
        World selectedWorld = plugin.selectedWorld.get(player.getUniqueId());
        if (selectedWorld == null) return null;

        String selectedWorldName = selectedWorld.getName();
        if (selectedWorldName.length() > 17) {
            selectedWorldName = selectedWorldName.substring(0, 14) + "...";
        }
        return selectedWorldName;
    }

    public XMaterial getCreateItem(WorldType worldType) {
        XMaterial material = null;
        switch (worldType) {
            case NORMAL:
                material = this.normalCreateItem;
                if (material == null) material = XMaterial.OAK_LOG;
                break;
            case FLAT:
                material = this.flatCreateItem;
                if (material == null) material = XMaterial.GRASS_BLOCK;
                break;
            case NETHER:
                material = this.netherCreateItem;
                if (material == null) material = XMaterial.NETHERRACK;
                break;
            case END:
                material = this.endCreateItem;
                if (material == null) material = XMaterial.END_STONE;
                break;
            case VOID:
                material = this.voidCreateItem;
                if (material == null) material = XMaterial.GLASS;
                break;
        }
        return material;
    }

    public void setCreateItem(WorldType worldType, XMaterial material) {
        switch (worldType) {
            case NORMAL:
                this.normalCreateItem = material;
                return;
            case FLAT:
                this.flatCreateItem = material;
                return;
            case NETHER:
                this.netherCreateItem = material;
                return;
            case END:
                this.endCreateItem = material;
                return;
            case VOID:
                this.voidCreateItem = material;
                break;
        }
    }

    public XMaterial getDefaultItem(WorldType worldType) {
        XMaterial material = null;
        switch (worldType) {
            case NORMAL:
                material = this.normalDefaultItem;
                if (material == null) material = XMaterial.OAK_LOG;
                break;
            case FLAT:
                material = this.flatDefaultItem;
                if (material == null) material = XMaterial.GRASS_BLOCK;
                break;
            case NETHER:
                material = this.netherDefaultItem;
                if (material == null) material = XMaterial.NETHERRACK;
                break;
            case END:
                material = this.endDefaultItem;
                if (material == null) material = XMaterial.END_STONE;
                break;
            case VOID:
                material = this.voidDefaultItem;
                if (material == null) material = XMaterial.GLASS;
                break;
            case IMPORTED:
                material = this.importedDefaultItem;
                if (material == null) material = XMaterial.FURNACE;
                break;
        }
        return material;
    }

    public void setDefaultItem(WorldType worldType, XMaterial material) {
        switch (worldType) {
            case NORMAL:
                this.normalDefaultItem = material;
                return;
            case FLAT:
                this.flatDefaultItem = material;
                return;
            case NETHER:
                this.netherDefaultItem = material;
                return;
            case END:
                this.endDefaultItem = material;
                return;
            case VOID:
                this.voidDefaultItem = material;
                return;
            case IMPORTED:
                this.importedDefaultItem = material;
                break;
        }
    }

    public XMaterial getStatusItem(WorldStatus worldStatus) {
        XMaterial material = null;
        switch (worldStatus) {
            case NOT_STARTED:
                material = this.notStartedItem;
                if (material == null) material = XMaterial.RED_DYE;
                break;
            case IN_PROGRESS:
                material = this.inProgressItem;
                if (material == null) material = XMaterial.ORANGE_DYE;
                break;
            case ALMOST_FINISHED:
                material = this.almostFinishedItem;
                if (material == null) material = XMaterial.LIME_DYE;
                break;
            case FINISHED:
                material = this.finishedItem;
                if (material == null) material = XMaterial.GREEN_DYE;
                break;
            case ARCHIVE:
                material = this.archivedItem;
                if (material == null) material = XMaterial.CYAN_DYE;
                break;
            case HIDDEN:
                material = this.hiddenItem;
                if (material == null) material = XMaterial.BONE_MEAL;
                break;
        }
        return material;
    }

    public void setStatusItem(WorldStatus worldStatus, XMaterial material) {
        switch (worldStatus) {
            case NOT_STARTED:
                this.notStartedItem = material;
                return;
            case IN_PROGRESS:
                this.inProgressItem = material;
                return;
            case ALMOST_FINISHED:
                this.almostFinishedItem = material;
                return;
            case FINISHED:
                this.finishedItem = material;
                return;
            case ARCHIVE:
                this.archivedItem = material;
                return;
            case HIDDEN:
                this.hiddenItem = material;
                break;
        }
    }

    public void save() {
        this.setupConfig.saveCreateItem(WorldType.NORMAL, getCreateItem(WorldType.NORMAL));
        this.setupConfig.saveCreateItem(WorldType.FLAT, getCreateItem(WorldType.FLAT));
        this.setupConfig.saveCreateItem(WorldType.NETHER, getCreateItem(WorldType.NETHER));
        this.setupConfig.saveCreateItem(WorldType.END, getCreateItem(WorldType.END));
        this.setupConfig.saveCreateItem(WorldType.VOID, getCreateItem(WorldType.VOID));

        this.setupConfig.saveDefaultItem(WorldType.NORMAL, getDefaultItem(WorldType.NORMAL));
        this.setupConfig.saveDefaultItem(WorldType.FLAT, getDefaultItem(WorldType.FLAT));
        this.setupConfig.saveDefaultItem(WorldType.NETHER, getDefaultItem(WorldType.NETHER));
        this.setupConfig.saveDefaultItem(WorldType.END, getDefaultItem(WorldType.END));
        this.setupConfig.saveDefaultItem(WorldType.VOID, getDefaultItem(WorldType.VOID));
        this.setupConfig.saveDefaultItem(WorldType.IMPORTED, getDefaultItem(WorldType.IMPORTED));

        this.setupConfig.saveStatusItem(WorldStatus.NOT_STARTED, getStatusItem(WorldStatus.NOT_STARTED));
        this.setupConfig.saveStatusItem(WorldStatus.IN_PROGRESS, getStatusItem(WorldStatus.IN_PROGRESS));
        this.setupConfig.saveStatusItem(WorldStatus.ALMOST_FINISHED, getStatusItem(WorldStatus.ALMOST_FINISHED));
        this.setupConfig.saveStatusItem(WorldStatus.FINISHED, getStatusItem(WorldStatus.FINISHED));
        this.setupConfig.saveStatusItem(WorldStatus.ARCHIVE, getStatusItem(WorldStatus.ARCHIVE));
        this.setupConfig.saveStatusItem(WorldStatus.HIDDEN, getStatusItem(WorldStatus.HIDDEN));
    }

    public void loadTypes() {
        FileConfiguration configuration = setupConfig.getFile();
        if (configuration == null) return;

        ConfigurationSection configurationSection = configuration.getConfigurationSection("setup.type");
        if (configurationSection == null) return;

        Set<String> worldTypes = configurationSection.getKeys(false);
        if (worldTypes.isEmpty()) return;

        for (String worldType : worldTypes) {
            String createMaterialString = configuration.getString("setup.type." + worldType + ".create");
            if (createMaterialString != null) {
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(createMaterialString);
                xMaterial.ifPresent(material -> setCreateItem(WorldType.valueOf(worldType.toUpperCase()), material));
            }

            String defaultMaterialString = configuration.getString("setup.type." + worldType + ".default");
            if (defaultMaterialString != null) {
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(defaultMaterialString);
                xMaterial.ifPresent(material -> setDefaultItem(WorldType.valueOf(worldType.toUpperCase()), material));
            }
        }
    }

    public void loadStatus() {
        FileConfiguration configuration = setupConfig.getFile();
        if (configuration == null) return;

        ConfigurationSection configurationSection = configuration.getConfigurationSection("setup.status");
        if (configurationSection == null) return;

        Set<String> worldStatus = configurationSection.getKeys(false);
        if (worldStatus.isEmpty()) return;

        for (String status : worldStatus) {
            String statusString = configuration.getString("setup.status." + status);
            if (statusString != null) {
                Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(statusString);
                xMaterial.ifPresent(material -> setStatusItem(WorldStatus.valueOf(status.toUpperCase()), material));
            }
        }
    }

    private static class CreationComparator implements Comparator<World> {
        @Override
        public int compare(World world1, World world2) {
            return Long.compare(world1.getCreationDate(), world2.getCreationDate());
        }
    }
}
