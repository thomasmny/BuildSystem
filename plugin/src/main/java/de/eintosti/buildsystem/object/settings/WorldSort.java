package de.eintosti.buildsystem.object.settings;

/**
 * @author einTosti
 */
public enum WorldSort {
    NAME_A_TO_Z,
    NAME_Z_TO_A,
    PROJECT_A_TO_Z,
    PROJECT_Z_TO_A,
    NEWEST_FIRST,
    OLDEST_FIRST;

    public static WorldSort matchWorldSort(String type) {
        if (type == null) {
            return NAME_A_TO_Z;
        }

        for (WorldSort value : values()) {
            if (value.toString().equalsIgnoreCase(type)) {
                return value;
            }
        }

        return NAME_A_TO_Z;
    }
}
