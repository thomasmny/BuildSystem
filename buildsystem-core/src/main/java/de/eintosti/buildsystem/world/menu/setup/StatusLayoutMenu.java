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
package de.eintosti.buildsystem.world.menu.setup;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.data.WorldStatusImpl;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import de.eintosti.buildsystem.world.menu.SetupMenu;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The status setup, built as the exact counterpart of the {@link NavigatorLayoutMenu navigator layout editor}: the top
 * inventory is a live preview of the {@code /worlds setStatus} picker, and the player's own inventory is taken over (by
 * the shared {@link de.eintosti.buildsystem.navigator.NavigatorEditorService}, restored on close/quit/shutdown) to show
 * a back button, a create button, reset and delete controls, and the palette of statuses not currently in the picker.
 * Statuses are dragged into slots, dropped onto the bin to delete, and shift-clicked to open the
 * {@link StatusEditorMenu}.
 */
@NullMarked
public class StatusLayoutMenu extends Menu {

    private static final int PREVIEW_SIZE = WorldStatusRegistryImpl.STATUS_MENU_SIZE;

    private static final int BACK_SLOT = 0;
    private static final int CREATE_SLOT = 4;
    private static final int RESET_SLOT = 7;
    private static final int DELETE_SLOT = 8;
    private static final int PALETTE_FIRST_SLOT = 9;
    private static final int PALETTE_LAST_SLOT = 35;

    private final BuildSystemPlugin plugin;
    private final WorldStatusRegistryImpl registry;
    private final Held held = new Held();

