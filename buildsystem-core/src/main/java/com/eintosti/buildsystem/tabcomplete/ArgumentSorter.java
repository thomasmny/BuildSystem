/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.tabcomplete;

import com.eintosti.buildsystem.util.external.StringUtils;

import java.util.List;

/**
 * @author einTosti
 */
abstract class ArgumentSorter {

    public void addArgument(String input, String argument, List<String> arrayList) {
        if (input.equals("") || StringUtils.startsWithIgnoreCase(argument, input)) {
            arrayList.add(argument);
        }
    }
}