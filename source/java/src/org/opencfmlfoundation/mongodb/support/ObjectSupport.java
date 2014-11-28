package org.opencfmlfoundation.mongodb.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.opencfmlfoundation.mongodb.AggregationOutputImpl;
import org.opencfmlfoundation.mongodb.CommandResultImpl;
import org.opencfmlfoundation.mongodb.DBCollectionImpl;
import org.opencfmlfoundation.mongodb.DBCursorImpl;
import org.opencfmlfoundation.mongodb.DBImpl;
import org.opencfmlfoundation.mongodb.DBObjectImpl;
import org.opencfmlfoundation.mongodb.ObjectIdImpl;
import org.opencfmlfoundation.mongodb.util.SimpleDumpData;
import org.opencfmlfoundation.mongodb.util.print;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

import railo.loader.engine.CFMLEngine;
import railo.loader.engine.CFMLEngineFactory;
import railo.runtime.PageContext;
import railo.runtime.dump.DumpData;
import railo.runtime.dump.DumpProperties;
import railo.runtime.dump.Dumpable;
import railo.runtime.exp.PageException;
import railo.runtime.type.Array;
import railo.runtime.type.Collection;
import railo.runtime.type.Struct;
import railo.runtime.type.Collection.Key;
import railo.runtime.util.Cast;
import railo.runtime.util.Creation;
import railo.runtime.util.Decision;
import railo.runtime.util.Excepton;

public class ObjectSupport {
	private CFMLEngine engine;
	protected Cast caster;
	protected Excepton exp;
	protected Creation creator;
	protected Decision decision;

	public ObjectSupport(){
		engine=CFMLEngineFactory.getInstance();
		caster=engine.getCastUtil();
		exp=engine.getExceptionUtil();
		creator=engine.getCreationUtil();
		decision=engine.getDecisionUtil();
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException("this operation is not suppored");
	}

	public int checkArgLength(String functionName,Object[] arguments,int min, int max) throws PageException { 
		if(arguments==null) arguments=new Object[0];
		if(min>=0 && arguments.length<min)
			throw exp.createApplicationException("the function "+functionName+" needs at least "+min+" arguments, but you have defined only "+(arguments==null?0:arguments.length));
		if(max>=0 && arguments.length>max)
			throw exp.createApplicationException("the function "+functionName+" only support up to "+max+" arguments, but you have defined "+(arguments.length));
		return arguments.length;
	}

	public DBObject[] toDBObjectArray(Object obj) throws PageException {
		if(decision.isArray(obj)) {
			Array arr = caster.toArray(obj);
			DBObject[] objs=new DBObject[arr.size()];
			Iterator<Object> it = arr.valueIterator();
			int index=0;
			while(it.hasNext()){
				objs[index++]=toDBObject(it.next());
			}
			return objs;
		}
		return new DBObject[]{toDBObject(obj)};
	}
	
	public DBObject toDBObject(Struct sct) {
		return new BasicDBObject(toMongo(sct));
	}
	
	public DBObject toDBObject(Object obj) throws PageException {
		if(obj instanceof DBObject) return (DBObject) obj;
		return toDBObject(caster.toMap(obj),null);
	}
	
	public DBObject toDBObject(Object obj, DBObject defaultValue) {
		if(obj instanceof DBObject) return (DBObject) obj;
		Object mo = toMongo(obj);
		if(mo instanceof Map) return new BasicDBObject((Map)mo);
		return defaultValue;
	}


	public WriteConcern toWriteConcern(Object obj, WriteConcern defaultValue) {
		if(obj instanceof WriteConcern) return (WriteConcern) obj;
		if(decision.isSimpleValue(obj)) {
			String str = caster.toString(obj,"");
			str=str.trim().toUpperCase();
			if("ACKNOWLEDGED".equals(str))
				return WriteConcern.ACKNOWLEDGED;
			else if("ERRORS_IGNORED".equals(str) || "ERRORSIGNORED".equals(str))
				return WriteConcern.ERRORS_IGNORED;
			else if("ACKNOWLEDGED".equals(str))
				return WriteConcern.FSYNC_SAFE;
			else if("FSYNC_SAFE".equals(str) || "FSYNCSAFE".equals(str))
				return WriteConcern.FSYNCED;
			else if("JOURNAL_SAFE".equals(str) || "JOURNALSAFE".equals(str))
				return WriteConcern.JOURNAL_SAFE;
			else if("JOURNALED".equals(str))
				return WriteConcern.JOURNALED;
			else if("MAJORITY".equals(str))
				return WriteConcern.MAJORITY;
			else if("NONE".equals(str))
				return WriteConcern.NONE;
			else if("NORMAL".equals(str))
				return WriteConcern.NORMAL;
			else if("REPLICA_ACKNOWLEDGED".equals(str) || "REPLICAACKNOWLEDGED".equals(str))
				return WriteConcern.REPLICA_ACKNOWLEDGED;
			else if("REPLICAS_SAFE".equals(str) || "REPLICASSAFE".equals(str))
				return WriteConcern.REPLICAS_SAFE;
			else if("SAFE".equals(str))
				return WriteConcern.SAFE;
			else if("UNACKNOWLEDGED".equals(str))
				return WriteConcern.UNACKNOWLEDGED;
		}
		return defaultValue;
	}

