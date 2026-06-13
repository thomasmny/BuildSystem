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
import de.eintosti.buildsystem.i18n.Messages;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class PaginatedMenu extends Menu {

    private int page = 0;

    protected PaginatedMenu(Messages messages, int size, String title) {
        super(messages, size, title);
    }

    // package-private: only for unit tests that cannot run a Bukkit server
    PaginatedMenu(Messages messages, org.bukkit.inventory.Inventory inventory) {
        super(messages, inventory);
    }

    protected abstract int totalItems();

    protected final int page() {
        return page;
    }

    protected final void resetPage() {
        page = 0;
    }

    protected final int totalPages(int itemsPerPage) {
        int total = totalItems();
        return total == 0 ? 1 : (int) Math.ceil((double) total / itemsPerPage);
    }

    protected boolean previousPage(Player player, int itemsPerPage) {
        if (totalPages(itemsPerPage) > 1 && page > 0) {
            page--;
            playPageSound(player);
            return true;
        }
        playRefuseSound(player);
        return false;
    }

    protected boolean nextPage(Player player, int itemsPerPage) {
        if (totalPages(itemsPerPage) > 1 && page < totalPages(itemsPerPage) - 1) {
            page++;
            playPageSound(player);
            return true;
        }
        playRefuseSound(player);
        return false;
    }

    protected void playPageSound(Player player) {
        XSound.ENTITY_CHICKEN_EGG.play(player);
    }

    protected void playRefuseSound(Player player) {
        XSound.ENTITY_ITEM_BREAK.play(player);
    }
}
