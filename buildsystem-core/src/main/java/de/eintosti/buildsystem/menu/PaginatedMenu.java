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

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.i18n.Messages;
import java.util.List;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * A {@link Menu} that shows a variable-length collection across multiple pages.
 *
 * <p>This base owns only the <strong>paging cursor</strong> and the arithmetic/sounds around moving it; it does not
 * render anything itself. The current page is held in {@link #page}, and subclasses report the collection size via
 * {@link #totalItems()}. On a page-navigation click a subclass calls {@link #nextPage}/{@link #previousPage} (which
 * advance the cursor and play feedback) and then re-{@link #open(Player) opens}/re-{@code populate}s so its
 * {@code populate} can render the slice for the new page.
 *
 * <p><strong>Items per page</strong> is passed in per call rather than stored, because different paginated menus lay
 * their content out across different numbers of slots. {@link #totalPages(int)} always reports at least one page, so an
 * empty collection still renders a valid (blank) first page.
 *
 * <p>The page-change and refusal sounds are overridable ({@link #playPageSound}/{@link #playRefuseSound}) for menus
 * that want different audio feedback.
 *
 * <p>Being a {@link ButtonMenu}, a paginated menu may also {@link #register(int, MenuButton) register} fixed control
 * buttons (e.g. the page arrows and a close button) whose clicks are routed by the base {@code handleClick}, while the
 * variable page content is rendered separately by its {@code populate}.
 */
@NullMarked
public abstract class PaginatedMenu extends ButtonMenu<MenuButton> {

    private int page = 0;

    /**
     * @param messages The message provider (see {@link Menu})
     * @param size The inventory size in slots
     * @param title The inventory title
     */
    protected PaginatedMenu(Messages messages, int size, String title) {
        super(messages, size, title);
    }

    /**
     * {@return the total number of items across all pages} Drives {@link #totalPages(int)} and the page bounds. Read
     * fresh each call, so a menu backed by changing data paginates correctly without resetting the cursor.
     */
    protected abstract int totalItems();

    /**
     * {@return the current zero-based page index}
     */
    protected final int page() {
        return page;
    }

    /**
     * Resets the cursor back to the first page. Call when the underlying collection changes enough that the current
     * page may no longer be valid (e.g. after a filter or a deletion).
     */
    protected final void resetPage() {
        page = 0;
    }

    /**
     * {@return the number of pages needed to show {@link #totalItems()} at the given page size} Never less than one: an
     * empty collection still occupies a single (blank) page.
     *
     * @param itemsPerPage The number of items shown per page
     */
    protected final int totalPages(int itemsPerPage) {
        int total = totalItems();
        return total == 0 ? 1 : (int) Math.ceil((double) total / itemsPerPage);
    }

    /**
     * Moves to the previous page if one exists, playing the page sound; otherwise plays the refusal sound and leaves the
     * cursor unchanged. The caller is responsible for re-rendering after a successful move.
     *
     * @param player The player navigating (for sound feedback)
     * @param itemsPerPage The number of items shown per page
     * @return {@code true} if the page changed, {@code false} if already on the first page
     */
    protected boolean previousPage(Player player, int itemsPerPage) {
        if (totalPages(itemsPerPage) > 1 && page > 0) {
            page--;
            playPageSound(player);
            return true;
        }
        playRefuseSound(player);
        return false;
    }

    /**
     * Moves to the next page if one exists, playing the page sound; otherwise plays the refusal sound and leaves the
     * cursor unchanged. The caller is responsible for re-rendering after a successful move.
     *
     * @param player The player navigating (for sound feedback)
     * @param itemsPerPage The number of items shown per page
     * @return {@code true} if the page changed, {@code false} if already on the last page
     */
    protected boolean nextPage(Player player, int itemsPerPage) {
        if (totalPages(itemsPerPage) > 1 && page < totalPages(itemsPerPage) - 1) {
            page++;
            playPageSound(player);
            return true;
        }
        playRefuseSound(player);
        return false;
    }

    /**
     * {@return a button that flips to the previous page and re-{@link #populate populates}} If already on the first page
     * it does nothing beyond the refusal feedback from {@link #previousPage}. Register it at the slot the menu uses for
     * its "previous page" control.
     *
     * @param skullTexture The skull texture for the arrow icon
     * @param itemsPerPage The number of items shown per page
     */
    protected final MenuButton previousPageButton(String skullTexture, int itemsPerPage) {
        return pageButton(skullTexture, "gui_previous_page", true, itemsPerPage);
    }

    /**
     * {@return a button that flips to the next page and re-{@link #populate populates}} If already on the last page it
     * does nothing beyond the refusal feedback from {@link #nextPage}. Register it at the slot the menu uses for its
     * "next page" control.
     *
     * @param skullTexture The skull texture for the arrow icon
     * @param itemsPerPage The number of items shown per page
     */
    protected final MenuButton nextPageButton(String skullTexture, int itemsPerPage) {
        return pageButton(skullTexture, "gui_next_page", false, itemsPerPage);
    }

    /**
     * Registers a button for each item visible on the current page, placing the i-th visible item at
     * {@code contentSlots[i]}. The page window is derived from {@link #page()} and {@code contentSlots.length}; the
     * {@code itemsPerPage} the page arrows use should equal that length.
     *
     * @param contentSlots The slots that hold page content, in order
     * @param items The full, unpaged item list
     * @param buttonFactory Builds the button for a given item
     * @param <T> The item type
     */
    protected final <T> void registerPageItems(
            int[] contentSlots, List<T> items, Function<T, MenuButton> buttonFactory) {
        int startIndex = page() * contentSlots.length;
        for (int i = 0; i < contentSlots.length && startIndex + i < items.size(); i++) {
            register(contentSlots[i], buttonFactory.apply(items.get(startIndex + i)));
        }
    }

    /**
     * Registers a button for each item visible on the current page across {@code count} contiguous slots starting at
     * {@code firstSlot}. Convenience for the common case where page content occupies a contiguous slot range.
     *
     * @param firstSlot The first content slot
     * @param count The number of content slots (also the items-per-page)
     * @param items The full, unpaged item list
     * @param buttonFactory Builds the button for a given item
     * @param <T> The item type
     */
    protected final <T> void registerPageItems(
            int firstSlot, int count, List<T> items, Function<T, MenuButton> buttonFactory) {
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = firstSlot + i;
        }
        registerPageItems(slots, items, buttonFactory);
    }

    private MenuButton pageButton(String skullTexture, String nameKey, boolean previous, int itemsPerPage) {
        return MenuButton.builder()
                .render((player, inventory, slot) -> ItemBuilder.skull(Profileable.detect(skullTexture))
                        .name(messages.getString(nameKey, player))
                        .into(inventory, slot))
                .onClick((player, event) -> {
                    boolean moved = previous ? previousPage(player, itemsPerPage) : nextPage(player, itemsPerPage);
                    if (moved) {
                        populate(player);
                    }
                })
                .build();
    }

    /**
     * Plays the sound for a successful page change. Override to customise.
     *
     * @param player The player to play the sound to
     */
    protected void playPageSound(Player player) {
        XSound.ENTITY_CHICKEN_EGG.play(player);
    }

    /**
     * Plays the sound for a refused page change (already at the first/last page). Override to customise.
     *
     * @param player The player to play the sound to
     */
    protected void playRefuseSound(Player player) {
        XSound.ENTITY_ITEM_BREAK.play(player);
    }
}
