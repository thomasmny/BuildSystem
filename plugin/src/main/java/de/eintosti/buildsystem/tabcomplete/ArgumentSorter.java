/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

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
