/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.util.exception;

/**
 * @author einTosti
 */
public class UnexpectedEnumValueException extends Exception {

    public UnexpectedEnumValueException(String value) {
        super("Unhandled value: " + value);
    }
}