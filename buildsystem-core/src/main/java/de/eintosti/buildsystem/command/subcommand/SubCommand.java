/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command.subcommand;

import org.bukkit.entity.Player;

public interface SubCommand {

    void execute(Player player, String[] args);

    Argument getArgument();

    default boolean hasPermission(Player player) {
        if (getArgument().getPermission() == null) {
            return true;
        }
        return player.hasPermission(getArgument().getPermission());
    }
}