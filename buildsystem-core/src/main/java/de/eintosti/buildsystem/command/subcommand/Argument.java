/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command.subcommand;

public interface Argument {

    /**
     * The name of the command argument.
     *
     * @return The command argument name
     */
    String getName();

    /**
     * The permission required to run the command.
     *
     * @return The permission required to run the command
     */
    String getPermission();
}