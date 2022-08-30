package org.lucee.mongodb.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import org.lucee.mongodb.util.SerializerUtil;

import com.mongodb.BasicDBObject;

public class MongoDBCacheDocument implements Serializable {

	private static final long serialVersionUID = -7033308305053665899L;
	
	private BasicDBObject dbObject;
	
	public MongoDBCacheDocument(BasicDBObject dbObject){
		this.dbObject = dbObject;
	}

	public String getId(){
		return dbObject.getString("_id");
	}

	public void setValue(Object value) throws Exception {
		dbObject.put("data",SerializerUtil.serialize(value));
	}
	
	public Object getValue() throws Exception{
		return getValue(dbObject);
	}

	public static Object getValue(BasicDBObject dbObject) throws Exception{
		return SerializerUtil.evaluate(dbObject.getString("data"));
	}

	public void setKey(String value) {
		dbObject.put("key",value);
		dbObject.put("_id",value);
	}
	
	public String getKey(){
		return dbObject.getString("key");
	}
	
	public String getCreatedOn(){
		return dbObject.getString("createdOn");
	}
	
	public void setCreatedOn(long created){
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
	
	public void setLifeSpan(long value) {
		dbObject.put("lifeSpan",value);
	}
	
	public long getLifeSpan(){
		return dbObject.getLong("lifeSpan");
	}
	
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
		dbObject.put("expires",value);
	}
	
	public long getExpires(){
		return dbObject.getLong("expires"); 		
	}

	public void setExpireAt(Date value) {
		dbObject.put("expireAt",value);
	}

	public Object getExpireAt(){
		return dbObject.get("expireAt");	
	}

	public BasicDBObject getDbObject(){
		return dbObject;
	}
	
	public void addHit(){
		int hits = getHits();
		long expires = getExpires();
		long idle = getTimeIdle();
		hits++;
		setHits(hits);
		if (expires > 0 && idle > 0) {
			expires = System.currentTimeMillis() + idle;
			setExpires(expires);
			setExpireAt( new Date(expires) );
		}
	}

}