    public StatusLayoutMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), PREVIEW_SIZE, plugin.getMessages().getString("setup_statuses_title", player));
        this.plugin = plugin;
        this.registry = plugin.getWorldStatusRegistry();
    }

    @Override
    public void open(Player player) {
        plugin.getNavigatorEditorService().beginSession(player);
        populate(player);
        player.openInventory(getInventory());
        renderControls(player);
    }

    @Override
    protected void populate(Player player) {
        Inventory inventory = getInventory();
        for (int slot = 0; slot < PREVIEW_SIZE; slot++) {
            plugin.getMenuItems().addGlassPane(player, inventory, slot);
        }

        for (BuildWorldStatus status : registry.getStatuses()) {
            int slot = status.getStatusSlot();
            if (!status.isShownInStatusMenu()
                    || !isSlotValid(slot)
                    || status.getId().equals(held.getStatusId())) {
                continue;
            }
            ItemBuilder.of(status.getIcon())
                    .name(ColorAPI.process(status.getStyledName()))
                    .lore(messages.getStringList("setup_status_layout_placed_lore", player))
                    .into(inventory, slot);
        }
    }

    private void renderControls(Player player) {
        Inventory playerInventory = player.getInventory();
        playerInventory.clear();

        ItemBuilder.of(XMaterial.BARRIER)
                .name(messages.getString("setup_back", player))
                .into(playerInventory, BACK_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                .name(messages.getString("setup_status_add", player))
                .lore(messages.getStringList("setup_status_layout_create_lore", player))
                .into(playerInventory, CREATE_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.RESET))
                .name(messages.getString("setup_status_layout_reset", player))
                .lore(messages.getStringList("setup_status_layout_reset_lore", player))
                .into(playerInventory, RESET_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.DELETE))
                .name(messages.getString("setup_status_layout_delete", player))
                .lore(messages.getStringList("setup_status_layout_delete_lore", player))
                .into(playerInventory, DELETE_SLOT);

        List<BuildWorldStatus> notAdded = notAddedStatuses();
        for (int i = 0; i < notAdded.size() && PALETTE_FIRST_SLOT + i <= PALETTE_LAST_SLOT; i++) {
            BuildWorldStatus status = notAdded.get(i);
            if (status.getId().equals(held.getStatusId())) {
                continue;
            }
            ItemBuilder.of(status.getIcon())
                    .name(ColorAPI.process(status.getStyledName()))
                    .lore(messages.getStringList("setup_status_layout_palette_lore", player))
                    .into(playerInventory, PALETTE_FIRST_SLOT + i);
        }
    }

    private List<BuildWorldStatus> notAddedStatuses() {
        return registry.getStatuses().stream()
                .filter(status -> !status.isShownInStatusMenu())
                .toList();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == getInventory()) {
            handlePreviewClick(player, event.getRawSlot(), event.isShiftClick());
        } else if (event.getClickedInventory() == player.getInventory()) {
            handlePlayerClick(player, event.getSlot(), event.isRightClick(), event.isShiftClick());
        } else {
            handleOutsideClickWhileHolding(player);
        }
    }

    private void handlePreviewClick(Player player, int slot, boolean shiftClick) {
        if (!isSlotValid(slot)) {
            return;
        }
        if (held.isHolding()) {
            placeHeld(player, slot);
            return;
        }

        WorldStatusImpl occupant = statusAtSlot(slot);
        if (occupant != null) {
            if (shiftClick) {
                new StatusEditorMenu(plugin, player, occupant).open(player);
            } else {
                pickUp(player, occupant.getId(), slot);
            }
        }
    }

    private void placeHeld(Player player, int slot) {
        WorldStatusImpl heldStatus = currentHeld();
        if (heldStatus == null) {
            clearHeld(player);
            return;
        }

        WorldStatusImpl occupant = statusAtSlot(slot);
        if (occupant != null && !occupant.equals(heldStatus)) {
            if (held.getFromSlot() >= 0) {
                occupant.setStatusSlot(held.getFromSlot());
            } else {
                occupant.setShownInStatusMenu(false);
            }
            registry.persist(occupant);
        }

        heldStatus.setStatusSlot(slot);
        heldStatus.setShownInStatusMenu(true);
        registry.persist(heldStatus);
        clearHeld(player);
        refresh(player);
    }

    private void handlePlayerClick(Player player, int slot, boolean rightClick, boolean shiftClick) {
        if (held.isHolding() && slot == DELETE_SLOT) {
            deleteHeld(player);
            return;
        }
        if (held.isHolding()) {
            handleOutsideClickWhileHolding(player);
            return;
        }

        if (slot == BACK_SLOT) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            player.closeInventory();
            new SetupMenu(plugin, player).open(player);
            return;
        }
        if (slot == CREATE_SLOT) {
            beginStatusCreation(player);
            return;
        }
        if (slot == RESET_SLOT) {
            // Left-click resets only the layout; right-click resets every status to the built-in defaults.
            promptReset(player, rightClick);
            return;
        }

        int paletteIndex = slot - PALETTE_FIRST_SLOT;
        List<BuildWorldStatus> notAdded = notAddedStatuses();
        if (paletteIndex < 0 || paletteIndex >= notAdded.size() || slot > PALETTE_LAST_SLOT) {
            return;
        }

        BuildWorldStatus clicked = notAdded.get(paletteIndex);
        if (shiftClick) {
            new StatusEditorMenu(plugin, player, clicked).open(player);
        } else {
            pickUp(player, clicked.getId(), -1);
        }
    }

    private void deleteHeld(Player player) {
        if (held.isHolding() && registry.deleteStatus(held.getStatusId())) {
            XSound.ENTITY_ITEM_BREAK.play(player);
        }
        clearHeld(player);
        refresh(player);
    }

    private void handleOutsideClickWhileHolding(Player player) {
        WorldStatusImpl heldStatus = currentHeld();
        if (heldStatus != null) {
            heldStatus.setShownInStatusMenu(false);
            registry.persist(heldStatus);
        }
        clearHeld(player);
        refresh(player);
    }

    private void promptReset(Player player, boolean resetEverything) {
        String confirmLoreKey = resetEverything
                ? "setup_status_layout_reset_all_confirm_lore"
                : "setup_status_layout_reset_confirm_lore";
        new DeletionConfirmMenu(
                        plugin,
                        player,
                        messages.getString("setup_status_layout_reset", player),
                        messages.getStringList(confirmLoreKey, player),
                        () -> {
                            if (resetEverything) {
                                registry.resetToDefaults();
                            } else {
                                registry.resetStatusLayout();
                            }
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            new StatusLayoutMenu(plugin, player).open(player);
                        },
                        () -> new StatusLayoutMenu(plugin, player).open(player))
                .open(player);
    }

    private void beginStatusCreation(Player player) {
        PlayerChatInput.requestSanitizedName(
                plugin,
                player,
                "setup_status_add_prompt",
                "setup_name_invalid_characters",
                "setup_name_empty",
                name -> {
                    WorldStatusImpl created = registry.createStatus(name);
                    new StatusEditorMenu(plugin, player, created).open(player);
                },
                () -> new StatusLayoutMenu(plugin, player).open(player));
    }

    private void pickUp(Player player, String statusId, int fromSlot) {
        held.track(statusId, fromSlot);
        BuildWorldStatus status = registry.getStatus(statusId).orElse(null);
        ItemStack cursor = status == null
                ? null
                : ItemBuilder.of(status.getIcon())
                        .name(ColorAPI.process(status.getStyledName()))
                        .build();

        setCursorNextTick(player, cursor);
        XSound.ITEM_ARMOR_EQUIP_LEATHER.play(player);
        refresh(player);
    }

    private void clearHeld(Player player) {
        held.reset();
        setCursorNextTick(player, null);
    }

    private void setCursorNextTick(Player player, @Nullable ItemStack cursor) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getNavigatorEditorService().hasSession(player)) {
                player.setItemOnCursor(cursor);
            }
        });
    }

    private void refresh(Player player) {
        populate(player);
        renderControls(player);
    }

    private @Nullable WorldStatusImpl statusAtSlot(int slot) {
        return registry.getStatuses().stream()
                .filter(status -> status.isShownInStatusMenu() && status.getStatusSlot() == slot)
                .map(status -> (WorldStatusImpl) status)
                .findFirst()
                .orElse(null);
    }

    private @Nullable WorldStatusImpl currentHeld() {
        return (WorldStatusImpl) registry.getStatus(held.getStatusId()).orElse(null);
    }

    private static boolean isSlotValid(int slot) {
        return slot >= 0 && slot < PREVIEW_SIZE;
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        plugin.getNavigatorEditorService().restore((Player) event.getPlayer());
    }

    /**
     * Pick-and-place tracking for the status currently held on the cursor.
     */
    private static final class Held {
        private @Nullable String statusId;
        private int fromSlot = -1;

        void track(String statusId, int fromSlot) {
            this.statusId = statusId;
            this.fromSlot = fromSlot;
        }

        void reset() {
            this.statusId = null;
            this.fromSlot = -1;
        }

        @Nullable String getStatusId() {
            return statusId;
        }

        boolean isHolding() {
            return statusId != null;
        }

        int getFromSlot() {
            return fromSlot;
        }
    }
}
