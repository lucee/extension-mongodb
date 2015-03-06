package org.lucee.mongodb.cache;

import java.util.Date;

import lucee.commons.io.cache.CacheEntry;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.type.Struct;

public class MongoDBCacheEntry implements CacheEntry {
	
	private MongoDBCacheDocument doc; 
	
	public MongoDBCacheEntry(MongoDBCacheDocument doc) {
		this.doc = doc;
	}
	
	@Override
	public Date created() {
		Date date = new Date(new Long(doc.getCraetedOn()));
		return date;
	}

	@Override
	public Struct getCustomInfo() {
		Struct metadata = CFMLEngineFactory.getInstance().getCreationUtil().createStruct();
		metadata.setEL("hits", hitCount());			
		
		return metadata;
	}

	@Override
	public String getKey() {
		String key = doc.getKey();
		return key;
	}

	@Override
	public Object getValue() {
		try{
			return doc.getValue();
		}
		catch(Exception e){
			throw new RuntimeException(e);
		}	
	}

	@Override
	public int hitCount() {
		int hits = doc.getHits();
		return hits;
	}

	@Override
	public long idleTimeSpan() {
		return new Long(doc.getTimeIdle());
	}

	@Override
	public Date lastHit() {
		return new Date(new Long(doc.getLastAccessed()));
	}

	@Override
	public Date lastModified() {
		return new Date(new Long(doc.getLastUpdated()));
	}

	@Override
	public long liveTimeSpan() {
		return new Long(doc.getExpires());
	}

	@Override
	public long size() {
		return 0;
	}

}
