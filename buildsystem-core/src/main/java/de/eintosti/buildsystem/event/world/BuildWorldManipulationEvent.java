package de.eintosti.buildsystem.event.world;

import de.eintosti.buildsystem.world.BuildWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event shall reduce duplicated code.
 * <p>It will be called when</p>
 * <ul>
 *     <li>Breaking Blocks</li>
 *     <li>Placing Blocks</li>
 * </ul>
 */
public class BuildWorldManipulationEvent extends BuildWorldEvent implements Cancellable {

    private final Cancellable parentEvent;
    private final Player player;

    public BuildWorldManipulationEvent(Cancellable parentEvent, Player player, BuildWorld buildWorld) {
        super(buildWorld);
        this.parentEvent = parentEvent;
        this.player = player;
    }

    /**
     * @return whether the parent event has been cancelled
     */
    @Override
    public boolean isCancelled() {
        return parentEvent.isCancelled();
    }

    /**
     * @param cancelled true if the parent event should be cancelled.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        parentEvent.setCancelled(cancelled);
    }

    public Cancellable getParentEvent() {
        return parentEvent;
    }

    public Player getPlayer() {
        return player;
    }


}
