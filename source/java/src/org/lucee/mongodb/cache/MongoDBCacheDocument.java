package org.lucee.mongodb.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import lucee.loader.util.Util;

import org.lucee.mongodb.util.Base64Encoder;
import org.lucee.mongodb.util.SerializerUtil;
import org.lucee.mongodb.util.print;

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
	
	public String getCraetedOn(){
		return dbObject.getString("createdOn");
	}
	
	public void setCraetedOn(long created){
		dbObject.put("createdOn",created);
	}
	

	
	
	public void setLastAccessed(long value) {
		dbObject.put("lastAccessed",value);
	}
	
	public long getLastAccessed(){
		return dbObject.getLong("lastAccessed");
	}

	public void setLastUpdated(long value) {
		dbObject.put("lastUpdated",value);
	}
	
	public long getLastUpdated(){
		return dbObject.getLong("lastUpdated");
	}
	
	/*public void setLifeSpan(long value) {
		dbObject.put("lifeSpan",value);
	}
	
	public long getLifeSpan(){
		return dbObject.getLong("lifeSpan");
	}*/
	
	public void setTimeIdle(long value) {
		dbObject.put("timeIdle",value);
	}
	
	public long getTimeIdle(){
		return dbObject.getLong("timeIdle");
	}

	public void setHits(int value) {
		dbObject.put("hits",value);
	}
	
	public int getHits(){
		int hits = dbObject.getInt("hits",0);
		return hits;
	}

	public void setExpires(long value) {
		print.ds("set-expires:\n"+value+"\n"+((int)value));
		dbObject.put("expires",value);
	}
	
	public long getExpires(){
		return dbObject.getLong("expires"); 		
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
