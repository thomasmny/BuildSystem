/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world;

import de.eintosti.buildsystem.api.world.Builder;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CraftBuilder implements Builder {

    private final UUID uuid;
    private String name;

    public CraftBuilder(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public CraftBuilder(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return uuid.toString() + "," + name;
    }
}