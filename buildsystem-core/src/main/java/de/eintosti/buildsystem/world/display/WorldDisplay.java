package de.eintosti.buildsystem.world.display;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.world.BuildWorld;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the display-related functionality for a BuildWorld. This class is responsible for converting a BuildWorld into displayable items and managing how it appears in
 * inventories.
 */
public class WorldDisplay implements Displayable {

    private final BuildWorld buildWorld;

    public WorldDisplay(BuildWorld buildWorld) {
        this.buildWorld = buildWorld;
    }

    @Override
    public String getName() {
        return buildWorld.getName();
    }

    @Override
    public XMaterial getMaterial() {
        return buildWorld.getData().getMaterial();
    }

    @Override
    public String getDisplayName(Player player) {
        return Messages.getString("world_item_title", player,
                new AbstractMap.SimpleEntry<>("%world%", getName())
        );
    }

    @Override
    public List<String> getLore(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(Messages.getString("world_item_creator", player,
                new AbstractMap.SimpleEntry<>("%creator%", buildWorld.hasCreator() ? buildWorld.getCreator().getName() : "N/A"))
        );
        lore.add(Messages.getString("world_item_type", player,
                new AbstractMap.SimpleEntry<>("%type%", buildWorld.getType().getName(player)))
        );
        return lore;
    }

    @Override
    public ItemStack asItemStack(Player player) {
        return getMaterial().parseItem();
    }
} 