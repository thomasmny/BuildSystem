package de.eintosti.buildsystem.object.world;

/**
 * @author einTosti
 */
public enum Generator {
    NORMAL(WorldType.NORMAL),
    FLAT(WorldType.FLAT),
    VOID(WorldType.VOID),
    CUSTOM(WorldType.IMPORTED);

    private final WorldType worldType;

    Generator(WorldType worldType) {
        this.worldType = worldType;
    }

    public WorldType getWorldType() {
        return worldType;
    }
}
