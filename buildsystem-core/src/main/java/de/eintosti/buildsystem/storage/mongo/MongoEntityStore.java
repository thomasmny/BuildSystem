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
import org.jspecify.annotations.Nullable;

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
     * leaves the documents before it written. An entry with a field name MongoDB cannot store is skipped and logged.
     */
    @Override
    public void upsert(Map<String, Map<String, Object>> entries) {
        write(storable(entries));
    }

    /**
     * Deletes every document and then writes the entries, so like {@link #upsert}, a failure part way keeps what was
     * done before it.
     */
    @Override
    public void replaceAll(Map<String, Map<String, Object>> entries) {
        List<ReplaceOneModel<Document>> replacements = storable(entries);
        run("replace", () -> collection.deleteMany(new Document()));
        write(replacements);
    }

    private List<ReplaceOneModel<Document>> storable(Map<String, Map<String, Object>> entries) {
        List<ReplaceOneModel<Document>> replacements = new ArrayList<>();
        entries.forEach((key, values) -> {
            Map<String, Object> flat = SerializedObjects.flatten(values);
            String badField = badFieldName(flat);
            if (badField != null) {
                logger.warning("Not saving \"" + key + "\" to " + name() + ": MongoDB cannot store the field name \""
                        + badField + "\"");
                return;
            }
            Document document = new Document(ID, key);
            document.putAll(flat);
            replacements.add(new ReplaceOneModel<>(Filters.eq(ID, key), document, new ReplaceOptions().upsert(true)));
        });
        return replacements;
    }

    private void write(List<ReplaceOneModel<Document>> replacements) {
        // A bulk write refuses an empty list.
        if (!replacements.isEmpty()) {
            run("save to", () -> collection.bulkWrite(replacements));
        }
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
     * {@return the first field name MongoDB would read as an operator or a path, or {@code null}} The codecs only use
     * fixed names, so there is only one if a codec starts naming fields after user input.
     */
    private static @Nullable String badFieldName(@Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> field : map.entrySet()) {
                if (!(field.getKey() instanceof String name) || name.startsWith("$") || name.contains(".")) {
                    return String.valueOf(field.getKey());
                }
                String nested = badFieldName(field.getValue());
                if (nested != null) {
                    return nested;
                }
            }
        } else if (value instanceof List<?> list) {
            for (Object element : list) {
                String nested = badFieldName(element);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
