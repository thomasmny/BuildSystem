/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter.WorldsArgument;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SetItemSubCommand implements SubCommand {

    @Nullable
    private final BuildWorld buildWorld;

    public SetItemSubCommand(BuildSystemPlugin plugin, String worldName) {
        this.buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(worldName);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!WorldPermissionsImpl.of(buildWorld).canPerformCommand(player, getArgument().getPermission())) {
            Messages.sendPermissionError(player);
            return;
        }

        if (args.length > 2) {
            Messages.sendMessage(player, "worlds_setitem_usage");
            return;
        }

        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_setitem_unknown_world");
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            Messages.sendMessage(player, "worlds_setitem_hand_empty");
            return;
        }

        buildWorld.getData().material().set(XMaterial.matchXMaterial(itemStack));
        Messages.sendMessage(player, "worlds_setitem_set",
                Map.entry("%world%", buildWorld.getName())
        );
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.SET_ITEM;
    }
}