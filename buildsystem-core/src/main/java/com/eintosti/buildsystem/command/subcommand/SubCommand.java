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
public interface SubCommand {

    void execute(Player player, String[] args);
}