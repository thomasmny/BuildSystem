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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@code /explosions}, {@code /noai} and {@code /physics}: each flips one boolean of a build world.
 */
@NullMarked
public class WorldToggleCommand extends CommandBase {

    /**
     * What each label flips: the world data key, its permission and message prefix, whether activating clears the
     * value, and whether the label has an {@code all} form.
     */
    public enum Toggle {
        EXPLOSIONS("explosions", Permissions.EXPLOSIONS, WorldDataKey.EXPLOSIONS, false, false),
        /**
         * Activating {@code /noai} turns mob AI off, and the living entities already in the world follow at once.
         */
        NOAI("noai", Permissions.NOAI, WorldDataKey.MOB_AI, true, false) {
            @Override
            void afterToggle(World world, boolean mobAi) {
                world.getLivingEntities().forEach(entity -> entity.setAI(mobAi));
            }
        },
        /**
         * The only toggle with an {@code all} form, which turns physics on in every world the player may toggle it in.
         */
        PHYSICS("physics", Permissions.PHYSICS, WorldDataKey.PHYSICS, false, true);

        private final String label;
        private final String permission;
        private final WorldDataKey<Boolean> key;
        private final boolean inverted;
        private final boolean allowsAll;

        Toggle(String label, String permission, WorldDataKey<Boolean> key, boolean inverted, boolean allowsAll) {
            this.label = label;
            this.permission = permission;
            this.key = key;
            this.inverted = inverted;
            this.allowsAll = allowsAll;
        }

        void afterToggle(World world, boolean value) {}
    }

    private final Toggle toggle;
    private final WorldServiceImpl worldService;
    private final WorldStorageImpl worldStorage;

    public WorldToggleCommand(Messages messages, Logger logger, WorldServiceImpl worldService, Toggle toggle) {
        super(messages, logger, true);
        this.toggle = toggle;
        this.worldService = worldService;
        this.worldStorage = worldService.getWorldStorage();
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        String worldName = worldNameFromArgs(player, args, 0, worldService, toggle.permission);
        if (worldName == null) {
            return;
        }
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld != null && !buildWorld.getPermissions().canPerformCommand(player, toggle.permission)) {
            messages.sendPermissionError(player);
            return;
        }

        switch (args.length) {
            case 0 -> flip(player, player.getWorld());
            case 1 -> {
                if (toggle.allowsAll && args[0].equalsIgnoreCase("all") && !worldStorage.worldExists(worldName)) {
                    activateEverywhere(player);
                } else {
                    flip(player, WorldNames.bukkitWorld(worldName));
                }
            }
            default -> messages.sendMessage(player, toggle.label + "_usage");
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            Completions.addWorldNames(
                    args[0],
                    worldStorage,
                    world -> world.getPermissions().canPerformCommand(player, toggle.permission),
                    list);
        }
        return list;
    }

    private void activateEverywhere(Player player) {
        List<BuildWorld> permitted = worldStorage.getBuildWorlds().stream()
                .filter(world -> world.getPermissions().canPerformCommand(player, toggle.permission))
                .toList();
        if (permitted.isEmpty()) {
            messages.sendPermissionError(player);
            return;
        }

        permitted.forEach(world -> world.getData().set(toggle.key, !toggle.inverted));
        messages.sendMessage(player, toggle.label + "_activated_all");
    }

    private void flip(Player player, @Nullable World world) {
        if (world == null) {
            messages.sendMessage(player, toggle.label + "_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(world);
        if (buildWorld == null) {
            messages.sendMessage(player, toggle.label + "_world_not_imported");
            return;
        }

        WorldData worldData = buildWorld.getData();
        boolean value = !worldData.get(toggle.key);
        worldData.set(toggle.key, value);
        boolean activated = value != toggle.inverted;
        messages.sendMessage(
                player,
                toggle.label + (activated ? "_activated" : "_deactivated"),
                Placeholders.of("%world%", buildWorld.getName()));
        toggle.afterToggle(world, value);
    }
}
