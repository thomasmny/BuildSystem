/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.version.util;

import com.google.common.collect.ComparisonChain;
import org.bukkit.Bukkit;

import javax.annotation.Nonnull;

/**
 * Utility class for retrieving and comparing minecraft server versions.
 *
 * @author IntellectualSites
 */
public class MinecraftVersion implements Comparable<MinecraftVersion> {

    public static final MinecraftVersion BOUNTIFUL_8 = new MinecraftVersion(1, 8);
    public static final MinecraftVersion AQUATIC_13 = new MinecraftVersion(1, 13);
    public static final MinecraftVersion CAVES_17 = new MinecraftVersion(1, 17);

    private static MinecraftVersion current = null;

    private final int major;
    private final int minor;
    private final int patch;

    /**
     * Construct a new version with major, minor and patch version.
     *
     * @param major Major part of the version, only {@code 1} would make sense.
     * @param minor Minor part, full updates, e.g. Nether &amp; Caves &amp; Cliffs
     * @param patch Patch, changes for the server software during a minor update.
     */
    public MinecraftVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Construct a new version with major and minor version.
     * The patch version is set to 0, therefore ignored.
     *
     * @see MinecraftVersion#MinecraftVersion(int, int, int)
     */
    public MinecraftVersion(int major, int minor) {
        this(major, minor, 0);
    }

    /**
     * Get the minecraft version that the server is currently running
     */
    public static MinecraftVersion getCurrent() {
        if (current == null) {
            return current = detectMinecraftVersion();
        }
        return current;
    }

    private static MinecraftVersion detectMinecraftVersion() {
        String[] parts = {};
        try {
            parts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        if (parts.length != 3) {
            throw new IllegalStateException("Failed to determine minecraft version: " + Bukkit.getBukkitVersion());
        }

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        return new MinecraftVersion(major, minor, patch);
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is equal to the other version.
     */
    public boolean isEqual(MinecraftVersion other) {
        return compareTo(other) == 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is higher or equal compared to the other version.
     */
    public boolean isEqualOrHigherThan(MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is lower or equal compared to the other version.
     */
    public boolean isEqualOrLowerThan(MinecraftVersion other) {
        return compareTo(other) <= 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is higher than the other version.
     */
    public boolean isHigherThan(MinecraftVersion other) {
        return compareTo(other) > 0;
    }

    /**
     * @param other The other version to compare against.
     * @return {@code true} if this version is lower than to the other version.
     */
    public boolean isLowerThan(MinecraftVersion other) {
        return compareTo(other) < 0;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public int compareTo(@Nonnull MinecraftVersion other) {
        if (other.equals(this)) {
            return 0;
        }
        return ComparisonChain.start()
                .compare(getMajor(), other.getMajor())
                .compare(getMinor(), other.getMinor())
                .compare(getPatch(), other.getPatch())
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MinecraftVersion that = (MinecraftVersion) o;
        if (getMajor() != that.getMajor()) {
            return false;
        }
        if (getMinor() != that.getMinor()) {
            return false;
        }
        return getPatch() == that.getPatch();
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}