/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Main package for the {@link de.eintosti.buildsystem.api.BuildSystem} API interface.
 *
 * <h2>API stability and deprecation protocol</h2>
 * <p>Public API is never removed or signature-changed without a deprecation cycle:</p>
 * <ol>
 *   <li>Mark with {@code @Deprecated(forRemoval = true, since = "&lt;version&gt;")} and add a
 *       {@code @deprecated} Javadoc tag naming the replacement.</li>
 *   <li>The deprecated member is kept until the maintainer explicitly designates a major version
 *       for removal.</li>
 *   <li>New additions must include Javadoc and a {@code @since} tag.</li>
 * </ol>
 */
package de.eintosti.buildsystem.api;