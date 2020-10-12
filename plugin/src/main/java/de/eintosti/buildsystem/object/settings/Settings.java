package de.eintosti.buildsystem.object.settings;

import de.eintosti.buildsystem.object.navigator.NavigatorType;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * @author einTosti
 */
public class Settings implements ConfigurationSerializable {
    private NavigatorType navigatorType;
    private Colour glassColor;
    private WorldSort worldSort;
    private boolean slabBreaking;
    private boolean noClip;
    private boolean trapDoor;
    private boolean nightVision;
    private boolean scoreboard;
    private boolean disableInteract;
    private boolean spawnTeleport;
    private boolean clearInventory;
    private boolean instantPlaceSigns;
    private boolean hidePlayers;
    private boolean placePlants;

    private BukkitTask scoreboardTask;

    public Settings() {
        this.navigatorType = NavigatorType.OLD;
        this.glassColor = Colour.BLACK;
        this.worldSort = WorldSort.NAME_A_TO_Z;
        this.slabBreaking = false;
        this.noClip = false;
        this.trapDoor = false;
        this.nightVision = false;
        this.scoreboard = true;
        this.disableInteract = false;
        this.spawnTeleport = true;
        this.clearInventory = false;
        this.instantPlaceSigns = false;
        this.hidePlayers = false;
        this.placePlants = false;
    }

    public Settings(NavigatorType navigatorType, Colour glassColor, WorldSort worldSort, boolean slabBreaking, boolean noClip,
                    boolean trapDoor, boolean nightVision, boolean scoreboard, boolean disableInteract, boolean spawnTeleport,
                    boolean clearInventory, boolean instantPlaceSigns, boolean hidePlayers, boolean placePlants) {
        this.navigatorType = navigatorType == null ? NavigatorType.OLD : navigatorType;
        this.glassColor = glassColor == null ? Colour.BLACK : glassColor;
        this.worldSort = worldSort;
        this.slabBreaking = slabBreaking;
        this.noClip = noClip;
        this.trapDoor = trapDoor;
        this.nightVision = nightVision;
        this.scoreboard = scoreboard;
        this.disableInteract = disableInteract;
        this.spawnTeleport = spawnTeleport;
        this.clearInventory = clearInventory;
        this.instantPlaceSigns = instantPlaceSigns;
        this.hidePlayers = hidePlayers;
        this.placePlants = placePlants;
    }

    public NavigatorType getNavigatorType() {
        return navigatorType;
    }

    public void setNavigatorType(NavigatorType navigatorType) {
        this.navigatorType = navigatorType;
    }

    public Colour getGlassColor() {
        return glassColor;
    }

    public void setGlassColor(Colour glassColor) {
        this.glassColor = glassColor;
    }

    public WorldSort getWorldSort() {
        return worldSort;
    }

    public void setWorldSort(WorldSort worldSort) {
        this.worldSort = worldSort;
    }

    public boolean isSlabBreaking() {
        return slabBreaking;
    }

    public void setSlabBreaking(boolean slabBreaking) {
        this.slabBreaking = slabBreaking;
    }

    public boolean isNoClip() {
        return noClip;
    }

    public void setNoClip(boolean noClip) {
        this.noClip = noClip;
    }

    public boolean isTrapDoor() {
        return trapDoor;
    }

    public void setTrapDoor(boolean trapDoor) {
        this.trapDoor = trapDoor;
    }

    public boolean isNightVision() {
        return nightVision;
    }

    public void setNightVision(boolean nightVision) {
        this.nightVision = nightVision;
    }

    public boolean isScoreboard() {
        return scoreboard;
    }

    public void setScoreboard(boolean scoreboard) {
        this.scoreboard = scoreboard;
    }

    public boolean isDisableInteract() {
        return disableInteract;
    }

    public void setDisableInteract(boolean disableInteract) {
        this.disableInteract = disableInteract;
    }

    public boolean isSpawnTeleport() {
        return spawnTeleport;
    }

    public void setSpawnTeleport(boolean spawnTeleport) {
        this.spawnTeleport = spawnTeleport;
    }

    public boolean isClearInventory() {
        return clearInventory;
    }

    public void setClearInventory(boolean clearInventory) {
        this.clearInventory = clearInventory;
    }

    public BukkitTask getScoreboardTask() {
        return scoreboardTask;
    }

    public void setScoreboardTask(BukkitTask scoreboardTask) {
        this.scoreboardTask = scoreboardTask;
    }

    public boolean isInstantPlaceSigns() {
        return instantPlaceSigns;
    }

    public void setInstantPlaceSigns(boolean instantPlaceSigns) {
        this.instantPlaceSigns = instantPlaceSigns;
    }

    public boolean isHidePlayers() {
        return hidePlayers;
    }

    public void setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
    }

    public boolean isPlacePlants() {
        return placePlants;
    }

    public void setPlacePlants(boolean placePlants) {
        this.placePlants = placePlants;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("type", getNavigatorType().toString());
        settings.put("glass", getGlassColor().toString());
        settings.put("world-sort", getWorldSort().toString());
        settings.put("slab-breaking", isSlabBreaking());
        settings.put("no-clip", isNoClip());
        settings.put("trapdoor", isTrapDoor());
        settings.put("nightvision", isNightVision());
        settings.put("scoreboard", isScoreboard());
        settings.put("disable-interact", isDisableInteract());
        settings.put("spawn-teleport", isSpawnTeleport());
        settings.put("clear-inventory", isClearInventory());
        settings.put("instant-place-signs", isInstantPlaceSigns());
        settings.put("hide-players", isHidePlayers());
        settings.put("place-plants", isPlacePlants());
        return settings;
    }
}
