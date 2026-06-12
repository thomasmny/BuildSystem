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

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.api.player.settings.Settings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ItemBuilder {

    private static final Logger LOGGER = Logger.getLogger(ItemBuilder.class.getName());

    private final ItemStack itemStack;
    private @Nullable ItemMeta itemMeta;

    private ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }

    public static ItemBuilder of(XMaterial material) {
        ItemStack base = material.parseItem();
        if (base == null) {
            base = XMaterial.BEDROCK.parseItem();
            LOGGER.warning("Unknown material (" + material + "). Defaulting to BEDROCK.");
        }
        return new ItemBuilder(base);
    }

    public static ItemBuilder skull(Profileable profileable) {
        ItemStack skull = XSkull.createItem().profile(profileable).lenient().apply();
        return new ItemBuilder(skull);
    }

    public static ItemBuilder glassPane(Player player, SettingsService settingsService) {
        Settings settings = settingsService.getSettings(player);
        DesignColor color = settings.getDesignColor();
        XMaterial material = XMaterial.matchXMaterial(color.name() + "_STAINED_GLASS_PANE")
                .orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
        return of(material).name(" ");
    }

    public ItemBuilder name(String displayName) {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.setLore(new ArrayList<>(lines));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder hideAttributes() {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemBuilder glow(boolean enabled) {
        if (!enabled) {
            return this;
        }
        ensureMeta();
        if (itemMeta != null) {
            var enchantment = XEnchantment.UNBREAKING.get();
            if (enchantment != null) {
                itemStack.addUnsafeEnchantment(enchantment, 1);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }
        return this;
    }

    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        ensureMeta();
        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(key, type, value);
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private void ensureMeta() {
        if (itemMeta == null) {
            itemMeta = itemStack.getItemMeta();
        }
    }
}
