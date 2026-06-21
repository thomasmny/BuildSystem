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
package de.eintosti.buildsystem.world.menu;

import static java.util.Map.entry;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.menu.MenuItems;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * The declarative catalog of {@link EditMenu}'s uniform toggle slots — those whose only action is "check permission,
 * flip a boolean world setting, re-open". Each {@link Toggle} owns its icon, permission, message keys, the world-data
 * getter/setter it flips, and how to render itself, so the menu just iterates the catalog to wire and draw them. The
 * heterogeneous slots (sub-menus, time, butcher, difficulty) are wired individually in the menu.
 */
@NullMarked
final class EditMenuToggles {

    static final int SLOT_PIN = 5;

    static final Map<Integer, Toggle> TOGGLES = Map.ofEntries(
            entry(
                    SLOT_PIN,
                    new Toggle(
                            XMaterial.ITEM_FRAME,
                            XMaterial.GLOW_ITEM_FRAME,
                            "buildsystem.edit.pin",
                            "worldeditor_pin_item",
                            "worldeditor_pin_lore",
                            WorldData::isPinned,
                            WorldData::setPinned)),
            entry(
                    20,
                    new Toggle(
                            XMaterial.OAK_PLANKS,
                            "buildsystem.edit.breaking",
                            "worldeditor_blockbreaking_item",
                            "worldeditor_blockbreaking_lore",
                            WorldData::isBlockBreaking,
                            WorldData::setBlockBreaking)),
            entry(
                    21,
                    new Toggle(
                            XMaterial.POLISHED_ANDESITE,
                            "buildsystem.edit.placement",
                            "worldeditor_blockplacement_item",
                            "worldeditor_blockplacement_lore",
                            WorldData::isBlockPlacement,
                            WorldData::setBlockPlacement)),
            entry(
                    22,
                    new Toggle(
                            XMaterial.SAND,
                            "buildsystem.edit.physics",
                            "worldeditor_physics_item",
                            "worldeditor_physics_lore",
                            WorldData::isPhysics,
                            WorldData::setPhysics)),
            entry(
                    24,
                    new Toggle(
                            XMaterial.TNT,
                            "buildsystem.edit.explosions",
                            "worldeditor_explosions_item",
                            "worldeditor_explosions_lore",
                            WorldData::isExplosions,
                            WorldData::setExplosions)),
            entry(
                    31,
                    new Toggle(
                            XMaterial.ARMOR_STAND,
                            "buildsystem.edit.mobai",
                            "worldeditor_mobai_item",
                            "worldeditor_mobai_lore",
                            WorldData::isMobAi,
                            WorldData::setMobAi)),
            entry(
                    33,
                    new Toggle(
                            XMaterial.TRIPWIRE_HOOK,
                            "buildsystem.edit.interactions",
                            "worldeditor_blockinteractions_item",
                            "worldeditor_blockinteractions_lore",
                            WorldData::isBlockInteractions,
                            WorldData::setBlockInteractions)));

    private EditMenuToggles() {}

    record Toggle(
            XMaterial material,
            XMaterial enabledMaterial,
            String permission,
            String itemKey,
            String loreKey,
            Predicate<WorldData> getter,
            BiConsumer<WorldData, Boolean> setter) {

        /**
         * Creates a toggle whose icon is the same whether the setting is enabled or not.
         */
        Toggle(
                XMaterial material,
                String permission,
                String itemKey,
                String loreKey,
                Predicate<WorldData> getter,
                BiConsumer<WorldData, Boolean> setter) {
            this(material, material, permission, itemKey, loreKey, getter, setter);
        }

        /**
         * Renders this toggle's icon into the slot, reflecting the live world-data state.
         */
        void render(MenuItems menuItems, WorldData worldData, Player player, Inventory inventory, int slot) {
            boolean enabled = getter.test(worldData);
            menuItems.addToggleItem(player, inventory, slot, iconFor(enabled), enabled, itemKey, loreKey);
        }

        private XMaterial iconFor(boolean enabled) {
            return enabled ? enabledMaterial : material;
        }
    }
}
