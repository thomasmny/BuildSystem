/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world;

import org.bukkit.entity.Player;

import java.util.UUID;

public class Builder {

    private final UUID uuid;
    private String name;

    public Builder(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public Builder(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return uuid.toString() + "," + name;
    }
}