	public Object toCFML(Object obj) {
		if(obj instanceof List) {
			List list = (List)obj;
			Array rtn=creator.createArray();
			Iterator it = list.iterator();
			while(it.hasNext()){
				rtn.appendEL(toCFML(it.next()));
			}
			return rtn;
		}
		if(obj instanceof Map) {
			Map map = (Map)obj;
			Struct rtn=creator.createStruct();
			Iterator it = map.entrySet().iterator();
			Entry e;
			while(it.hasNext()){
				e = (Map.Entry)it.next();
				rtn.put(toCFML(e.getKey()), toCFML(e.getValue()));
			}
			return rtn;
		}
		if(obj instanceof AggregationOutput) return new AggregationOutputImpl((AggregationOutput) obj);
		if(obj instanceof CommandResult) return new CommandResultImpl((CommandResult) obj);
		if(obj instanceof DBObject) return new DBObjectImpl((DBObject) obj);
		if(obj instanceof DBCollection) return new DBCollectionImpl((DBCollection) obj);
		if(obj instanceof DBCursor) return new DBCursorImpl((DBCursor) obj);
		if(obj instanceof DB) return new DBImpl((DB) obj);
		if(obj instanceof ObjectId) return new ObjectIdImpl((ObjectId) obj);
		if(obj instanceof Set) {
			Set set=(Set) obj;
			Iterator it = set.iterator();
			Array arr=CFMLEngineFactory.getInstance().getCreationUtil().createArray();
			while(it.hasNext()){
				arr.appendEL(toCFML(it.next()));
			}
			return arr;
		}
		if(obj instanceof Number && !(obj instanceof Double)) return CFMLEngineFactory.getInstance().getCastUtil().toDouble(obj,null);
		
		//if(obj!=null)print.e("toCFML:"+obj+":"+obj.getClass().getName());
		
		return obj;
	}

	
	
	
	public Object toMongo(Object obj) {
		if(obj instanceof List || decision.isArray(obj)) {
			List list = caster.toList(obj,null);
			ArrayList rtn=new ArrayList();
			Iterator it = list.iterator();
			while(it.hasNext()){
				rtn.add(toMongo(it.next()));
			}
			return rtn;
		}
		if(obj instanceof Map || decision.isStruct(obj)) {
			return toMongo(caster.toMap(obj,null));
		}
		
		if(obj instanceof AggregationOutputImpl) return ((AggregationOutputImpl) obj).getAggregationOutput();
		if(obj instanceof CommandResultImpl) return ((CommandResultImpl) obj).getDBObject();
		if(obj instanceof DBObjectImpl) return ((DBObjectImpl) obj).getDBObject();
		if(obj instanceof DBCollectionImpl) return ((DBCollectionImpl) obj).getDBCollection();
		if(obj instanceof DBCursorImpl) return ((DBCursorImpl) obj).getDBCursor();
		if(obj instanceof DBImpl) return ((DBImpl) obj).getDB();
		if(obj instanceof ObjectIdImpl) return ((ObjectIdImpl) obj).getObjectId();
		//if(obj instanceof Struct) return toDBObject((Struct)obj);
		
		return obj;
	}
	
	public Map toMongo(Map map) {
			// single record in Map
			if(map.size()==1) {
				Entry e=(Entry) map.entrySet().iterator().next();
				return new BasicDBObject(caster.toString(e.getKey(),null), toMongo(e.getValue()));
			}
			
			// multiple records
			Map rtn=new HashMap();
			Iterator it = map.entrySet().iterator();
			Entry e;
			while(it.hasNext()){
				e = (Map.Entry)it.next();
				rtn.put(toMongo(e.getKey()), toMongo(e.getValue()));
			}
			return rtn;
	}

	public Object[] toNativeMongoArray(Object object) {
		List list = caster.toList(object,null);
		if(list!=null) {
			Object[] arr=new Object[list.size()];
			int index=0;
			Iterator it = list.iterator();
			while(it.hasNext()) {
				arr[index++]=toMongo(it.next());
			}
			return arr;
		}
		else return new Object[]{toMongo(object)};
	}
	
	public DumpData __toDumpData(Object obj, PageContext pageContext, int maxlevel, DumpProperties dp) {
		if(obj instanceof Dumpable)
			return ((Dumpable)obj).toDumpData(pageContext, maxlevel, dp);
		if(CFMLEngineFactory.getInstance().getDecisionUtil().isSimpleValue(obj))
			return new SimpleDumpData(caster.toString(obj,null));
		return new SimpleDumpData(obj.toString());
	}
	
	public static Set<Entry<String, Object>> entrySet(Collection coll) {
		Iterator<Entry<Key, Object>> it = coll.entryIterator();
		Entry<Key, Object> e;
		HashSet<Entry<String, Object>> set=new HashSet<Entry<String, Object>>();
		while(it.hasNext()){
			e= it.next();
			set.add(new CollectionMapEntry(coll,e.getKey(),e.getValue()));
		}
		return set;
	}
	
	public static java.util.Collection<?> values(Collection coll) {
		ArrayList<Object> arr = new ArrayList<Object>();
		//Key[] keys = sct.keys();
		Iterator<Object> it = coll.valueIterator();
		while(it.hasNext()) {
			arr.add(it.next());
		}
		return arr;
	}
	
	public void putAll(Collection coll, Map map) {
		Iterator it = map.entrySet().iterator();
		Map.Entry entry;
		while(it.hasNext()) {
			entry=(Entry) it.next();
			coll.setEL(caster.toKey(entry.getKey(),null), entry.getValue());
		}
	}
	
	public static class CollectionMapEntry implements Map.Entry<String,Object> {
		
		private Collection.Key key;
		private Object value;
		private Collection coll;

		public CollectionMapEntry(Collection coll,Collection.Key key,Object value) {
			this.coll=coll;
			this.key=key;
			this.value=value;
		}
		
		@Override
		public String getKey() {
			return key.getString();
		}

		public Object getValue() {
			return value;
		}

		public Object setValue(Object value) {
			Object old = value;
			coll.setEL(key, value);
			this.value=value;
			return old;
		}
		
	}
}
