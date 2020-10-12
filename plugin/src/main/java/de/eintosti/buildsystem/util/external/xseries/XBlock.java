package de.eintosti.buildsystem.util.external.xseries;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Lightable;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wood;

/**
 * <b>XBlock</b> - MaterialData/BlockData Support<br>
 * BlockState (Old): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/BlockState.html
 * BlockData (New): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/data/BlockData.html
 * MaterialData (Old): https://hub.spigotmc.org/javadocs/spigot/org/bukkit/material/MaterialData.html
 * All the parameters are non-null except the ones marked as nullable.
 *
 * @author Crypto Morin
 * @version 1.1.3
 * @see Block
 * @see BlockState
 * @see MaterialData
 * @see XMaterial
 */
@SuppressWarnings("deprecation")
public class XBlock {
    private static final boolean ISFLAT = XMaterial.isNewVersion();

    /**
     * Can be furnaces or redstone lamps.
     */
    public static void setLit(Block block, boolean lit) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Lightable)) return;
            Lightable lightable = (Lightable) block.getBlockData();
            lightable.setLit(lit);
            return;
        }

        String name = block.getType().name();
        if (name.endsWith("FURNACE")) block.setType(Material.getMaterial("BURNING_FURNACE"));
        else if (name.startsWith("REDSTONE_LAMP")) block.setType(Material.getMaterial("REDSTONE_LAMP_ON"));
        else block.setType(Material.getMaterial("REDSTONE_TORCH_ON"));
    }

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

    public static XMaterial getType(Block block) {
        if (ISFLAT) return XMaterial.matchXMaterial(block.getType());
        String type = block.getType().name();
        BlockState state = block.getState();
        MaterialData data = state.getData();

        if (data instanceof Wood) {
            TreeSpecies species = ((Wood) data).getSpecies();
            return XMaterial.matchXMaterial(species.name() + block.getType().name())
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported material from tree species " + species.name() + ": " + block.getType().name()));
        }
        if (data instanceof Colorable) {
            Colorable color = (Colorable) data;
            return XMaterial.matchXMaterial(color.getColor().name() + '_' + type)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported colored material"));
        }
        return XMaterial.matchXMaterial(block.getType());
    }

    private static boolean isMaterial(Block block, String... materials) {
        String type = block.getType().name();
        for (String material : materials)
            if (type.equals(material)) return true;
        return false;
    }
}