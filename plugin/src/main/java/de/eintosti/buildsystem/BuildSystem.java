package de.eintosti.buildsystem;

import de.eintosti.buildsystem.command.*;
import de.eintosti.buildsystem.inventory.*;
import de.eintosti.buildsystem.listener.*;
import de.eintosti.buildsystem.manager.*;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.world.Builder;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.tabcomplete.*;
import de.eintosti.buildsystem.util.Messages;
import de.eintosti.buildsystem.util.bstats.Metrics;
import de.eintosti.buildsystem.util.external.UpdateChecker;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import de.eintosti.buildsystem.util.placeholder.BuildSystemExpansion;
import de.eintosti.buildsystem.version.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class BuildSystem extends JavaPlugin {
    public final static int PLUGIN_ID = 60441;
    public static BuildSystem plugin = null;

    public HashMap<UUID, World> selectedWorld;
    public HashMap<UUID, GameMode> buildPlayerGamemode;
    public HashMap<UUID, Float> playerWalkSpeed;
    public HashMap<UUID, Float> playerFlySpeed;

    public HashSet<Player> openNavigator;
    public HashSet<UUID> buildPlayers;

    private String prefix;
    private String version;
    private String dateFormat;
    private String timeUntilUnload;

    private boolean archiveVanish;
    private boolean scoreboard;
    private boolean spawnTeleportMessage;
    private boolean joinQuitMessages;
    private boolean unloadWorlds;
    private boolean voidBlock;
    private boolean updateChecker;
    private boolean blockWorldEditNonBuilder;
    private boolean creatorIsBuilder;

    private int sunriseTime;
    private int noonTime;
    private int nightTime;
    private int worldBorderSize;
    private int importDelay;

    private HashMap<String, String> defaultGameRules;

    private String scoreboardTitle;
    private List<String> scoreboardBody;

    private WorldsCommand worldsCommand;

    private ArmorStandManager armorStandManager;
    private InventoryManager inventoryManager;
    private NoClipManager noClipManager;
    private SettingsManager settingsManager;
    private SpawnManager spawnManager;
    private WorldManager worldManager;

    private ArchiveInventory archiveInventory;
    private BlocksInventory blocksInventory;
    private BuilderInventory builderInventory;
    private CreateInventory createInventory;
    private DeleteInventory deleteInventory;
    private DesignInventory designInventory;
    private EditInventory editInventory;
    private GameRuleInventory gameRuleInventory;
    private NavigatorInventory navigatorInventory;
    private PrivateInventory privateInventory;
    private SettingsInventory settingsInventory;
    private SetupInventory setupInventory;
    private SpeedInventory speedInventory;
    private StatusInventory statusInventory;
    private WorldsInventory worldsInventory;

    private PlayerMoveListener playerMoveListener;
    private PlayerTeleportListener playerTeleportListener;

    private CustomBlocks customBlocks;
    private GameRules gameRules;
    private Sidebar sidebar;
    private ManageEntityAI manageEntityAI;

    @Override
    public void onEnable() {
        plugin = this;

        createLanguageFile();
        createTemplateFolder();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        this.prefix = getPrefixString();
        this.scoreboardTitle = getString("title");
        this.scoreboardBody = getStringList("body");
        setConfigValues();

        this.selectedWorld = new HashMap<>();
        this.buildPlayerGamemode = new HashMap<>();
        this.playerWalkSpeed = new HashMap<>();
        this.playerFlySpeed = new HashMap<>();
        this.openNavigator = new HashSet<>();
        this.buildPlayers = new HashSet<>();

        getVersion();
        if (!setupCustomBlocks()) return;
        if (!setupGameRules()) return;
        if (!setupSidebar()) return;
        setupManageEntityAI();

        initClasses();
        registerCommands();
        registerTabCompleter();
        registerListeners();
        registerStats();
        registerPlaceholders();

        worldManager.load();
        settingsManager.load();
        spawnManager.load();

        Bukkit.getOnlinePlayers().forEach(pl -> {
            settingsManager.createSettings(pl);
            Settings settings = settingsManager.getSettings(pl);
            if (settings.isScoreboard()) {
                settingsManager.startScoreboard(pl);
            }
            if (settings.isNoClip()) {
                noClipManager.startNoClip(pl);
            }
        });

        if (isUpdateChecker()) {
            performUpdateCheck();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.RESET + "BuildSystem » Plugin " + ChatColor.GREEN + "enabled" + ChatColor.RESET + "!");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(pl -> {
            sidebar.remove(pl);
            noClipManager.stopNoClip(pl.getUniqueId());
            playerMoveListener.closeNavigator(pl);
        });

        reloadConfig();
        reloadConfigData(false);
        saveConfig();

        worldManager.save();
        settingsManager.save();
        spawnManager.save();
        inventoryManager.save();

        plugin = null;
        Bukkit.getConsoleSender().sendMessage(ChatColor.RESET + "BuildSystem » Plugin " + ChatColor.RED + "disabled" + ChatColor.RESET + "!");
    }

    private void initClasses() {
        this.armorStandManager = new ArmorStandManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.inventoryManager.loadTypes();
        this.inventoryManager.loadStatus();
        this.noClipManager = new NoClipManager(this);
        this.worldManager = new WorldManager(this);
        this.settingsManager = new SettingsManager(this);
        this.spawnManager = new SpawnManager(this);

        this.archiveInventory = new ArchiveInventory(this);
        this.blocksInventory = new BlocksInventory(this);
        this.builderInventory = new BuilderInventory(this);
        this.createInventory = new CreateInventory(this);
        this.deleteInventory = new DeleteInventory(this);
        this.designInventory = new DesignInventory(this);
        this.editInventory = new EditInventory(this);
        this.gameRuleInventory = new GameRuleInventory(this);
        this.navigatorInventory = new NavigatorInventory(this);
        this.privateInventory = new PrivateInventory(this);
        this.settingsInventory = new SettingsInventory(this);
        this.setupInventory = new SetupInventory(this);
        this.speedInventory = new SpeedInventory(this);
        this.statusInventory = new StatusInventory(this);
        this.worldsInventory = new WorldsInventory(this);
    }

    private void getVersion() {
        try {
            this.version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (ArrayIndexOutOfBoundsException e) {
            getLogger().log(Level.SEVERE, "Unknown server version");
        }
    }

    public boolean setupCustomBlocks() {
        switch (version) {
            case "v1_8_R1":
            case "v1_8_R2":
            case "v1_8_R3":
            case "v1_9_R1":
            case "v1_9_R2":
            case "v1_10_R1":
            case "v1_11_R1":
            case "v1_12_R1":
                this.customBlocks = new CustomBlock_1_12_R1(this);
                return true;
            case "v1_13_R1":
            case "v1_13_R2":
                this.customBlocks = new CustomBlock_1_13_R1(this);
                return true;
            case "v1_14_R1":
            case "v1_15_R1":
            case "v1_16_R1":
            case "v1_16_R2":
                this.customBlocks = new CustomBlocks_1_14_R1(this);
                return true;
            default:
                getLogger().log(Level.SEVERE, "\"CustomBlocks\" not found for version: " + version);
                getLogger().log(Level.SEVERE, "Please report this bug to einTosti with your server version");
                this.setEnabled(false);
                return false;
        }
    }

    public boolean setupGameRules() {
        switch (version) {
            case "v1_8_R1":
            case "v1_8_R2":
            case "v1_8_R3":
            case "v1_9_R1":
            case "v1_9_R2":
            case "v1_10_R1":
            case "v1_11_R1":
            case "v1_12_R1":
                this.gameRules = new GameRules_1_12_R1(
                        getString("worldeditor_gamerules_title"),
                        getStringList("worldeditor_gamerules_boolean_enabled"),
                        getStringList("worldeditor_gamerules_boolean_disabled"),
                        getStringList("worldeditor_gamerules_unknown"),
                        getStringList("worldeditor_gamerules_integer"));
                return true;
            case "v1_13_R1":
            case "v1_13_R2":
            case "v1_14_R1":
            case "v1_15_R1":
            case "v1_16_R1":
            case "v1_16_R2":
                this.gameRules = new GameRules_1_13_R1(
                        getString("worldeditor_gamerules_title"),
                        getStringList("worldeditor_gamerules_boolean_enabled"),
                        getStringList("worldeditor_gamerules_boolean_disabled"),
                        getStringList("worldeditor_gamerules_integer"));
                return true;
            default:
                getLogger().log(Level.SEVERE, "\"GameRules\" not found for version: " + version);
                getLogger().log(Level.SEVERE, "Please report this bug to einTosti with your server version");
                this.setEnabled(false);
                return false;
        }
    }

    public boolean setupSidebar() {
        switch (version) {
            case "v1_8_R1":
                this.sidebar = new Sidebar_1_8_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_8_R2":
                this.sidebar = new Sidebar_1_8_R2(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_8_R3":
                this.sidebar = new Sidebar_1_8_R3(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_9_R1":
                this.sidebar = new Sidebar_1_9_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_9_R2":
                this.sidebar = new Sidebar_1_9_R2(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_10_R1":
                this.sidebar = new Sidebar_1_10_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_11_R1":
                this.sidebar = new Sidebar_1_11_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_12_R1":
                this.sidebar = new Sidebar_1_12_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_13_R1":
                this.sidebar = new Sidebar_1_13_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_13_R2":
                this.sidebar = new Sidebar_1_13_R2(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_14_R1":
                this.sidebar = new Sidebar_1_14_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_15_R1":
                this.sidebar = new Sidebar_1_15_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_16_R1":
                this.sidebar = new Sidebar_1_16_R1(getScoreboardTitle(), getScoreboardBody());
                return true;
            case "v1_16_R2":
                this.sidebar = new Sidebar_1_16_R2(getScoreboardTitle(), getScoreboardBody());
                return true;
            default:
                getLogger().log(Level.SEVERE, "\"Sidebar\" not found for version: " + version);
                getLogger().log(Level.SEVERE, "Please report this bug to einTosti with your server version");
                this.setEnabled(false);
                return false;
        }
    }

    public void setupManageEntityAI() {
        switch (version) {
            case "v1_8_R1":
                this.manageEntityAI = new ManageEntityAI_1_8_R1();
                break;
            case "v1_8_R2":
                this.manageEntityAI = new ManageEntityAI_1_8_R2();
                break;
            case "v1_8_R3":
                this.manageEntityAI = new ManageEntityAI_1_8_R3();
                break;
            default:
                this.manageEntityAI = null;
                break;
        }
    }

    private void registerCommands() {
        new BackCommand(this);
        new BlocksCommand(this);
        new BuildCommand(this);
        new BuildSystemCommand(this);
        new ConfigCommand(this);
        new ExplosionsCommand(this);
        new GamemodeCommand(this);
        new NoAICommand(this);
        new PhysicsCommand(this);
        new SettingsCommand(this);
        new SetupCommand(this);
        new SkullCommand(this);
        new SpawnCommand(this);
        new SpeedCommand(this);
        new TimeCommand(this);
        new TopCommand(this);
        this.worldsCommand = new WorldsCommand(this);
    }

    private void registerTabCompleter() {
        new ConfigTabComplete(this);
        new EmptyTabComplete(this);
        new GamemodeTabComplete(this);
        new PhysicsTabComplete(this);
        new SpawnTabComplete(this);
        new SpeedTabComplete(this);
        new TimeTabComplete(this);
        new WorldsTabComplete(this);
    }

    private void registerListeners() {
        new AsyncPlayerChatListener(this);
        new BlockBreakListener(this);
        new BlockPhysicsListener(this);
        new BlockPlaceListener(this);
        new EntityDamageByEntityListener(this);
        new EntitySpawnListener(this);
        new FoodLevelChangeListener(this);
        new InventoryClickListener(this);
        new InventoryCloseListener(this);
        new PlayerArmorStandManipulateListener(this);
        new PlayerChangedWorldListener(this);
        new PlayerCommandPreprocessListener(this);
        new PlayerInteractListener(this);
        new PlayerInteractAtEntityListener(this);
        new PlayerJoinListener(this);
        this.playerMoveListener = new PlayerMoveListener(this);
        new PlayerQuitListener(this);
        new PlayerRespawnListener(this);
        this.playerTeleportListener = new PlayerTeleportListener(this);
        new SignChangeListener(this);
        new WeatherChangeListener(this);
    }

    private void registerStats() {
        int pluginId = 7427;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("scoreboard", () -> String.valueOf(scoreboard)));
        metrics.addCustomChart(new Metrics.SimplePie("archive_vanish", () -> String.valueOf(archiveVanish)));
        metrics.addCustomChart(new Metrics.SimplePie("join_quit_messages", () -> String.valueOf(joinQuitMessages)));
        metrics.addCustomChart(new Metrics.SimplePie("unload_worlds", () -> String.valueOf(unloadWorlds)));
        metrics.addCustomChart(new Metrics.SimplePie("void_block", () -> String.valueOf(voidBlock)));
        metrics.addCustomChart(new Metrics.SimplePie("update_checker", () -> String.valueOf(updateChecker)));
        metrics.addCustomChart(new Metrics.SimplePie("block_world_edit", () -> String.valueOf(blockWorldEditNonBuilder)));
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BuildSystemExpansion(this).register();
        }
    }

    private void performUpdateCheck() {
        UpdateChecker.init(this, PLUGIN_ID).requestUpdateCheck().whenComplete((result, e) -> {
                    if (result.requiresUpdate()) {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[BuildSystem] Great! a new update is available:" + ChatColor.GREEN + "v" + result.getNewestVersion());
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + " ➥ Your current version: " + ChatColor.RED + this.getDescription().getVersion());
                        return;
                    }

                    UpdateChecker.UpdateReason reason = result.getReason();
                    switch (reason) {
                        case COULD_NOT_CONNECT:
                        case INVALID_JSON:
                        case UNAUTHORIZED_QUERY:
                        case UNKNOWN_ERROR:
                        case UNSUPPORTED_VERSION_SCHEME:
                            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildSystem] Could not check for a new version of BuildSystem. Reason: " + reason);
                            break;
                    }
                }
        );
    }

    private void createTemplateFolder() {
        File templateFolder = new File(getDataFolder() + File.separator + "templates");
        if (templateFolder.mkdir()) {
            getLogger().log(Level.INFO, "Created \"templates\" folder");
        }
    }

    private void createLanguageFile() {
        if (getDataFolder().mkdir()) {
            getLogger().log(Level.INFO, "Created \"BuildSystem\" folder");
        }
        Messages.getInstance().createMessageFile();
    }

    private String getPrefixString() {
        String prefix = Messages.getInstance().messageData.get("prefix");
        try {
            return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : "§8× §bBuildSystem §8┃";
        } catch (NullPointerException e) {
            Messages.getInstance().createMessageFile();
            return getPrefixString();
        }
    }

    public String getString(String key) {
        try {
            return ChatColor.translateAlternateColorCodes('&', Messages.getInstance().messageData.get(key).replace("%prefix%", getPrefix()));
        } catch (NullPointerException e) {
            Messages.getInstance().createMessageFile();
            return getString(key);
        }
    }

    public List<String> getStringList(String key) {
        try {
            List<String> list = new ArrayList<>();
            String string = Messages.getInstance().messageData.get(key);
            String[] splitString = string.substring(1, string.length() - 1).split(", ");
            for (String s : splitString) {
                list.add(ChatColor.translateAlternateColorCodes('&', s.replace("%prefix%", getPrefix())));
            }
            return list;
        } catch (NullPointerException e) {
            Messages.getInstance().createMessageFile();
            return getStringList(key);
        }
    }

    public void sendPermissionMessage(Player player) {
        player.sendMessage(getString("no_permissions"));
    }

    public void reloadConfigData(boolean init) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            getSettingsManager().stopScoreboard(pl);
        }
        setConfigValues();

        if (init) {
            setupCustomBlocks();
            setupSidebar();
            if (isScoreboard()) {
                getSettingsManager().startScoreboard();
            } else {
                getSettingsManager().stopScoreboard();
            }
        }
    }


    private void setConfigValues() {
        // Messages
        this.spawnTeleportMessage = getConfig().getBoolean("messages.spawn-teleport-message", false);
        this.joinQuitMessages = getConfig().getBoolean("messages.join-quit-messages", true);
        this.dateFormat = getConfig().getString("messages.date-format", "dd/MM/yyyy");

        // Settings
        this.updateChecker = getConfig().getBoolean("settings.update-checker", true);
        this.scoreboard = getConfig().getBoolean("settings.scoreboard", true);
        this.archiveVanish = getConfig().getBoolean("settings.archive-vanish", true);

        this.blockWorldEditNonBuilder = getConfig().getBoolean("settings.builder.block-worldedit-non-builder", true);
        this.creatorIsBuilder = getConfig().getBoolean("settings.builder.creator-is-builder", true);

        // World
        this.sunriseTime = getConfig().getInt("world.default.time.sunrise", 0);
        this.noonTime = getConfig().getInt("world.default.time.noon", 6000);
        this.nightTime = getConfig().getInt("world.default.time.night", 18000);

        this.worldBorderSize = getConfig().getInt("world.default.worldborder.size", 6000000);

        this.unloadWorlds = getConfig().getBoolean("world.unload.enabled", false);
        this.timeUntilUnload = getConfig().getString("world.unload.time-until-unload", "01:00:00");

        this.voidBlock = getConfig().getBoolean("world.void-block", true);

        this.importDelay = getConfig().getInt("world.import-all.delay", 30);

        HashMap<String, String> defaultGameRules = new HashMap<>();
        ConfigurationSection configurationSection = getConfig().getConfigurationSection("world.default.gamerules");
        if (configurationSection != null) {
            for (Map.Entry<String, Object> entry : configurationSection.getValues(true).entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue().toString();
                defaultGameRules.put(name, value);
            }
        }
        this.defaultGameRules = defaultGameRules;
    }

    public void replaceItem(Player player, String findItemName, XMaterial findItemType, ItemStack replaceItem) {
        PlayerInventory playerInventory = player.getInventory();
        int slot = -1;
        for (int i = 0; i < playerInventory.getSize(); i++) {
            ItemStack currentItem = playerInventory.getItem(i);
            if (currentItem != null && currentItem.getType() == findItemType.parseMaterial()) {
                ItemMeta itemMeta = currentItem.getItemMeta();
                if (itemMeta != null) {
                    if (itemMeta.getDisplayName().equals(findItemName)) {
                        slot = i;
                    }
                }
            }
        }

        if (slot != -1) {
            playerInventory.setItem(slot, replaceItem);
        } else {
            ItemStack slot8 = playerInventory.getItem(8);
            if (slot8 == null || slot8.getType() == XMaterial.AIR.parseMaterial()) {
                playerInventory.setItem(8, replaceItem);
            } else {
                playerInventory.addItem(replaceItem);
            }
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public Boolean isArchiveVanish() {
        return archiveVanish;
    }

    public Boolean isScoreboard() {
        return scoreboard;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public boolean isSpawnTeleportMessage() {
        return spawnTeleportMessage;
    }

    public boolean isJoinQuitMessages() {
        return joinQuitMessages;
    }

    public boolean isUnloadWorlds() {
        return unloadWorlds;
    }

    public boolean isVoidBlock() {
        return voidBlock;
    }

    public boolean isUpdateChecker() {
        return updateChecker;
    }

    public boolean isBlockWorldEditNonBuilder() {
        return blockWorldEditNonBuilder;
    }

    public boolean isCreatorIsBuilder() {
        return creatorIsBuilder;
    }

    public int getSunriseTime() {
        return sunriseTime;
    }

    public int getNoonTime() {
        return noonTime;
    }

    public int getNightTime() {
        return nightTime;
    }

    public int getWorldBorderSize() {
        return worldBorderSize;
    }

    public HashMap<String, String> getDefaultGameRules() {
        return defaultGameRules;
    }

    public int getImportDelay() {
        return importDelay;
    }

    public long getTimeUntilUnload() {
        String[] timeArray = timeUntilUnload.split(":");
        int hours = Integer.parseInt(timeArray[0]);
        int minutes = Integer.parseInt(timeArray[1]);
        int seconds = Integer.parseInt(timeArray[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    public String getStatus(World world) {
        if (world == null) {
            return "§f-";
        }
        return world.getStatusName();
    }

    public String getPermission(World world) {
        if (world == null) {
            return "§f-";
        }
        return world.getPermission();
    }

    public String getProject(World world) {
        if (world == null) {
            return "§f-";
        }
        return world.getProject();
    }

    public String getCreator(World world) {
        if (world == null) {
            return "§f-";
        }
        return world.getCreator();
    }

    public String getWorldTime(World world) {
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
        if (bukkitWorld == null) {
            return "?";
        }
        return String.valueOf(bukkitWorld.getTime());
    }

    public String getCreationDate(World world) {
        if (world == null) {
            return "§f-";
        }
        return formatDate(world.getCreationDate());
    }

    public String formatDate(long date) {
        return date > 0 ? new SimpleDateFormat(plugin.getDateFormat()).format(date) : "-";
    }

    public String getBuilders(World world) {
        if (world == null) {
            return "§f-";
        }

        String template = plugin.getString("world_item_builders_builder_template");
        ArrayList<Builder> builders = new ArrayList<>();

        if (plugin.isCreatorIsBuilder()) {
            if (world.getCreator() != null && !world.getCreator().equals("-")) {
                builders.add(new Builder(world.getCreatorId(), world.getCreator()));
            }
        }
        builders.addAll(world.getBuilders());

        String string = "";
        if (builders.isEmpty()) {
            string = template.replace("%builder%", "-").trim();
        } else {
            for (Builder builder : builders) {
                string = string.concat(template.replace("%builder%", builder.getName()));
            }
            string = string.trim();
        }
        return string.substring(0, string.length() - 1);
    }

    public void forceUpdateSidebar(World world) {
        if (!isScoreboard()) return;
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
        if (bukkitWorld == null) return;
        bukkitWorld.getPlayers().forEach(this::forceUpdateSidebar);
    }

    public void forceUpdateSidebar(Player player) {
        if (!isScoreboard()) return;
        if (!settingsManager.getSettings(player).isScoreboard()) return;
        String worldName = player.getWorld().getName();
        World world = worldManager.getWorld(worldName);
        getSidebar().update(player, true, getStatus(world), getPermission(world), getProject(world), getCreator(world), getCreationDate(world));
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public List<String> getScoreboardBody() {
        return scoreboardBody;
    }

    public WorldsCommand getWorldsCommand() {
        return worldsCommand;
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public NoClipManager getNoClipManager() {
        return noClipManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public ArchiveInventory getArchiveInventory() {
        return archiveInventory;
    }

    public BlocksInventory getBlocksInventory() {
        return blocksInventory;
    }

    public BuilderInventory getBuilderInventory() {
        return builderInventory;
    }

    public CreateInventory getCreateInventory() {
        return createInventory;
    }

    public DeleteInventory getDeleteInventory() {
        return deleteInventory;
    }

    public DesignInventory getDesignInventory() {
        return designInventory;
    }

    public EditInventory getEditInventory() {
        return editInventory;
    }

    public GameRuleInventory getGameRuleInventory() {
        return gameRuleInventory;
    }

    public NavigatorInventory getNavigatorInventory() {
        return navigatorInventory;
    }

    public PrivateInventory getPrivateInventory() {
        return privateInventory;
    }

    public SettingsInventory getSettingsInventory() {
        return settingsInventory;
    }

    public SetupInventory getSetupInventory() {
        return setupInventory;
    }

    public SpeedInventory getSpeedInventory() {
        return speedInventory;
    }

    public StatusInventory getStatusInventory() {
        return statusInventory;
    }

    public WorldsInventory getWorldsInventory() {
        return worldsInventory;
    }

    public PlayerMoveListener getPlayerMoveListener() {
        return playerMoveListener;
    }

    public PlayerTeleportListener getPlayerTeleportListener() {
        return playerTeleportListener;
    }

    public CustomBlocks getCustomBlocks() {
        return customBlocks;
    }

    public GameRules getGameRules() {
        return gameRules;
    }

    public ManageEntityAI getManageEntityAI() {
        return manageEntityAI;
    }

    public Sidebar getSidebar() {
        return sidebar;
    }
}
