package de.eintosti.buildsystem.object.world;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * @author einTosti
 */
public class Builder {
    private UUID uuid;
    private String name;

    public Builder(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public Builder(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return uuid.toString() + "," + name;
    }
}
