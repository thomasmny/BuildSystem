package de.eintosti.buildsystem.tabcomplete;

import java.util.ArrayList;

/**
 * @author einTosti
 */
abstract class ArgumentSorter {

    public void addArgument(String argument, String name, ArrayList<String> arrayList) {
        if (!argument.equals("")) {
            if (name.toLowerCase().startsWith(argument.toLowerCase())) {
                arrayList.add(name);
            }
        } else {
            arrayList.add(name);
        }
    }
}
