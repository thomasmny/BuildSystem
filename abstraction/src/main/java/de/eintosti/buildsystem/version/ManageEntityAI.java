package de.eintosti.buildsystem.version;

import org.bukkit.entity.LivingEntity;

/**
 * @author einTosti
 */
public interface ManageEntityAI {
    void setAI(LivingEntity entity, boolean hasAI);
}
