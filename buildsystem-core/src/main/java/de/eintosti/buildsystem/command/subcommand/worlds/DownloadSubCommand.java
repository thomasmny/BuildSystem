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
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.download.WorldDownloadService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DownloadSubCommand extends AbstractSubCommand {

    private final WorldDownloadService downloadService;
    private final TaskScheduler scheduler;
    private final Logger logger;

    /**
     * Players with an export in flight. Zipping a world is expensive, so a player cannot stack up exports by spamming
     * the command.
     */
    private final Set<UUID> preparing = ConcurrentHashMap.newKeySet();

    public DownloadSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            WorldDownloadService downloadService,
            TaskScheduler scheduler,
            Logger logger) {
        super(messages, worldService);
        this.downloadService = downloadService;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = requireWorld(player, worldName, args, 2, "worlds_download");
        if (buildWorld == null) {
            return;
        }

        if (!downloadService.isEnabled()) {
            messages.sendMessage(player, "worlds_download_disabled");
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!preparing.add(playerId)) {
            messages.sendMessage(player, "worlds_download_in_progress");
            return;
        }

        Placeholders worldPlaceholder = Placeholders.of("%world%", buildWorld.getName());
        messages.sendMessage(player, "worlds_download_preparing", worldPlaceholder);
        buildWorld.getWorld().ifPresent(World::save);

        downloadService
                .prepare(buildWorld)
                .whenCompleteAsync(
                        (url, throwable) -> {
                            preparing.remove(playerId);
                            if (throwable != null) {
                                logger.log(Level.SEVERE, "Failed to export world " + buildWorld.getName(), throwable);
                                messages.sendMessage(player, "worlds_download_failed", worldPlaceholder);
                                return;
                            }
                            if (player.isOnline()) {
                                sendLink(player, buildWorld, url);
                            }
                        },
                        scheduler.mainThread());
    }

    private void sendLink(Player player, BuildWorld buildWorld, String url) {
        String message = messages.getString(
                "worlds_download_finished",
                player,
                Placeholders.of()
                        .add("%world%", buildWorld.getName())
                        .add("%minutes%", downloadService.getExpirationMinutes())
                        .build());

        TextComponent component = new TextComponent(TextComponent.fromLegacyText(message));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(url)));
        player.spigot().sendMessage(component);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }

        WorldStorage worldStorage = worldService.getWorldStorage();
        return WorldsCompletions.permittedWorldNames(
                player, worldStorage, getArgument().getPermission(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.DOWNLOAD;
    }
}
