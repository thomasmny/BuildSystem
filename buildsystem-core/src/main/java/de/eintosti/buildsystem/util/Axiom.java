package de.eintosti.buildsystem.util;

public class Axiom {

    public static boolean isAxiomAvailable() {
        try {
            Class.forName("com.moulberry.axiom.AxiomPaper");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


}
