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

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
 * Reads the XML documents S3 answers with. In one place so every response goes through the same hardened parser
 * configuration.
 */
@NullMarked
final class S3Xml {

    private S3Xml() {}

    /**
     * {@return the document parsed with entity resolution disabled} A response is remote input, so a hostile or
     * compromised endpoint must not be able to make the parser read local files or fetch URLs.
     *
     * @param xml The response document
     * @param what What was being read, for the error message
     * @throws IOException If the bytes are not parseable XML
     */
    static Document parse(byte[] xml, String what) throws IOException {
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
            throw new IOException("Could not parse the S3 " + what, e);
        }
    }

    /**
     * {@return the text of the first {@code tag} below {@code parent}}
     *
     * @param parent The element to search
     * @param tag The tag name
     * @throws IOException If the element is absent
     */
    static String required(Element parent, String tag) throws IOException {
        String value = optional(parent, tag);
        if (value == null) {
            throw new IOException("S3 response is missing <" + tag + ">");
        }
        return value;
    }

    /**
     * {@return the text of the first {@code tag} below {@code parent}, or {@code null} if there is none}
     *
     * @param parent The element to search
     * @param tag The tag name
     */
    static @Nullable String optional(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent() == null) {
            return null;
        }
        return nodes.item(0).getTextContent().trim();
    }

    /**
     * Escapes text for inclusion in a request document.
     *
     * @param value The text to escape
     * @return The escaped text
     */
    static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
