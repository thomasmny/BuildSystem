/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.command.subcommand;

import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public abstract class SubCommand {

    private final Argument argument;

    public SubCommand(Argument argument) {
        this.argument = argument;
    }

    public abstract void execute(Player player, String[] args);

    public Argument getArgument() {
        return argument;
    }

    public boolean hasPermission(Player player) {
        if (argument.getPermission() == null) {
            return true;
        }
        return player.hasPermission(argument.getPermission());
    }
}