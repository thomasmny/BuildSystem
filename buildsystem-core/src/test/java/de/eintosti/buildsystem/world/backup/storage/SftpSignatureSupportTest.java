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
package de.eintosti.buildsystem.world.backup.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sshd.common.signature.BuiltinSignatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The SFTP backup verifies host keys with RSA and Ed25519 signatures. Both used to require BouncyCastle to be
 * registered as a security provider, which cost ~19 MB in the shaded jar. The JDK has supported Ed25519 natively
 * since 15 and this plugin targets 25, so the provider was dropped — this pins that the algorithms are still there
 * without it, since losing one would silently break connecting to a backup server.
 */
class SftpSignatureSupportTest {

    @Test
    @DisplayName("Host-key signatures resolve from the JDK, with no BouncyCastle on the classpath")
    void signaturesSupportedWithoutBouncyCastle() {
        assertTrue(BuiltinSignatures.rsa.isSupported(), "RSA host keys must stay verifiable");
        assertTrue(BuiltinSignatures.ed25519.isSupported(), "Ed25519 host keys must stay verifiable");
    }
}
