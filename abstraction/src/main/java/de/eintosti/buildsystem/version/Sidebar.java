package de.eintosti.buildsystem.version;

import org.bukkit.entity.Player;

public interface Sidebar {
    void set(Player player, String... information);

    void remove(Player player);

    void update(Player player, boolean forceUpdate, String... information);
}

