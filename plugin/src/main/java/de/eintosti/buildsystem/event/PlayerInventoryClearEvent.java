package de.eintosti.buildsystem.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;

/**
 * @author einTosti
 */
public class PlayerInventoryClearEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ArrayList<Integer> navigatorSlots;

    public PlayerInventoryClearEvent(Player player, ArrayList<Integer> navigatorSlots) {
        this.player = player;
        this.navigatorSlots = navigatorSlots;
    }

    public Player getPlayer() {
        return player;
    }

    public ArrayList<Integer> getNavigatorSlots() {
        return navigatorSlots;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
