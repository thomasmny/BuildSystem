package de.eintosti.buildsystem.version;

import net.minecraft.server.v1_8_R1.EntityLiving;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * @author einTosti
 */
public class ManageEntityAI_1_8_R1 implements ManageEntityAI {

    public void setAI(LivingEntity entity, boolean hasAi) {
        EntityType entityType = entity.getType();
        if (entityType == EntityType.ARMOR_STAND || entityType == EntityType.ITEM_FRAME) return;

        try {
            EntityLiving handle = ((CraftLivingEntity) entity).getHandle();
            handle.getDataWatcher().watch(15, (byte) (hasAi ? 0 : 1));
        } catch (NullPointerException ignored) {
        }
    }
}
