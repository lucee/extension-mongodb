package org.lucee.mongodb.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.Date;

import lucee.commons.io.cache.Cache;
import lucee.commons.io.cache.CacheEntry;
import lucee.commons.io.cache.CacheEntryFilter;
import lucee.commons.io.cache.CacheKeyFilter;
import lucee.commons.io.cache.exp.CacheException;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClientURI;

public class MongoDBCache implements Cache {

	private String collectionName;
	private String databaseName;
	private Boolean persists = false;

	//counters
	private int hits = 0;
	private int misses = 0;
	private Cast caster;

	public void init(String cacheName, Struct arguments) throws IOException, PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		caster = engine.getCastUtil();

		this.persists = caster.toBoolean(arguments.get("persist",false));
		this.collectionName = caster.toString(arguments.get("collection"));
		this.databaseName = caster.toString(arguments.get("database"));
		
		MongoDBClient.init(new MongoClientURI(caster.toString(arguments.get("uri"))));

		// clean the collection on startup if required
		if (!persists) {
			getCollection().drop();
		}

		//create the indexes
		createIndexes();
	}

	public static void init(Config config, String[] cacheName, Struct[] arguments) {
		//Not used at the moment
	}

	public void init(Config config, String cacheName, Struct arguments) {
		try {
			init(cacheName, arguments);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private DBCollection getCollection(){
		return MongoDBClient
				.getInstance()
				.getDB(this.databaseName)
				.getCollection(this.collectionName);
	}

	protected void createIndexes() {
		DBCollection coll = getCollection();
		// create the indexes
		coll.createIndex(new BasicDBObject("key", 1));
		coll.createIndex(new BasicDBObject("lifeSpan", 1));
		//coll.createIndex(new BasicDBObject("timeIdle", 1)); Idle is not supported from version 2
		coll.createIndex(new BasicDBObject("expires", 1));
		// this is a MongoDB native TTL index - documents will be automatically removed from collection when expireAt is reached
		coll.createIndex(new BasicDBObject("expireAt", 1), new BasicDBObject("expireAfterSeconds", 0));
	}

	@Override
	public boolean contains(String key) {
		DBCollection coll = getCollection();
		BasicDBObject query = new BasicDBObject();
		query.put("key", key.toLowerCase());
		DBCursor cur = coll.find(query);
		return cur.count() > 0;
	}

	@Override
	public List<CacheEntry> entries() {
		List<CacheEntry> result = new ArrayList<CacheEntry>();
		DBCursor cur = qAll();

		while (cur.hasNext()) {
			BasicDBObject obj = (BasicDBObject) cur.next();
			MongoDBCacheDocument doc = new MongoDBCacheDocument(obj);
			result.add(new MongoDBCacheEntry(doc));
		}

		return result;
	}

	@Override
	public List<CacheEntry> entries(CacheKeyFilter filter) {
		List<CacheEntry> result = new ArrayList<CacheEntry>();
		DBCursor cur = qAll();

		while (cur.hasNext()) {
			BasicDBObject obj = (BasicDBObject) cur.next();
			MongoDBCacheDocument doc = new MongoDBCacheDocument(obj);
			if (filter.accept(doc.getKey())) {
				result.add(new MongoDBCacheEntry(doc));
			}
		}
		return result;
	}

	@Override
	public List<CacheEntry> entries(CacheEntryFilter filter) {
		List<CacheEntry> result = new ArrayList<CacheEntry>();
		DBCursor cur = qAll();

		while (cur.hasNext()) {
			BasicDBObject obj = (BasicDBObject) cur.next();
			MongoDBCacheDocument doc = new MongoDBCacheDocument(obj);
			MongoDBCacheEntry entry = new MongoDBCacheEntry(doc);
			if (filter.accept(entry)) {
				result.add(entry);
			}
		}
		return result;
	}

	@Override
	public CacheEntry getCacheEntry(String key) throws CacheException {
		CacheEntry ce = getCacheEntry(key, null);
		if(ce!=null) return ce;
		throw new CacheException("The document with key [" + key + "] has not been found int this cache.");
	}

	public CacheEntry getCacheEntry(String key, CacheEntry defaultValue) {
		DBObject cur = null;
		DBCollection coll = getCollection();
		BasicDBObject query = new BasicDBObject("key", key.toLowerCase());

		cur = coll.findOne(query);

		if (cur != null) {
			hits++;
			MongoDBCacheDocument doc = new MongoDBCacheDocument((BasicDBObject) cur);
			//update the statistic and persist
			doc.addHit();
			save(doc,0);			
			return new MongoDBCacheEntry(doc);
		}
		misses++;
		return defaultValue;
	}

	@Override
	public Struct getCustomInfo() {
		Struct info=CFMLEngineFactory.getInstance().getCreationUtil().createStruct();

		long value = hitCount();
		if(value>=0)info.setEL("hit_count", new Double(value));
		value = missCount();
		if(value>=0)info.setEL("miss_count", new Double(value));

		return info;
	}

	@Override
	public Object getValue(String key) throws IOException {
		return getCacheEntry(key).getValue();
	}

	@Override
	public Object getValue(String key, Object defaultValue) {
		CacheEntry ce = getCacheEntry(key,null);
		if(ce!=null) return ce.getValue();
		return defaultValue;
	}

	@Override
	public long hitCount() {
		return hits;
	}

	@Override
	public List<String> keys() {
		List<String> result = new ArrayList<String>();
		DBCursor cur = qAll_Keys();

		if (cur.count() > 0) {
			while (cur.hasNext()) {
				result.add(caster.toString(cur.next().get("key"),""));
			}
		}
		return result;
	}

	@Override
	public List<String> keys(CacheKeyFilter filter) {
		List<String> result = new ArrayList<String>();
		DBCursor cur = qAll_Keys();

		if (cur.count() > 0) {
			while (cur.hasNext()) {
				String key = new MongoDBCacheDocument((BasicDBObject) cur.next()).getKey();
				if (filter.accept(key)) {
					result.add(key);
				}
			}
		}
		return result;
	}

	@Override
	public List keys(CacheEntryFilter filter) {
		List<String> result = new ArrayList<String>();
		DBCursor cur = qAll();

		if (cur.count() > 0) {
			while (cur.hasNext()) {
				MongoDBCacheEntry entry = new MongoDBCacheEntry(new MongoDBCacheDocument((BasicDBObject) cur.next()));
				if (filter.accept(entry)) {
					result.add(entry.getKey());
				}
			}
		}
		return result;
	}

	@Override
	public long missCount() {
		return misses;
	}

	@Override
	public void put(String key, Object value, Long idleTime, Long lifeSpan) {

		long created = System.currentTimeMillis();
		long idle = idleTime==null ?0:idleTime.longValue();
		long life = lifeSpan==null ?0:lifeSpan.longValue();
		long expires = 0;

		BasicDBObject obj = new BasicDBObject();
		MongoDBCacheDocument doc = new MongoDBCacheDocument(obj);
		doc.setCreatedOn(created);
		doc.setTimeIdle(idle);
		doc.setLifeSpan(life);
		doc.setHits(0);
		if (life > 0) {
			expires = life + created;
		}
		else if (idle > 0) {
			expires = idle + created;
		}
		doc.setExpires( expires );
		if (expires != 0)
			doc.setExpireAt( new Date( expires ) );

		try {
			doc.setValue(value);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		doc.setKey(key.toLowerCase());
		save(doc,created);

	}

	@Override
	public boolean remove(String key) {
		DBCollection coll = getCollection();
		BasicDBObject query = new BasicDBObject();
		query.put("key", key.toLowerCase());
		DBCursor cur = coll.find(query);
		if (cur.hasNext()) {
			doDelete(cur.next());
			return true;
		}
		return false;
	}

	@Override
	public int remove(CacheKeyFilter filter) {
		DBCursor cur = qAll_Keys();
		int counter = 0;

		while (cur.hasNext()) {
			DBObject obj = cur.next();
			String key = (String) obj.get("key");
			if (filter.accept(key)) {
				doDelete((BasicDBObject) obj);
				counter++;
			}
		}

		return counter;
	}

	@Override
	public int remove(CacheEntryFilter filter) {
		DBCursor cur = qAll();
		int counter = 0;

		while (cur.hasNext()) {
			BasicDBObject obj = (BasicDBObject) cur.next();
			MongoDBCacheEntry entry = new MongoDBCacheEntry(new MongoDBCacheDocument(obj));
			if (filter.accept(entry)) {
				doDelete(obj);
				counter++;
			}
		}

		return counter;
	}

	@Override
	public List<Object> values() {
		DBCursor cur = qAll_Values();
		List<Object> result = new ArrayList<Object>();

		while (cur.hasNext()) {
			try {
				result.add(MongoDBCacheDocument.getValue((BasicDBObject) cur.next()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	@Override
	public List<Object> values(CacheKeyFilter filter) {
		DBCursor cur = qAll_Keys_Values();
		List<Object> result = new ArrayList<Object>();

		while (cur.hasNext()) {
			BasicDBObject obj = (BasicDBObject) cur.next();
			MongoDBCacheDocument doc = new MongoDBCacheDocument(obj);

			if (filter.accept(doc.getKey())) {
				try {
					result.add(MongoDBCacheDocument.getValue(obj));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		}

		return result;
	}

	@Override
	public List<Object> values(CacheEntryFilter filter) {
		DBCursor cur = qAll_Keys_Values();
		List<Object> result = new ArrayList<Object>();

		while (cur.hasNext()) {
			BasicDBObject obj = (BasicDBObject) cur.next();
			MongoDBCacheEntry entry = new MongoDBCacheEntry(new MongoDBCacheDocument(obj));

			if (filter.accept(entry)) {
				try {
					result.add(MongoDBCacheDocument.getValue(obj));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		}

		return result;
	}

	private void doDelete(DBObject obj) {
		DBCollection coll = getCollection();
		coll.remove(obj);
	}

	private void save(MongoDBCacheDocument doc, long now) {
		DBCollection coll = getCollection();
		if(now<=0)now = System.currentTimeMillis();

		doc.setLastAccessed(now);
		doc.setLastUpdated(now);
		// BasicDBObject q = new BasicDBObject("key", doc.getKey());
		// coll.update(q, doc.getDbObject(), true, false);
		coll.save(doc.getDbObject());
	}

	private DBCursor qAll() {
		DBCollection coll = getCollection();
		DBCursor cur = null;
		cur = coll.find();
		return cur;
	}

	private DBCursor qAll_Keys() {
		DBCollection coll = getCollection();
		Integer attempts = 0;
		DBCursor cur = null;
		//get all entries but retrieve just the keys for better performance
		cur = coll.find(new BasicDBObject(), new BasicDBObject("key", 1));
		return cur;
	}

	private DBCursor qAll_Values() {
		DBCollection coll = getCollection();
		DBCursor cur = null;
		//get all entries but retrieve just the data for better performance
		cur = coll.find(new BasicDBObject(), new BasicDBObject("data", 1));
		return cur;
	}

	private DBCursor qAll_Keys_Values() {
		DBCollection coll = getCollection();
		DBCursor cur = null;

		//get all entries but retrieve just the keys and data for better performance
		cur = coll.find(new BasicDBObject(), new BasicDBObject("key", 1).append("data", 1));
		return cur;
	}

}
