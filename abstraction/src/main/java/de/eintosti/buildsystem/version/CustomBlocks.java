package de.eintosti.buildsystem.version;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public interface CustomBlocks {
    void setBlock(BlockPlaceEvent event, String... blockName);

    void setPlant(PlayerInteractEvent event);

    void modifySlab(PlayerInteractEvent event);

    void toggleIronTrapdoor(PlayerInteractEvent event);

    void toggleIronDoor(PlayerInteractEvent event);

    void rotate(Block block, Player player, BlockFace blockFace);
}
