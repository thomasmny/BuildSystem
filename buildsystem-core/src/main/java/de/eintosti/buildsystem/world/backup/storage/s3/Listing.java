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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
        Document document = parseSafely(xml);

        List<S3Object> objects = new ArrayList<>();
        NodeList contents = document.getElementsByTagName("Contents");
        for (int i = 0; i < contents.getLength(); i++) {
            Element entry = (Element) contents.item(i);
            objects.add(new S3Object(required(entry, "Key"), lastModified(entry)));
        }

        boolean truncated = Boolean.parseBoolean(optional(document, "IsTruncated"));
        return new Listing(objects, truncated ? optional(document, "NextContinuationToken") : null);
    }

    private static Instant lastModified(Element entry) throws IOException {
        String value = required(entry, "LastModified");
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IOException("S3 returned an unreadable LastModified: " + value, e);
        }
    }

    /**
     * {@return the document parsed with entity resolution disabled} The response is remote input, so a hostile or
     * compromised endpoint must not be able to make the parser read local files or fetch URLs.
     */
    private static Document parseSafely(byte[] xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Could not parse the S3 listing", e);
        }
    }

    private static String required(Element parent, String tag) throws IOException {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            throw new IOException("S3 listing entry is missing <" + tag + ">");
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static @Nullable String optional(Document document, String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }
}
