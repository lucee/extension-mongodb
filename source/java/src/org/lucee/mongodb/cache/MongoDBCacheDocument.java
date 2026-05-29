package org.lucee.mongodb.cache;

import java.io.Serializable;
import java.util.Date;

import org.bson.Document;
import org.lucee.mongodb.util.SerializerUtil;

public class MongoDBCacheDocument implements Serializable {

	private static final long serialVersionUID = -7033308305053665899L;

	private Document dbObject;

	public MongoDBCacheDocument(Document dbObject) {
		this.dbObject = dbObject;
	}

	public String getId() {
		Object id = dbObject.get("_id");
		return id != null ? id.toString() : null;
	}

	public void setValue(Object value) throws Exception {
		dbObject.put("data", SerializerUtil.serialize(value));
	}

	public Object getValue() throws Exception {
		return getValue(dbObject);
	}

	public static Object getValue(Document dbObject) throws Exception {
		return SerializerUtil.evaluate((String) dbObject.get("data"));
	}

	public void setKey(String value) {
		dbObject.put("key", value);
		dbObject.put("_id", value);
	}

	public String getKey() {
		return (String) dbObject.get("key");
	}

	public long getCreatedOn() {
		Object v = dbObject.get("createdOn");
		return v instanceof Number ? ((Number) v).longValue() : 0L;
	}

	public void setCreatedOn(long created) {
		dbObject.put("createdOn", created);
	}

	public void setLastAccessed(long value) {
		dbObject.put("lastAccessed", value);
	}

	public long getLastAccessed() {
		Object v = dbObject.get("lastAccessed");
		return v instanceof Number ? ((Number) v).longValue() : 0L;
	}

	public void setLastUpdated(long value) {
		dbObject.put("lastUpdated", value);
	}

	public long getLastUpdated() {
		Object v = dbObject.get("lastUpdated");
		return v instanceof Number ? ((Number) v).longValue() : 0L;
	}

	public void setLifeSpan(long value) {
		dbObject.put("lifeSpan", value);
	}

	public long getLifeSpan() {
		Object v = dbObject.get("lifeSpan");
		return v instanceof Number ? ((Number) v).longValue() : 0L;
	}

	public void setTimeIdle(long value) {
		dbObject.put("timeIdle", value);
	}

	public long getTimeIdle() {
		Object v = dbObject.get("timeIdle");
		return v instanceof Number ? ((Number) v).longValue() : 0L;
	}

	public void setHits(int value) {
		dbObject.put("hits", value);
	}

	public int getHits() {
		Object v = dbObject.get("hits");
		return v instanceof Number ? ((Number) v).intValue() : 0;
	}

	public void setExpires(long value) {
		dbObject.put("expires", value);
	}

	public long getExpires() {
		Object v = dbObject.get("expires");
		return v instanceof Number ? ((Number) v).longValue() : 0L;
	}

	public void setExpireAt(Date value) {
		dbObject.put("expireAt", value);
	}

	public Object getExpireAt() {
		return dbObject.get("expireAt");
	}

	public Document getDbObject() {
		return dbObject;
	}

	public void addHit() {
		int hits = getHits();
		long expires = getExpires();
		long idle = getTimeIdle();
		hits++;
		setHits(hits);
		if (expires > 0 && idle > 0) {
			expires = System.currentTimeMillis() + idle;
			setExpires(expires);
			setExpireAt(new Date(expires));
		}
	}
}
