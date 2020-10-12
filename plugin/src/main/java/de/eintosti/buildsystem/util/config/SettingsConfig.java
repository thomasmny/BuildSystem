package de.eintosti.buildsystem.util.config;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.object.settings.Settings;

import java.util.UUID;

/**
 * @author einTosti
 */
public class SettingsConfig extends ConfigurationFile {

    public SettingsConfig(BuildSystem plugin) {
        super(plugin, "settings.yml");
    }

    public void saveSettings(UUID uuid, Settings settings) {
        getFile().set("settings." + uuid.toString(), settings.serialize());
        saveFile();
    }
}
