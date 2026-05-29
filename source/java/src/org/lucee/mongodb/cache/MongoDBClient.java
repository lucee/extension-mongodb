package org.lucee.mongodb.cache;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDBClient {

    private static MongoClient instance = null;

    private MongoDBClient() {}

    public static MongoClient init(String uri) {
        try {
            instance = MongoClients.create(uri);
        } catch (MongoException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public static MongoClient getInstance() {
        return instance;
    }
}
