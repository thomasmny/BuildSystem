package de.eintosti.buildsystem.util.external.xseries;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wood;

/**
 * <b>XBlock</b> - MaterialData/BlockData Support<br>
 * BlockState (Old): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/BlockState.html
 * BlockData (New): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/data/BlockData.html
 * MaterialData (Old): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/material/MaterialData.html
 * <p>
 * All the parameters are non-null except the ones marked as nullable.
 * This class doesn't and shouldn't support materials that are {@link Material#isLegacy()}.
 *
 * @author Crypto Morin
 * @version 2.2.0
 * @see Block
 * @see BlockState
 * @see MaterialData
 * @see XMaterial
 */
@SuppressWarnings("deprecation")
public class XBlock {
    private static final boolean ISFLAT = XMaterial.isNewVersion();

    /**
     * Sets the type of any block that can be colored.
     *
     * @param block the block to color.
     * @param color the color to use.
     * @return true if the block can be colored, otherwise false.
     */
    public static boolean setColor(Block block, DyeColor color) {
        if (ISFLAT) {
            String type = block.getType().name();
            int index = type.indexOf('_');
            if (index == -1) return false;

            String realType = type.substring(index);
            Material material = Material.getMaterial(color.name() + '_' + realType);
            if (material == null) return false;
            block.setType(material);
            return true;
        }

        BlockState state = block.getState();
        state.setRawData(color.getWoolData());
        state.update(true);
        return false;
    }
}