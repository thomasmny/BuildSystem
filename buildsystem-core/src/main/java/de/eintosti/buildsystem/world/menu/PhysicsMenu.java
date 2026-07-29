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
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ButtonMenu;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.world.menu.EditMenuToggles.Toggle;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jspecify.annotations.NullMarked;

/**
 * The physics fine-control menu, reached by right-clicking the physics toggle in the {@link EditMenu}: the master
 * physics switch plus one toggle per {@link PhysicsCategory} exception that still runs while physics is disabled.
 * Every toggle is gated by the same {@code buildsystem.edit.physics} permission as the editor's physics button.
 */
@NullMarked
public class PhysicsMenu extends ButtonMenu<MenuButton> {

    private static final String PERMISSION = "buildsystem.edit.physics";

    private static final int MENU_SIZE = 45;
    private static final int SLOT_MASTER = 4;

    private static final Toggle MASTER_TOGGLE = new Toggle(
            XMaterial.SAND,
            PERMISSION,
            "worldeditor_physics_master_item",
            "worldeditor_physics_master_lore",
            WorldDataKey.PHYSICS);

    private static final Map<Integer, Toggle> CATEGORY_TOGGLES = Map.ofEntries(
            entry(12, categoryToggle(XMaterial.OBSERVER, "blockupdates", PhysicsCategory.BLOCK_UPDATES)),
            entry(13, categoryToggle(XMaterial.OAK_FENCE, "connections", PhysicsCategory.CONNECTIONS)),
            entry(14, categoryToggle(XMaterial.GRAVEL, "fallingblocks", PhysicsCategory.FALLING_BLOCKS)),
            entry(21, categoryToggle(XMaterial.WATER_BUCKET, "fluidflow", PhysicsCategory.FLUID_FLOW)),
            entry(22, categoryToggle(XMaterial.OAK_LEAVES, "leafdecay", PhysicsCategory.LEAF_DECAY)),
            entry(23, categoryToggle(XMaterial.WHEAT, "growth", PhysicsCategory.GROWTH)),
            entry(30, categoryToggle(XMaterial.VINE, "spreading", PhysicsCategory.SPREADING)),
            entry(31, categoryToggle(XMaterial.SNOW_BLOCK, "blockforming", PhysicsCategory.BLOCK_FORMING)),
            entry(32, categoryToggle(XMaterial.ICE, "blockfading", PhysicsCategory.BLOCK_FADING)));

    private final MenuItems menuItems;
    private final Menus menus;
    private final BuildWorld buildWorld;

    public PhysicsMenu(Messages messages, MenuItems menuItems, Menus menus, BuildWorld buildWorld, Player player) {
        super(messages, MENU_SIZE, messages.getString("worldeditor_physics_title", player));
        this.menuItems = menuItems;
        this.menus = menus;
        this.buildWorld = buildWorld;

        register(SLOT_MASTER, toggleButton(MASTER_TOGGLE));
        CATEGORY_TOGGLES.forEach((slot, toggle) -> register(slot, toggleButton(toggle)));
    }

    private static Toggle categoryToggle(XMaterial material, String messageName, PhysicsCategory category) {
        return new Toggle(
                material,
                PERMISSION,
                "worldeditor_physics_" + messageName + "_item",
                "worldeditor_physics_" + messageName + "_lore",
                category.key());
    }

    private MenuButton toggleButton(Toggle toggle) {
        return MenuButton.builder()
                .render((player, inventory, slot) ->
                        toggle.render(menuItems, buildWorld.getData(), player, inventory, slot))
                .onClick((player, event) -> {
                    if (requirePermission(player, PERMISSION)) {
                        toggle.flip(buildWorld.getData());
                        XSound.ENTITY_CHICKEN_EGG.play(player);
                    }
                    populate(player);
                })
                .build();
    }

    @Override
    protected void populate(Player player) {
        for (int i = 0; i < MENU_SIZE; i++) {
            if (i != SLOT_MASTER && !CATEGORY_TOGGLES.containsKey(i)) {
                menuItems.addGlassPane(player, getInventory(), i);
            }
        }
        renderButtons(player);
    }

    @Override
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {
        // Only the menu's own slots return to the editor; clicks in the player inventory are ignored.
        if (event.getRawSlot() < 0 || event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        XSound.BLOCK_CHEST_OPEN.play(player);
        menus.openEdit(buildWorld, player);
    }

    /**
     * {@return the slot &rarr; world-data key mapping this menu toggles} Exposed for the golden layout test.
     */
    Map<Integer, WorldDataKey<Boolean>> keyBySlot() {
        Map<Integer, WorldDataKey<Boolean>> keys = new HashMap<>();
        keys.put(SLOT_MASTER, MASTER_TOGGLE.key());
        CATEGORY_TOGGLES.forEach((slot, toggle) -> keys.put(slot, toggle.key()));
        return keys;
    }
}
