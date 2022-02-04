/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.util.RBGUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/**
 * @author einTosti
 */
public class SignChangeListener implements Listener {

    public SignChangeListener(BuildSystem plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("buildsystem.color.sign")) {
            return;
        }

        for (int i = 0; i < event.getLines().length; i++) {
            String line = event.getLine(i);
            if (line != null) {
                String coloredLine = ChatColor.translateAlternateColorCodes('&', RBGUtils.color(line));
                event.setLine(i, coloredLine);
            }
        }
    }
}
