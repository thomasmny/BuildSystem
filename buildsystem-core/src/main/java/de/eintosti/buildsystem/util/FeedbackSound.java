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
package de.eintosti.buildsystem.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The sounds the plugin plays as feedback, one per kind of action, each with its own volume so that frequent sounds stay
 * quiet and a menu click never sounds like a fanfare. Every sound plays at normal pitch.
 */
@NullMarked
public enum FeedbackSound {
    CLICK(Sound.UI_BUTTON_CLICK, 0.4f),
    TOGGLE_ON(Sound.BLOCK_COPPER_BULB_TURN_ON, 0.6f),
    TOGGLE_OFF(Sound.BLOCK_COPPER_BULB_TURN_OFF, 0.6f),
    REFUSE(Sound.BLOCK_CRAFTER_FAIL, 1f),
    PAGE(Sound.ITEM_BOOK_PAGE_TURN, 1f),
    OPEN(Sound.BLOCK_CHEST_OPEN, 0.6f),
    CLOSE(Sound.BLOCK_CHEST_CLOSE, 0.6f),
    CONFIRM(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f),
    SUCCESS(Sound.ENTITY_PLAYER_LEVELUP, 1f),
    DELETE(Sound.ENTITY_ITEM_BREAK, 1f),
    REMOVE(Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1f),
    TELEPORT(Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f),
    PICK_UP(Sound.ITEM_BUNDLE_REMOVE_ONE, 1f),
    PUT_DOWN(Sound.ITEM_BUNDLE_INSERT, 1f),
    PROMPT(Sound.ITEM_BOOK_PUT, 1f),
    HOVER(Sound.ENTITY_CHICKEN_EGG, 0.5f);

    private final Sound sound;
    private final float volume;

    FeedbackSound(Sound sound, float volume) {
        this.sound = sound;
        this.volume = volume;
    }

    /**
     * {@return the sound for a toggle that is now in the given state}
     *
     * @param on Whether the toggle is on after the click
     */
    public static FeedbackSound toggle(boolean on) {
        return on ? TOGGLE_ON : TOGGLE_OFF;
    }

    /**
     * Plays this sound to the player only, at normal pitch.
     *
     * @param player The player to play the sound to
     */
    public void play(Player player) {
        player.playSound(player, sound, volume, 1f);
    }
}
