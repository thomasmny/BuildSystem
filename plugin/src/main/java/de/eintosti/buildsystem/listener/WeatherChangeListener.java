package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * @author einTosti
 */
public class WeatherChangeListener implements Listener {
    public WeatherChangeListener(BuildSystem plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeatherChange(WeatherChangeEvent event) {
        boolean rain = event.toWeatherState();
        if (rain) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onThunderChange(ThunderChangeEvent event) {
        boolean storm = event.toThunderState();
        if (storm) event.setCancelled(true);
    }
}
