package org.lucee.mongodb.cache;

import java.util.ArrayList;
import java.util.List;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

public class MongoConnection {

    private static Mongo instance;

    private MongoConnection(){}

    public static Mongo init(Struct arguments){

        if(instance != null){
            return instance;
        }

        CFMLEngine engine = CFMLEngineFactory.getInstance();
        Cast caster = engine.getCastUtil();
        MongoOptions opts = new MongoOptions();
        List<ServerAddress> addr = new ArrayList<ServerAddress>();

        try{
            //options
            opts.connectionsPerHost = caster.toIntValue(arguments.get("connectionsPerHost"));

            String[] hosts = caster.toString(arguments.get("hosts")).split("\\n");

            for (int i = 0; i < hosts.length; i++) {
                addr.add(new ServerAddress(hosts[i]));
            }

            //create the mongo instance
            try {
                instance = new Mongo(addr, opts);
            } catch (MongoException e) {
                e.printStackTrace();
            }

            /* authenticate if required
             TODO: deprecated, must use MongoCredential
            String username = caster.toString(arguments.get("username"));
            char[] password = caster.toString(arguments.get("password")).toCharArray();
            String database = caster.toString(arguments.get("database"));
            instance.getDB(database).authenticate(username, password);*/

        }catch(Exception e){
            e.printStackTrace();
        }

        return instance;
    }

    public static Mongo getInstance(){
        return instance;
    }

}
