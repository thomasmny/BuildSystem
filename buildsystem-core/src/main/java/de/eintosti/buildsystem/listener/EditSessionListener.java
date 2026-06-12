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
package de.eintosti.buildsystem.listener;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy;
import de.eintosti.buildsystem.protection.WorldProtectionPolicy.Denial;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class EditSessionListener implements Listener {

    private final WorldStorageImpl worldStorage;
    private final WorldProtectionPolicy policy;

    public EditSessionListener(BuildSystemPlugin plugin) {
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        this.policy = new WorldProtectionPolicy();
        WorldEdit.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        Actor actor = event.getActor();
        if (actor == null || !actor.isPlayer()) {
            return;
        }

        Player player = Bukkit.getPlayer(actor.getName());
        if (player == null) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            return;
        }

        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
            return;
        }

        if (buildWorld.getPermissions().hasAdminPermission(player)) {
            buildWorld.getData().lastEdited().set(System.currentTimeMillis());
            return;
        }

        if (policy.checkArchive(player, buildWorld) == Denial.ARCHIVED) {
            event.setExtent(new NullExtent());
            return;
        }

        if (policy.checkBuilders(player, buildWorld) == Denial.NOT_A_BUILDER) {
            event.setExtent(new NullExtent());
            return;
        }

        buildWorld.getData().lastEdited().set(System.currentTimeMillis());
    }
}
