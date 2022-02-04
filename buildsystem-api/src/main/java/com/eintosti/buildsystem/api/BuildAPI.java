/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api;

import com.eintosti.buildsystem.api.world.BuildWorld;

import java.util.List;

/**
 * @author einTosti
 */
public interface BuildAPI {

    List<BuildWorld> getBuildWorlds();

    BuildWorld getBuildWorld(String name);
}
