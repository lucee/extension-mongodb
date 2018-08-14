package org.lucee.mongodb.cache;

import java.util.ArrayList;
import java.util.List;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;

public class MongoDBClient {

    private static MongoClient instance=null;

    private MongoDBClient(){}

    public static MongoClient init(MongoClientURI clientUri){

        if(instance != null){
            return instance;
        }

        try {
            instance = new MongoClient(clientUri);
        } catch (MongoException e) {
            e.printStackTrace();
        }

        return instance;
    }

    public static MongoClient getInstance(){
        return instance;
    }
}
