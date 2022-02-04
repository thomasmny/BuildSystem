/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.world;

import java.util.UUID;

/**
 * @author einTosti
 */
public interface Builder {

    UUID getUuid();

    void setUuid(UUID uuid);

    String getName();

    void setName(String name);
}
