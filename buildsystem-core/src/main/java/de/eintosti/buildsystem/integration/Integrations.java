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
package de.eintosti.buildsystem.integration;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.integration.luckperms.LuckPermsExpansion;
import de.eintosti.buildsystem.integration.placeholderapi.PapiTextResolver;
import de.eintosti.buildsystem.integration.placeholderapi.PlaceholderApiExpansion;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Owns lifecycle (activate/deactivate) for all optional-plugin integrations. New integrations: add a new subpackage
 * under {@code integration/}, expose {@code register}/{@code unregister}, and wire it in here.
 */
@NullMarked
public final class Integrations {

    private final BuildSystemPlugin plugin;

    private @Nullable PlaceholderApiExpansion placeholderApi;
    private @Nullable LuckPermsExpansion luckPerms;

    public Integrations(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void activate() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        if (pluginManager.getPlugin("PlaceholderAPI") != null) {
            this.placeholderApi = new PlaceholderApiExpansion(plugin);
            this.placeholderApi.register();
            plugin.getMessages().setPlaceholderResolver(new PapiTextResolver());
        }

        if (pluginManager.getPlugin("LuckPerms") != null) {
            this.luckPerms = new LuckPermsExpansion(plugin);
            this.luckPerms.registerAll();
        }
    }

    public void deactivate() {
        if (this.placeholderApi != null) {
            this.placeholderApi.unregister();
            plugin.getMessages().setPlaceholderResolver(null);
        }

        if (this.luckPerms != null) {
            this.luckPerms.unregisterAll();
        }
    }
}
