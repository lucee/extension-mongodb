package org.lucee.mongodb.cache;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDBClient {

    private static MongoClient instance = null;
    private static String currentUri   = null;

    private MongoDBClient() {}

    /**
     * Return the cached client if the URI is unchanged, otherwise close the
     * existing client and open a new one.  Synchronized to be safe against
     * concurrent Lucee cache-initialisation calls on startup.
     */
    public static synchronized MongoClient init(String uri) {
        if (instance != null && uri != null && uri.equals(currentUri)) {
            return instance; // already open for this URI — reuse it
        }
        if (instance != null) {
            try { instance.close(); } catch (Exception ignored) {}
            instance = null;
        }
        try {
            instance   = MongoClients.create(uri);
            currentUri = uri;
        } catch (MongoException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public static MongoClient getInstance() {
        return instance;
    }
}
