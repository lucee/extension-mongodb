package org.lucee.mongodb.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import lucee.loader.util.Util;

import org.lucee.mongodb.util.Base64Encoder;
import org.lucee.mongodb.util.SerializerUtil;

import com.mongodb.BasicDBObject;

public class MongoDBCacheDocument implements Serializable {

	private BasicDBObject dbObject;
	
	public MongoDBCacheDocument(BasicDBObject dbObject){
		this.dbObject = dbObject;
	}

	public String getId(){
		return dbObject.getString("_id");
	}

	public void setValue(Object value) throws IOException {
		dbObject.put("data",SerializerUtil.serialize(value));
	}
	
	public Object getValue() throws ClassNotFoundException, IOException{
		return getValue(dbObject);
	}

	public static Object getValue(BasicDBObject dbObject) throws ClassNotFoundException, IOException{
		return SerializerUtil.evaluate(dbObject.getString("data"));
	}

	public void setKey(String value) {
		dbObject.put("key",value);
	}
	
	public String getKey(){
		return dbObject.getString("key");
	}

	public void setCreatedOn(int value) {
		dbObject.put("createdOn",value);
	}
	
	public String getCraetedOn(){
		return dbObject.getString("createdOn");
	}

	public void setLastAccessed(int value) {
		dbObject.put("lastAccessed",value);
	}
	
	public String getLastAccessed(){
		return dbObject.getString("lastAccessed");
	}

	public void setLastUpdated(int value) {
		dbObject.put("lastUpdated",value);
	}
	
	public String getLastUpdated(){
		return dbObject.getString("lastUpdated");
	}
	
	public void setLifeSpan(int value) {
		dbObject.put("lifeSpan",value);
	}
	
	public String getLifeSpan(){
		return dbObject.getString("lifeSpan");
	}
	
	public void setTimeIdle(int value) {
		dbObject.put("timeIdle",value);
	}
	
	public String getTimeIdle(){
		return dbObject.getString("timeIdle");
	}

	public void setHits(int value) {
		dbObject.put("hits",value);
	}
	
	public int getHits(){
		int hits = dbObject.getInt("hits",0);
		return hits;
	}

	public void setExpires(int value) {
		dbObject.put("expires",value);
	}
	
	public String getExpires(){
		return dbObject.getString("expires"); 		
	}

	public BasicDBObject getDbObject(){
		return dbObject;
	}
	
	public void addHit(){
		int hits = getHits();
		hits++;
		setHits(hits);		
	}
}
