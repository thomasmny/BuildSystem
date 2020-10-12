package de.eintosti.buildsystem.object.settings;

/**
 * @author einTosti
 */
public enum Colour {
    RED(14),
    ORANGE(1),
    YELLOW(4),
    PINK(6),
    MAGENTA(2),
    PURPLE(10),
    BROWN(12),
    LIME(5),
    GREEN(13),
    BLUE(11),
    CYAN(9),
    LIGHT_BLUE(3),
    WHITE(0),
    GREY(8),
    LIGHT_GREY(7),
    BLACK(15);

    private final int id;

    Colour(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
