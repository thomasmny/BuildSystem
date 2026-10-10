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

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.SerializedObjects;
import de.eintosti.buildsystem.storage.StorageException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import java.util.logging.Level;
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
        List<Document> documents =
                run("load", () -> collection.find().sort(Sorts.ascending(ID)).into(new ArrayList<>()));
        for (Document document : documents) {
            Object id = document.remove(ID);
            if (!(id instanceof String key)) {
                logger.warning("Skipping document " + id + " of " + name() + ": its _id is not a string");
                continue;
            }
            try {
                entries.put(key, SerializedObjects.revive(document));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Skipping document " + key + " of " + name() + ": could not be read", e);
            }
        }
        return entries;
    }

    @Override
    public boolean isEmpty() {
        return run("read", () -> collection.countDocuments(new Document(), new CountOptions().limit(1))) == 0;
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
            Map<String, Object> flat = SerializedObjects.flatten(values);
            checkFieldNames(flat);
            Document document = new Document(ID, key);
            document.putAll(flat);
            replacements.add(new ReplaceOneModel<>(Filters.eq(ID, key), document, new ReplaceOptions().upsert(true)));
        });
        run("save to", () -> collection.bulkWrite(replacements));
    }

    /**
     * Deletes every document and then writes the entries, so like {@link #upsert}, a failure part way keeps what was
     * done before it.
     */
    @Override
    public void replaceAll(Map<String, Map<String, Object>> entries) {
        entries.values().forEach(values -> checkFieldNames(SerializedObjects.flatten(values)));
        run("replace", () -> collection.deleteMany(new Document()));
        upsert(entries);
    }

    @Override
    public void delete(String key) {
        run("delete from", () -> collection.deleteOne(Filters.eq(ID, key)));
    }

    private <R> R run(String action, Supplier<R> work) {
        try {
            return work.get();
        } catch (MongoException e) {
            throw new StorageException(
                    "Could not " + action + " " + name() + ": " + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    e);
        }
    }

    private String name() {
        return collection.getNamespace().getCollectionName();
    }

    /**
     * Rejects a field name MongoDB would read as an operator or a path. The codecs only use fixed names, so this only
     * fails if a codec starts naming fields after user input.
     */
    private static void checkFieldNames(Map<?, ?> values) {
        values.forEach((name, value) -> {
            if (!(name instanceof String field) || field.startsWith("$") || field.contains(".")) {
                throw new IllegalArgumentException("Cannot store the field name \"" + name + "\" in MongoDB");
            }
            if (value instanceof Map<?, ?> nested) {
                checkFieldNames(nested);
            }
        });
    }
}
