package de.eintosti.buildsystem.util.external;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.util.external.xseries.Titles;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerChatInput implements Listener {
    private final BuildSystem plugin;

    private final BukkitTask taskId;
    private final InputRunnable runWhenComplete;
    private final UUID playerUuid;
    private final boolean inputMode;

    private static final Map<UUID, PlayerChatInput> inputs = new HashMap<>();

    public PlayerChatInput(BuildSystem plugin, Player player, String titleKey, InputRunnable runWhenComplete) {
        this.plugin = plugin;

        String title = plugin.getString(titleKey);
        String subtitle = plugin.getString("cancel_subtitle");

        this.taskId = new BukkitRunnable() {
            public void run() {
                Titles.sendTitle(player, 0, 30, 0, title, subtitle);
            }
        }.runTaskTimer(plugin, 0L, 20L);

        this.playerUuid = player.getUniqueId();
        this.runWhenComplete = runWhenComplete;
        this.inputMode = true;

        player.closeInventory();
        XSound.ENTITY_PLAYER_LEVELUP.play(player);

        this.register();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String input = event.getMessage();
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (!inputs.containsKey(playerUuid)) {
            return;
        }

        PlayerChatInput current = inputs.get(playerUuid);
        if (!current.inputMode) {
            return;
        }

        event.setCancelled(true);

        if (input.equalsIgnoreCase("cancel")) {
            current.taskId.cancel();
            current.unregister();

            XSound.ENTITY_ITEM_BREAK.play(player);
            Titles.clearTitle(player);
            player.sendMessage(plugin.getString("input_cancelled"));
            return;
        }

        current.taskId.cancel();
        Bukkit.getScheduler().scheduleSyncDelayedTask(current.plugin, () -> current.runWhenComplete.run(input), 3);
        Titles.clearTitle(player);
        current.unregister();
    }

    private void register() {
        inputs.put(this.playerUuid, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void unregister() {
        inputs.remove(this.playerUuid);
        HandlerList.unregisterAll(this);
    }

    @FunctionalInterface
    public interface InputRunnable {
        void run(String input);
    }
}