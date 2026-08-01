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
package de.eintosti.buildsystem.world.backup.storage.s3;

import de.eintosti.buildsystem.world.backup.storage.s3.S3Client.S3Object;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * One page of a {@code ListObjectsV2} response.
 *
 * @param objects The objects on this page
 * @param nextContinuationToken The token for the following page, or {@code null} when this is the last one
 */
@NullMarked
record Listing(List<S3Object> objects, @Nullable String nextContinuationToken) {

    /**
     * Parses a {@code ListObjectsV2} response.
     *
     * @param xml The response document
     * @return The parsed page
     * @throws IOException If the document is not a listing this client understands
     */
    static Listing parse(byte[] xml) throws IOException {
        Document document = S3Xml.parse(xml, "listing");
        Element root = document.getDocumentElement();

        List<S3Object> objects = new ArrayList<>();
        NodeList contents = document.getElementsByTagName("Contents");
        for (int i = 0; i < contents.getLength(); i++) {
            Element entry = (Element) contents.item(i);
            objects.add(new S3Object(S3Xml.required(entry, "Key"), lastModified(entry)));
        }

        boolean truncated = Boolean.parseBoolean(S3Xml.optional(root, "IsTruncated"));
        return new Listing(objects, truncated ? S3Xml.optional(root, "NextContinuationToken") : null);
    }

    private static Instant lastModified(Element entry) throws IOException {
        String value = S3Xml.required(entry, "LastModified");
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IOException("S3 returned an unreadable LastModified: " + value, e);
        }
    }
}
