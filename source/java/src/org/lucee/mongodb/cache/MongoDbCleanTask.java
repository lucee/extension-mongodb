package org.lucee.mongodb.cache;

import java.util.TimerTask;

import com.mongodb.BasicDBObject;

public class MongoDbCleanTask extends TimerTask {
	
	private MongoDBCache cache;
	
	public MongoDbCleanTask(MongoDBCache cache) {
		this.cache = cache;
	}

	@Override
	public void run() {
		try{
			this.cache.flushInvalid(null,new BasicDBObject());
		}catch(Exception e){
			//just fails so that task does not die
		}
	}

}
