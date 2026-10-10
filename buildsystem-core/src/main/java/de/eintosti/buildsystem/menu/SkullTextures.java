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
 * The skull texture profile hashes of every menu button.
 *
 * <p>To add a texture, copy the profile hash from the skin (e.g. from minecraft-heads.com), give it a constant named
 * after what it depicts or does, and reference it via {@code Profileable.detect(SkullTextures.MY_TEXTURE)}.
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
    public static final String RESET = "c49d271c5df84f8a3c8aa5d15427f62839341dab52c619a5987d38fbe18e464";

    /**
     * Green-check head used for "confirm"/"yes" actions.
     */
    public static final String CONFIRM = "a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756";

    /**
     * Red-cross head used for "cancel"/"no" actions.
     */
    public static final String CANCEL = "27548362a24c0fa8453e4d93e68c5969ddbde57bf6666c0319c1ed1e84d89065";

    /**
     * Head used for the navigator's "create folder" button.
     */
    public static final String CREATE_FOLDER = "69b861aabb316c4ed73b4e5428305782e735565ba2a053912e1efd834fa5a6f";

    /**
     * Head shown in the navigator when a category has no worlds to list.
     */
    public static final String NO_WORLDS = "2e3f50ba62cbda3ecf5479b62fedebd61d76589771cc19286bf2745cd71e47c6";

    /**
     * Head used for the create menu's predefined worlds tab.
     */
    public static final String PREDEFINED_WORLDS_TAB =
            "2cdc0feb7001e2c10fd5066e501b87e3d64793092b85a50c856d962f8be92c78";

    /**
     * Head used for the create menu's generators tab.
     */
    public static final String GENERATORS_TAB = "b2f79016cad84d1ae21609c4813782598e387961be13c15682752f126dce7a";

    /**
     * Head used for the create menu's templates tab.
     */
    public static final String TEMPLATES_TAB = "d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c";

    /**
     * Head used for speed 1, the slowest, in the speed menu.
     */
    public static final String SPEED_1 = "71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530";

    /**
     * Head used for speed 2 in the speed menu.
     */
    public static final String SPEED_2 = "4cd9eeee883468881d83848a46bf3012485c23f75753b8fbe8487341419847";

    /**
     * Head used for speed 3 in the speed menu.
     */
    public static final String SPEED_3 = "1d4eae13933860a6df5e8e955693b95a8c3b15c36b8b587532ac0996bc37e5";

    /**
     * Head used for speed 4 in the speed menu.
     */
    public static final String SPEED_4 = "d2e78fb22424232dc27b81fbcb47fd24c1acf76098753f2d9c28598287db5";

    /**
     * Head used for speed 5, the fastest, in the speed menu.
     */
    public static final String SPEED_5 = "6d57e3bc88a65730e31a14e3f41e038a5ecf0891a6c243643b8e5476ae2";

    private SkullTextures() {}
}
