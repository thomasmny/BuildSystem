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

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.eintosti.buildsystem.storage.Database;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.StorageBackend;
import de.eintosti.buildsystem.storage.StorageException;
import de.eintosti.buildsystem.storage.StorageTls;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bson.Document;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A MongoDB database, holding each collection of entities as a Mongo collection of documents.
 */
@NullMarked
public final class MongoConnection implements Database {

    private static final int SERVER_TIMEOUT_SECONDS = 10;

    private final MongoClient client;
    private final MongoDatabase database;
    private final String collectionPrefix;
    private final Logger logger;

    private MongoConnection(MongoClient client, MongoDatabase database, String collectionPrefix, Logger logger) {
        this.client = client;
        this.database = database;
        this.collectionPrefix = collectionPrefix;
        this.logger = logger;
    }

    /**
     * Connects right away, so a wrong address or login fails at startup rather than at the first save.
     *
     * @param username The user, authenticated against the {@code admin} database, or empty to connect without one
     * @throws StorageException If the server cannot be reached or rejects the login
     */
    public static MongoConnection connect(
            String host,
            int port,
            String databaseName,
            String username,
            @Nullable String password,
            StorageTls tls,
            String collectionPrefix,
            Logger logger) {
        MongoClient client = MongoClients.create(settings(host, port, username, password, tls));
        try {
            MongoDatabase database = client.getDatabase(databaseName);
            database.runCommand(new Document("ping", 1));
            return new MongoConnection(client, database, collectionPrefix, logger);
        } catch (MongoException e) {
            client.close();
            throw new StorageException("Could not reach MongoDB: " + e.getMessage(), e);
        }
    }

    static MongoClientSettings settings(
            String host, int port, String username, @Nullable String password, StorageTls tls) {
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .applyToClusterSettings(cluster -> cluster.hosts(List.of(new ServerAddress(host, port)))
                        .serverSelectionTimeout(SERVER_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .applyToSocketSettings(
                        socket -> socket.readTimeout(StorageBackend.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                // The driver cannot fall back to plain text, so prefer does what off does.
                .applyToSslSettings(ssl -> ssl.enabled(tls == StorageTls.REQUIRE || tls == StorageTls.VERIFY)
                        .invalidHostNameAllowed(tls == StorageTls.REQUIRE));
        if (!username.isEmpty()) {
            settings.credential(MongoCredential.createCredential(
                    username, "admin", (password == null ? "" : password).toCharArray()));
        }
        return settings.build();
    }

    @Override
    public boolean exists(EntityKind kind) {
        try {
            return database.listCollectionNames().into(new ArrayList<>()).contains(collectionPrefix + kind.id());
        } catch (MongoException e) {
            throw new StorageException(
                    "Could not list the collections: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void drop(EntityKind kind) {
        try {
            database.getCollection(collectionPrefix + kind.id()).drop();
        } catch (MongoException e) {
            throw new StorageException(
                    "Could not drop " + collectionPrefix + kind.id() + ": "
                            + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    e);
        }
    }

    @Override
    public EntityStore collection(EntityKind kind) {
        // MongoDB only creates a collection on the first write. Created now, so that it exists from here on like a
        // table does, and an emptied collection is not taken for a new one.
        if (!exists(kind)) {
            try {
                database.createCollection(collectionPrefix + kind.id());
            } catch (MongoException e) {
                throw new StorageException(
                        "Could not create " + collectionPrefix + kind.id() + ": "
                                + e.getClass().getSimpleName() + ": " + e.getMessage(),
                        e);
            }
        }
        return new MongoEntityStore(database.getCollection(collectionPrefix + kind.id()), logger);
    }

    @Override
    public void close() {
        client.close();
    }
}
