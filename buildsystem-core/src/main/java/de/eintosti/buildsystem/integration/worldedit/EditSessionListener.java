/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.integration.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

/**
 * Stops WorldEdit edits in build worlds the player may not build in, and records when a world was last edited.
 * FastAsyncWorldEdit fires {@link EditSessionEvent} off the main thread, so the decision only reads, and the
 * {@link WorldDataKey#LAST_EDITED} write is handed to the main thread.
 */
@NullMarked
public class EditSessionListener implements Listener {

    private final WorldStorageImpl worldStorage;
    private final TaskScheduler scheduler;
    private final WorldProtectionPolicy policy;

    public EditSessionListener(WorldStorageImpl worldStorage, TaskScheduler scheduler) {
        this.worldStorage = worldStorage;
        this.scheduler = scheduler;
        this.policy = new WorldProtectionPolicy();
        WorldEdit.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE || event.getWorld() == null) {
            return;
        }

        Actor actor = event.getActor();
        if (actor == null || !actor.isPlayer()) {
            return;
        }

        Player player = Bukkit.getPlayer(actor.getUniqueId());
        World world = Bukkit.getWorld(event.getWorld().getName());
        if (player == null || world == null) {
            return;
        }

        // The edited world, not the one the player stands in: //world can point WorldEdit elsewhere.
        BuildWorld buildWorld = worldStorage.getBuildWorld(world);
        if (buildWorld == null) {
            return;
        }

        if (policy.mayModify(player, buildWorld) != Denial.NONE) {
            event.setExtent(new NullExtent());
            return;
        }

        long editedAt = System.currentTimeMillis();
        scheduler.run(() -> buildWorld.getData().set(WorldDataKey.LAST_EDITED, editedAt));
    }
}
