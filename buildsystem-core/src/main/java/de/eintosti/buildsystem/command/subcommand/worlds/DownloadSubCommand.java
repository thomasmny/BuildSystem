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
import de.eintosti.buildsystem.util.WorldFlush;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.download.DownloadProgress;
import de.eintosti.buildsystem.world.download.ExportProgressBar;
import de.eintosti.buildsystem.world.download.WorldDownloadService;
import de.eintosti.buildsystem.world.download.WorldExporter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DownloadSubCommand extends AbstractSubCommand {

    /**
     * How often the action bar is redrawn. Four frames a second reads as motion without spamming packets, and the
     * action bar itself fades after about three seconds, so it must be refreshed well inside that.
     */
    private static final long ANIMATION_PERIOD_TICKS = 5L;

    /**
     * Where the clickable button goes in the finished message, quoted because {@link String#split} takes a regex.
     */
    private static final String BUTTON_PLACEHOLDER = Pattern.quote("%button%");

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
        buildWorld.getWorld().ifPresent(WorldFlush::saveAndFlush);

        AtomicLong doneBytes = new AtomicLong();
        AtomicLong totalBytes = new AtomicLong();
        AtomicReference<DownloadProgress.Phase> phase = new AtomicReference<>(DownloadProgress.Phase.PACKING);
        BukkitTask animation = startProgressAnimation(player, buildWorld, phase, doneBytes, totalBytes);

        downloadService
                .prepare(buildWorld, (currentPhase, done, total) -> {
                    // Set the phase last: it is what the animation reads to pick its message, so flipping it before
                    // the counters would draw the new phase with the old one's numbers.
                    doneBytes.set(done);
                    totalBytes.set(total);
                    phase.set(currentPhase);
                })
                .whenCompleteAsync(
                        (url, throwable) -> {
                            preparing.remove(playerId);
                            animation.cancel();
                            clearActionBar(player);
                            if (throwable != null) {
                                sendFailure(player, buildWorld, worldPlaceholder, throwable);
                                return;
                            }
                            if (player.isOnline()) {
                                sendLink(player, buildWorld, url);
                            }
                        },
                        scheduler.mainThread());
    }

    /**
     * Drives the action bar while the download is prepared. The frame counter advances every tick of this task, so the
     * bar keeps moving even while a single large region file is being packed, and the message follows the phase so a
     * full bar is never left standing over work that is still running.
     */
    private BukkitTask startProgressAnimation(
            Player player,
            BuildWorld buildWorld,
            AtomicReference<DownloadProgress.Phase> phase,
            AtomicLong doneBytes,
            AtomicLong totalBytes) {
        AtomicInteger frame = new AtomicInteger();
        return scheduler.runTimer(
                () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    long total = totalBytes.get();
                    double fraction = total <= 0 ? 0 : (double) doneBytes.get() / total;
                    int currentFrame = frame.getAndIncrement();

                    sendActionBar(
                            player,
                            messages.getString(
                                    messageKey(phase.get()),
                                    player,
                                    Placeholders.of()
                                            .add("%world%", buildWorld.getName())
                                            .add("%bar%", ExportProgressBar.bar(fraction, currentFrame))
                                            .add("%percent%", ExportProgressBar.percentText(fraction))
                                            .add("%spinner%", ExportProgressBar.spinner(currentFrame))
                                            .build()));
                },
                0L,
                ANIMATION_PERIOD_TICKS);
    }

    private static String messageKey(DownloadProgress.Phase phase) {
        return switch (phase) {
            case PACKING -> "worlds_download_progress";
            case PUBLISHING -> "worlds_download_uploading";
        };
    }

    private static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private static void clearActionBar(Player player) {
        if (player.isOnline()) {
            sendActionBar(player, "");
        }
    }

    /**
     * Reports the failure in the player's terms. A world that outgrows its limit or a full storage budget is an
     * operator-tunable condition rather than a bug, so neither is logged as one.
     */
    private void sendFailure(Player player, BuildWorld buildWorld, Placeholders worldPlaceholder, Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        if (cause instanceof UncheckedIOException uncheckedIoException) {
            cause = uncheckedIoException.getCause();
        }

        switch (cause) {
            case WorldExporter.WorldTooLargeException ignored ->
                messages.sendMessage(player, "worlds_download_too_large", worldPlaceholder);
            case WorldDownloadService.StorageFullException ignored ->
                messages.sendMessage(player, "worlds_download_storage_full", worldPlaceholder);
            default -> {
                logger.log(Level.SEVERE, "Failed to export world " + buildWorld.getName(), throwable);
                messages.sendMessage(player, "worlds_download_failed", worldPlaceholder);
            }
        }
    }

    /**
     * Sends the finished message with only its {@code %button%} segment carrying the link, so the rest of the line
     * cannot be clicked by accident.
     *
     * <p>A messages.yml written before the button existed has no {@code %button%} in it — an upgrade keeps the old
     * line, since only missing keys are filled in. Such a message becomes clickable as a whole, the way it used to be:
     * appending a button instead would print the words twice.
     */
    private void sendLink(Player player, BuildWorld buildWorld, String url) {
        Placeholders placeholders = Placeholders.of()
                .add("%world%", buildWorld.getName())
                .add("%minutes%", downloadService.getExpirationMinutes())
                .build();
        String text = messages.getString("worlds_download_finished", player, placeholders);

        String[] parts = text.split(BUTTON_PLACEHOLDER, 2);
        if (parts.length == 1) {
            player.spigot().sendMessage(linked(TextComponent.fromLegacyText(text), url));
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            return;
        }

        TextComponent message = new TextComponent();
        message.addExtra(new TextComponent(TextComponent.fromLegacyText(parts[0])));
        message.addExtra(linked(
                TextComponent.fromLegacyText(messages.getString("worlds_download_button", player, placeholders)), url));
        message.addExtra(new TextComponent(TextComponent.fromLegacyText(parts[1])));

        player.spigot().sendMessage(message);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
    }

    private static TextComponent linked(BaseComponent[] text, String url) {
        TextComponent component = new TextComponent(text);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(url)));
        return component;
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
