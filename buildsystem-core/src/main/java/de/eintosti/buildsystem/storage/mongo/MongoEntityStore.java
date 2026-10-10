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
package de.eintosti.buildsystem.storage.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import de.eintosti.buildsystem.storage.EntityStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.logging.Logger;
import org.bson.Document;
import org.jspecify.annotations.NullMarked;

/**
 * One Mongo collection, an entry being a document whose {@code _id} is the entry's key. Documents load in {@code _id}
 * order.
 */
@NullMarked
final class MongoEntityStore implements EntityStore {

    private static final String ID = "_id";

    private final MongoCollection<Document> collection;
    private final Logger logger;

    MongoEntityStore(MongoCollection<Document> collection, Logger logger) {
        this.collection = collection;
        this.logger = logger;
    }

    @Override
    public SequencedMap<String, Map<String, Object>> loadAll() {
        SequencedMap<String, Map<String, Object>> entries = new LinkedHashMap<>();
        for (Document document : collection.find().sort(Sorts.ascending(ID))) {
            Object id = document.remove(ID);
            if (id instanceof String key) {
                entries.put(key, document);
            } else {
                logger.warning("Skipping document " + id + " of "
                        + collection.getNamespace().getCollectionName() + ": its _id is not a string");
            }
        }
        return entries;
    }

    @Override
    public boolean isEmpty() {
        return collection.countDocuments(new Document(), new CountOptions().limit(1)) == 0;
    }

    /**
     * Writes the entries in one ordered bulk write. MongoDB applies it document by document, so a failure part way
     * leaves the documents before it written.
     */
    @Override
    public void upsert(Map<String, Map<String, Object>> entries) {
        if (entries.isEmpty()) {
            return;
        }
        List<ReplaceOneModel<Document>> replacements = new ArrayList<>();
        entries.forEach((key, values) -> {
            Document document = new Document(ID, key);
            document.putAll(values);
            replacements.add(new ReplaceOneModel<>(Filters.eq(ID, key), document, new ReplaceOptions().upsert(true)));
        });
        collection.bulkWrite(replacements);
    }

    @Override
    public void delete(String key) {
        collection.deleteOne(Filters.eq(ID, key));
    }
}
