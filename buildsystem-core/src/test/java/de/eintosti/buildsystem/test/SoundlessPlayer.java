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
package de.eintosti.buildsystem.test;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * A {@link PlayerMock} that records the sounds played to it instead of playing them. It also accepts the seeded
 * {@code playSound} overload XSound plays through, which MockBukkit does not implement.
 */
@NullMarked
public class SoundlessPlayer extends PlayerMock {

    private final List<Sound> sounds = new ArrayList<>();

    public SoundlessPlayer(ServerMock server, String name) {
        super(server, name);
    }

    public static SoundlessPlayer join(ServerMock server, String name) {
        SoundlessPlayer player = new SoundlessPlayer(server, name);
        server.addPlayer(player);
        return player;
    }

    @Override
    public void playSound(
            Location location, Sound sound, SoundCategory category, float volume, float pitch, long seed) {
        sounds.add(sound);
    }

    @Override
    public void playSound(Entity entity, Sound sound, float volume, float pitch) {
        sounds.add(sound);
    }

    /**
     * {@return the sounds played to this player so far, oldest first}
     */
    public List<Sound> sounds() {
        return sounds;
    }
}
