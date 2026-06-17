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
package de.eintosti.buildsystem.menu;

import org.jspecify.annotations.NullMarked;

/**
 * Central registry for skull texture profile hashes that are shared across multiple menus.
 *
 * <p>Only textures used by 2+ menus belong here — single-use textures stay inline in the menu that owns them. To add a
 * texture, copy the profile hash from the skin (e.g. from minecraft-heads.com), give it a constant named after what it
 * depicts, and reference it via {@code Profileable.detect(SkullTextures.MY_TEXTURE)}.
 */
@NullMarked
public final class SkullTextures {

    /**
     * Right-pointing arrow used for the "next page" button.
     */
    public static final String NEXT_PAGE = "f32ca66056b72863e98f7f32bd7d94c7a0d796af691c9ac3a9136331352288f9";

    /**
     * Left-pointing arrow used for the "previous page" button.
     */
    public static final String PREVIOUS_PAGE = "86971dd881dbaf4fd6bcaa93614493c612f869641ed59d1c9363a3666a5fa6";

    /**
     * Green plus used for "create"/"add" actions (create world, add builder).
     */
    public static final String ADD_ITEM = "3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716";

    /**
     * Compass-style head used for the world navigator entry.
     */
    public static final String WORLD_NAVIGATOR = "d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622";

    /**
     * Chest/archive head used for the world archive entry.
     */
    public static final String WORLD_ARCHIVE = "7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8";

    /**
     * Redstone-cog head used for the navigator settings entry.
     */
    public static final String SETTINGS = "1cba7277fc895bf3b673694159864b83351a4d14717e476ebda1c3bf38fcf37";

    /**
     * Green-check head used for "confirm"/"done" actions.
     */
    public static final String CONFIRM = "a92e31ffb59c90ab08fc9dc1fe26802035a3a47c42fee63423bcdb4262ecb9b6";

    /**
     * Upward-pointing arrow head used for scrolling a list up.
     */
    public static final String SCROLL_UP = "3f46abad924b22372bc966a6d517d2f1b8b57fdd262b4e04f48352e683fff92";

    /**
     * Downward-pointing arrow head used for scrolling a list down.
     */
    public static final String SCROLL_DOWN = "be9ae7a4be65fcbaee65181389a2f7d47e2e326db59ea3eb789a92c85ea46";

    /**
     * Red bin/trash head used for "delete" drop targets.
     */
    public static final String DELETE = "6ffb912612a06e7ebf865be50ce9ff609195efd19bbe497c61e228c73ef7757";

    /**
     * Refresh/arrows head used for "reset to defaults" actions.
     */
    public static final String RESET = "816ea34a6a6ec5c051e6932f1c471b7012b298d38d179f1b487c413f51959cd4";

    /**
     * Head used for "remove"/"cancel" actions.
     */
    public static final String CANCEL = "MHF_TNT";

    private SkullTextures() {}
}
