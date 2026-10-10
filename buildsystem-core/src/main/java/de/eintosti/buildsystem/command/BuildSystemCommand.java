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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.command.HelpPages.Entry;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.StorageMigrator;
import de.eintosti.buildsystem.storage.StorageType;
import de.eintosti.buildsystem.util.Permissions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildSystemCommand extends CommandBase {

    static final List<Entry> COMMANDS = List.of(
            new Entry("/back", "buildsystem_back", "/back", Permissions.BACK),
            new Entry("/blocks", "buildsystem_blocks", "/blocks", Permissions.BLOCKS),
            new Entry("/build [player]", "buildsystem_build", "/build", Permissions.BUILD),
            new Entry("/config reload", "buildsystem_config", "/config reload", Permissions.CONFIG),
            new Entry("/day [world]", "buildsystem_day", "/day", Permissions.DAY),
            new Entry("/explosions [world]", "buildsystem_explosions", "/explosions", Permissions.EXPLOSIONS),
            new Entry("/gm <gamemode> [player]", "buildsystem_gamemode", "/gm ", Permissions.GAMEMODE),
            new Entry("/night [world]", "buildsystem_night", "/night", Permissions.NIGHT),
            new Entry("/noai [world]", "buildsystem_noai", "/noai", Permissions.NOAI),
            new Entry("/physics [world]", "buildsystem_physics", "/physics", Permissions.PHYSICS),
            new Entry("/settings", "buildsystem_settings", "/settings", Permissions.SETTINGS),
            new Entry("/setup", "buildsystem_setup", "/setup", Permissions.SETUP),
            new Entry("/skull [player/id]", "buildsystem_skull", "/skull", Permissions.SKULL),
            new Entry("/spawn", "buildsystem_spawn", "/spawn", "-"),
            new Entry("/speed [1-5]", "buildsystem_speed", "/speed ", Permissions.SPEED),
            new Entry(
                    "/buildsystem storage migrate <type>",
                    "buildsystem_storage",
                    "/buildsystem storage migrate ",
                    Permissions.STORAGE_MIGRATE),
            new Entry("/top", "buildsystem_top", "/top", Permissions.TOP),
            new Entry("/worlds help", "buildsystem_worlds", "/worlds help", Permissions.HELP_WORLDS));

    private final HelpPages pages;
    private final Services services;

    public BuildSystemCommand(Messages messages, Logger logger, Services services) {
        super(messages, logger, false);
        this.pages = new HelpPages(messages, "buildsystem", COMMANDS);
        this.services = services;
    }

    @Override
    protected void run(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("storage")) {
            storage(sender, args);
            return;
        }
        if (!(sender instanceof Player player)) {
            messages.sendMessage(sender, "sender_not_player");
            return;
        }
        if (!requirePermission(player, Permissions.HELP_BUILDSYSTEM)) {
            return;
        }

        switch (args.length) {
            case 0 -> pages.send(player, 1);
            case 1 -> pages.send(player, args[0]);
            default -> messages.sendMessage(player, "buildsystem_usage");
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> result = new ArrayList<>();
        if (!player.hasPermission(Permissions.STORAGE_MIGRATE)) {
            return result;
        }
        String input = args[args.length - 1];
        switch (args.length) {
            case 1 -> Completions.addMatching(input, "storage", result);
            case 2 -> {
                if (args[0].equalsIgnoreCase("storage")) {
                    Completions.addMatching(input, "migrate", result);
                }
            }
            case 3 -> {
                if (args[0].equalsIgnoreCase("storage") && args[1].equalsIgnoreCase("migrate")) {
                    for (StorageType type : StorageType.values()) {
                        if (type != services.storage().type()) {
                            Completions.addMatching(input, type.key(), result);
                        }
                    }
                }
            }
            default -> {}
        }
        return result;
    }

    /**
     * {@code /buildsystem storage migrate <type> [confirm]}: without {@code confirm}, only says what would be copied.
     */
    private void storage(CommandSender sender, String[] args) {
        if (!requirePermission(sender, Permissions.STORAGE_MIGRATE)) {
            return;
        }
        if (args.length < 3
                || args.length > 4
                || !args[1].equalsIgnoreCase("migrate")
                || (args.length == 4 && !args[3].equalsIgnoreCase("confirm"))) {
            messages.sendMessage(sender, "storage_usage");
            return;
        }
        StorageType type;
        try {
            type = StorageType.parse(args[2]);
        } catch (IllegalArgumentException e) {
            String types =
                    Arrays.stream(StorageType.values()).map(StorageType::key).collect(Collectors.joining(", "));
            messages.sendMessage(sender, "storage_unknown_type", Placeholders.of("%types%", types));
            return;
        }
        if (type == services.storage().type()) {
            messages.sendMessage(sender, "storage_already_in_use", Placeholders.of("%type%", type.displayName()));
            return;
        }
        StorageMigrator migrator = services.storageMigrator();
        if (migrator.running()) {
            messages.sendMessage(sender, "storage_migrate_running");
            return;
        }
        PluginConfig.Storage.Database settings;
        try {
            settings = services.config().readStorageSettings();
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            failed(sender, type, e);
            return;
        }
        if (args.length == 3) {
            confirm(sender, type, settings);
            return;
        }

        messages.sendMessage(sender, "storage_migrate_started", Placeholders.of("%type%", type.displayName()));
        migrator.migrate(type, settings)
                .whenCompleteAsync(
                        (copied, failure) -> {
                            if (failure != null) {
                                failed(
                                        sender,
                                        type,
                                        failure instanceof CompletionException ? failure.getCause() : failure);
                                return;
                            }
                            messages.sendMessage(
                                    sender,
                                    "storage_migrate_done",
                                    counts(copied)
                                            .add("%type%", type.displayName())
                                            .build());
                        },
                        services.scheduler().mainThread());
    }

    /**
     * Says what a migration to {@code type} would copy, and whether it replaces what {@code type} holds already.
     */
    private void confirm(CommandSender sender, StorageType type, PluginConfig.Storage.Database settings) {
        StorageMigrator migrator = services.storageMigrator();
        migrator.holdsData(type, settings)
                .whenCompleteAsync(
                        (holdsData, failure) -> {
                            if (failure != null) {
                                failed(
                                        sender,
                                        type,
                                        failure instanceof CompletionException ? failure.getCause() : failure);
                                return;
                            }
                            if (holdsData) {
                                messages.sendMessage(
                                        sender,
                                        "storage_migrate_target_has_data",
                                        Placeholders.of("%type%", type.displayName()));
                            }
                            if (migrator.skipped() > 0) {
                                messages.sendMessage(
                                        sender,
                                        "storage_migrate_skipped",
                                        Placeholders.of()
                                                .add("%skipped%", String.valueOf(migrator.skipped()))
                                                .add(
                                                        "%from%",
                                                        services.storage()
                                                                .type()
                                                                .displayName())
                                                .build());
                            }
                            messages.sendMessage(
                                    sender,
                                    "storage_migrate_confirm",
                                    counts(migrator.counts())
                                            .add(
                                                    "%from%",
                                                    services.storage().type().displayName())
                                            .add("%type%", type.displayName())
                                            .add("%key%", type.key())
                                            .build());
                        },
                        services.scheduler().mainThread());
    }

    private void failed(CommandSender sender, StorageType type, Throwable failure) {
        logger.log(
                Level.SEVERE,
                "Could not migrate storage to " + type.displayName() + ", still using "
                        + services.storage().type().displayName(),
                failure);
        messages.sendMessage(
                sender,
                "storage_migrate_failed",
                Placeholders.of()
                        .add("%type%", type.displayName())
                        .add("%reason%", String.valueOf(failure.getMessage()))
                        .build());
    }

    private static Placeholders.Builder counts(Map<EntityKind, Integer> counts) {
        return Placeholders.of()
                .add("%worlds%", counts.get(EntityKind.WORLDS))
                .add("%folders%", counts.get(EntityKind.FOLDERS))
                .add("%players%", counts.get(EntityKind.PLAYERS));
    }
}
