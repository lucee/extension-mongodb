package org.lucee.mongodb.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.IndexOptions;

public class MongoDBCache implements Cache {

	private String collectionName;
	private String databaseName;
	private Boolean persists = false;

	private int hits = 0;
	private int misses = 0;
	private Cast caster;

	public void init(String cacheName, Struct arguments) throws IOException, PageException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		caster = engine.getCastUtil();

		this.persists = caster.toBoolean(arguments.get("persist", false));
		this.collectionName = caster.toString(arguments.get("collection"));
		this.databaseName = caster.toString(arguments.get("database"));

		MongoDBClient.init(caster.toString(arguments.get("uri")));

		if (!persists) {
			getCollection().drop();
		}
		createIndexes();
	}

	public static void init(Config config, String[] cacheName, Struct[] arguments) {
		// Not used
	}

	public void init(Config config, String cacheName, Struct arguments) {
		try {
			init(cacheName, arguments);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MongoCollection<Document> getCollection() {
		return MongoDBClient.getInstance()
			.getDatabase(this.databaseName)
			.getCollection(this.collectionName);
	}

	protected void createIndexes() {
		MongoCollection<Document> coll = getCollection();
		coll.createIndex(new Document("key", 1));
		coll.createIndex(new Document("lifeSpan", 1));
		coll.createIndex(new Document("expires", 1));
		coll.createIndex(new Document("expireAt", 1), new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
	}

	@Override
	public boolean contains(String key) {
		return getCollection().countDocuments(new Document("key", key.toLowerCase())) > 0;
	}

	@Override
	public List<CacheEntry> entries() {
		List<CacheEntry> result = new ArrayList<CacheEntry>();
		try (MongoCursor<Document> cur = getCollection().find().iterator()) {
			while (cur.hasNext()) {
				result.add(new MongoDBCacheEntry(new MongoDBCacheDocument(cur.next())));
			}
		}
		return result;
	}

	@Override
	public List<CacheEntry> entries(CacheKeyFilter filter) {
		List<CacheEntry> result = new ArrayList<CacheEntry>();
		try (MongoCursor<Document> cur = getCollection().find().iterator()) {
			while (cur.hasNext()) {
				MongoDBCacheDocument doc = new MongoDBCacheDocument(cur.next());
				if (filter.accept(doc.getKey())) result.add(new MongoDBCacheEntry(doc));
			}
		}
		return result;
	}

	@Override
	public List<CacheEntry> entries(CacheEntryFilter filter) {
		List<CacheEntry> result = new ArrayList<CacheEntry>();
		try (MongoCursor<Document> cur = getCollection().find().iterator()) {
			while (cur.hasNext()) {
				MongoDBCacheEntry entry = new MongoDBCacheEntry(new MongoDBCacheDocument(cur.next()));
				if (filter.accept(entry)) result.add(entry);
			}
		}
		return result;
	}

	@Override
	public CacheEntry getCacheEntry(String key) throws CacheException {
		CacheEntry ce = getCacheEntry(key, null);
		if (ce != null) return ce;
		throw new CacheException("The document with key [" + key + "] has not been found in this cache.");
	}

	public CacheEntry getCacheEntry(String key, CacheEntry defaultValue) {
		Document found = getCollection().find(new Document("key", key.toLowerCase())).first();
		if (found != null) {
			hits++;
			MongoDBCacheDocument doc = new MongoDBCacheDocument(found);
			doc.addHit();
			save(doc, 0);
			return new MongoDBCacheEntry(doc);
		}
		misses++;
		return defaultValue;
	}

	@Override
	public Struct getCustomInfo() {
		Struct info = CFMLEngineFactory.getInstance().getCreationUtil().createStruct();
		info.setEL("hit_count", new Double(hitCount()));
		info.setEL("miss_count", new Double(missCount()));
		return info;
	}

	@Override
	public Object getValue(String key) throws IOException {
		return getCacheEntry(key).getValue();
	}

	@Override
	public Object getValue(String key, Object defaultValue) {
		CacheEntry ce = getCacheEntry(key, null);
		return ce != null ? ce.getValue() : defaultValue;
	}

	@Override
	public long hitCount() {
		return hits;
	}

	@Override
	public List<String> keys() {
		List<String> result = new ArrayList<String>();
		try (MongoCursor<Document> cur = getCollection().find().projection(new Document("key", 1)).iterator()) {
			while (cur.hasNext()) {
				Object k = cur.next().get("key");
				if (k != null) result.add(k.toString());
			}
		}
		return result;
	}

	@Override
	public List<String> keys(CacheKeyFilter filter) {
		List<String> result = new ArrayList<String>();
		try (MongoCursor<Document> cur = getCollection().find().projection(new Document("key", 1)).iterator()) {
			while (cur.hasNext()) {
				Object k = cur.next().get("key");
				if (k != null && filter.accept(k.toString())) result.add(k.toString());
			}
		}
		return result;
	}

	@Override
	public List keys(CacheEntryFilter filter) {
		List<String> result = new ArrayList<String>();
		try (MongoCursor<Document> cur = getCollection().find().iterator()) {
			while (cur.hasNext()) {
				MongoDBCacheEntry entry = new MongoDBCacheEntry(new MongoDBCacheDocument(cur.next()));
				if (filter.accept(entry)) result.add(entry.getKey());
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
		long idle = idleTime == null ? 0 : idleTime.longValue();
		long life = lifeSpan == null ? 0 : lifeSpan.longValue();
		long expires = 0;

		Document obj = new Document();
		MongoDBCacheDocument doc = new MongoDBCacheDocument(obj);
		doc.setCreatedOn(created);
		doc.setTimeIdle(idle);
		doc.setLifeSpan(life);
		doc.setHits(0);
		if (life > 0) expires = life + created;
		else if (idle > 0) expires = idle + created;
		doc.setExpires(expires);
		if (expires != 0) doc.setExpireAt(new Date(expires));

		try {
			doc.setValue(value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		doc.setKey(key.toLowerCase());
		save(doc, created);
	}

	@Override
	public boolean remove(String key) {
		Document query = new Document("key", key.toLowerCase());
		Document found = getCollection().find(query).first();
		if (found != null) {
			getCollection().deleteOne(new Document("_id", found.get("_id")));
			return true;
		}
		return false;
	}

	@Override
	public int remove(CacheKeyFilter filter) {
		int counter = 0;
		try (MongoCursor<Document> cur = getCollection().find().projection(new Document("key", 1)).iterator()) {
			while (cur.hasNext()) {
				Document obj = cur.next();
				String k = (String) obj.get("key");
				if (filter.accept(k)) {
					getCollection().deleteOne(new Document("_id", obj.get("_id")));
					counter++;
				}
			}
		}
		return counter;
	}

	@Override
	public int remove(CacheEntryFilter filter) {
		int counter = 0;
		try (MongoCursor<Document> cur = getCollection().find().iterator()) {
			while (cur.hasNext()) {
				Document obj = cur.next();
				MongoDBCacheEntry entry = new MongoDBCacheEntry(new MongoDBCacheDocument(obj));
				if (filter.accept(entry)) {
					getCollection().deleteOne(new Document("_id", obj.get("_id")));
					counter++;
				}
			}
		}
		return counter;
	}

	@Override
	public List<Object> values() {
		List<Object> result = new ArrayList<Object>();
		try (MongoCursor<Document> cur = getCollection().find().projection(new Document("data", 1)).iterator()) {
			while (cur.hasNext()) {
				try { result.add(MongoDBCacheDocument.getValue(cur.next())); }
				catch (Exception e) { throw new RuntimeException(e); }
			}
		}
		return result;
	}

	@Override
	public List<Object> values(CacheKeyFilter filter) {
		List<Object> result = new ArrayList<Object>();
		try (MongoCursor<Document> cur = getCollection().find().projection(new Document("key", 1).append("data", 1)).iterator()) {
			while (cur.hasNext()) {
				Document obj = cur.next();
				MongoDBCacheDocument doc = new MongoDBCacheDocument(obj);
				if (filter.accept(doc.getKey())) {
					try { result.add(MongoDBCacheDocument.getValue(obj)); }
					catch (Exception e) { throw new RuntimeException(e); }
				}
			}
		}
		return result;
	}

	@Override
	public List<Object> values(CacheEntryFilter filter) {
		List<Object> result = new ArrayList<Object>();
		try (MongoCursor<Document> cur = getCollection().find().projection(new Document("key", 1).append("data", 1)).iterator()) {
			while (cur.hasNext()) {
				Document obj = cur.next();
				MongoDBCacheEntry entry = new MongoDBCacheEntry(new MongoDBCacheDocument(obj));
				if (filter.accept(entry)) {
					try { result.add(MongoDBCacheDocument.getValue(obj)); }
					catch (Exception e) { throw new RuntimeException(e); }
				}
			}
		}
		return result;
	}

	private void save(MongoDBCacheDocument doc, long now) {
		if (now <= 0) now = System.currentTimeMillis();
		doc.setLastAccessed(now);
		doc.setLastUpdated(now);
		Document filter = new Document("_id", doc.getDbObject().get("_id"));
		getCollection().replaceOne(filter, doc.getDbObject(), new com.mongodb.client.model.ReplaceOptions().upsert(true));
	}
}
