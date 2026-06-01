/**
 *
 * Copyright (c) 2015, Lucee Association Switzerland. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/
package org.lucee.mongodb.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.Dumpable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;
import lucee.runtime.util.Creation;
import lucee.runtime.util.Decision;
import lucee.runtime.util.Excepton;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.lucee.mongodb.AggregationOutputImpl;
import org.lucee.mongodb.CommandResultImpl;
import org.lucee.mongodb.CursorImpl;
import org.lucee.mongodb.DBCollectionImpl;
import org.lucee.mongodb.DBCursorImpl;
import org.lucee.mongodb.DBImpl;
import org.lucee.mongodb.DBObjectImpl;
import org.lucee.mongodb.ObjectIdImpl;
import org.lucee.mongodb.util.SimpleDumpData;

import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;

public class ObjectSupport {
	private CFMLEngine engine;
	protected Cast caster;
	protected Excepton exp;
	protected Creation creator;
	protected Decision decision;

	public ObjectSupport() {
		engine = CFMLEngineFactory.getInstance();
		caster = engine.getCastUtil();
		exp = engine.getExceptionUtil();
		creator = engine.getCreationUtil();
		decision = engine.getDecisionUtil();
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException("this operation is not suppored");
	}

	public int checkArgLength(String functionName, Object[] arguments, int min, int max) throws PageException {
		if (arguments == null) arguments = new Object[0];
		if (min >= 0 && arguments.length < min)
			throw exp.createApplicationException("the function " + functionName + " needs at least " + min + " arguments, but you have defined only " + arguments.length);
		if (max >= 0 && arguments.length > max)
			throw exp.createApplicationException("the function " + functionName + " only support up to " + max + " arguments, but you have defined " + arguments.length);
		return arguments.length;
	}

	public List<Document> toDocumentList(Object obj) throws PageException {
		List<Document> docs = new ArrayList<Document>();
		if (decision.isArray(obj)) {
			Array arr = caster.toArray(obj);
			Iterator<Object> it = arr.valueIterator();
			while (it.hasNext()) {
				docs.add(toDocument(it.next()));
			}
		} else {
			docs.add(toDocument(obj));
		}
		return docs;
	}

	public Document[] toDBObjectArray(Object obj) throws PageException {
		if (decision.isArray(obj)) {
			Array arr = caster.toArray(obj);
			Document[] objs = new Document[arr.size()];
			Iterator<Object> it = arr.valueIterator();
			int index = 0;
			while (it.hasNext()) {
				objs[index++] = toDocument(it.next());
			}
			return objs;
		}
		return new Document[]{toDocument(obj)};
	}

	public Document toDocument(Object obj) throws PageException {
		if (obj instanceof Document) return (Document) obj;
		if (obj instanceof DBObjectImpl) return ((DBObjectImpl) obj).getDocument();
		// Handle Lucee Struct directly to preserve ordered-struct key order.
		// caster.toMap() may materialise a new HashMap, discarding the insertion
		// order of linked structs (["key":val, ...] syntax).
		if (decision.isStruct(obj)) {
			Struct struct = caster.toStruct(obj, null);
			if (struct != null) return structToDocument(struct);
		}
		return toDocument(caster.toMap(obj), null);
	}

	public Document toDocument(Object obj, Document defaultValue) {
		if (obj instanceof Document) return (Document) obj;
		if (obj instanceof DBObjectImpl) return ((DBObjectImpl) obj).getDocument();
		Object mo = toMongo(obj);
		if (mo instanceof Document) return (Document) mo;
		if (mo instanceof Map) return new Document((Map<String, Object>) mo);
		return defaultValue;
	}

	public Document toDBObject(Object obj) throws PageException {
		return toDocument(obj);
	}

	public Document toDBObject(Object obj, Document defaultValue) {
		return toDocument(obj, defaultValue);
	}

	public WriteConcern toWriteConcern(Object obj, WriteConcern defaultValue) {
		if (obj instanceof WriteConcern) return (WriteConcern) obj;
		if (decision.isSimpleValue(obj)) {
			String str = caster.toString(obj, "").trim().toUpperCase();
			if ("ACKNOWLEDGED".equals(str)) return WriteConcern.ACKNOWLEDGED;
			if ("UNACKNOWLEDGED".equals(str)) return WriteConcern.UNACKNOWLEDGED;
			if ("MAJORITY".equals(str)) return WriteConcern.MAJORITY;
			if ("W1".equals(str) || "SAFE".equals(str) || "NORMAL".equals(str)) return WriteConcern.W1;
			if ("W2".equals(str) || "REPLICA_ACKNOWLEDGED".equals(str) || "REPLICAACKNOWLEDGED".equals(str)) return WriteConcern.W2;
			if ("W3".equals(str)) return WriteConcern.W3;
			if ("JOURNALED".equals(str) || "JOURNAL_SAFE".equals(str) || "JOURNALSAFE".equals(str)) return WriteConcern.JOURNALED;
		}
		return defaultValue;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object toCFML(Object obj) {
		if (obj instanceof List) {
			List list = (List) obj;
			Array rtn = creator.createArray();
			for (Object item : list) {
				rtn.appendEL(toCFML(item));
			}
			return rtn;
		}
		if (obj instanceof Document) return documentToStruct((Document) obj);
		if (obj instanceof Map) {
			Map map = (Map) obj;
			Struct rtn = creator.createStruct();
			for (Object raw : map.entrySet()) {
				Entry e = (Entry) raw;
				rtn.put(toCFML(e.getKey()), toCFML(e.getValue()));
			}
			return rtn;
		}
		if (obj instanceof AggregateIterable) return new AggregationOutputImpl((AggregateIterable<Document>) obj);
		if (obj instanceof MongoCollection) return new DBCollectionImpl((MongoCollection<Document>) obj, null);
		if (obj instanceof FindIterable) return new DBCursorImpl((FindIterable<Document>) obj);
		if (obj instanceof MongoCursor) return new CursorImpl((MongoCursor<Document>) obj);
		if (obj instanceof MongoDatabase) return new DBImpl((MongoDatabase) obj, null);
		if (obj instanceof ObjectId) return new ObjectIdImpl((ObjectId) obj);
		if (obj instanceof Set) {
			Set set = (Set) obj;
			Array arr = CFMLEngineFactory.getInstance().getCreationUtil().createArray();
			for (Object item : set) {
				arr.appendEL(toCFML(item));
			}
			return arr;
		}
		if (obj instanceof Number && !(obj instanceof Double))
			return CFMLEngineFactory.getInstance().getCastUtil().toDouble(obj, null);
		return obj;
	}

	/**
	 * Convert a Lucee Struct to a MongoDB Document, preserving the key insertion
	 * order of ordered structs (CFML's ["key":val, ...] syntax).
	 *
	 * Uses {@link Collection#entryIterator()} rather than {@link Cast#toMap} to
	 * avoid any re-hashing that could discard the LinkedHashMap ordering.
	 * This matters for compound index specs and multi-key sort documents where
	 * MongoDB treats field order as semantically significant.
	 */
	@SuppressWarnings({"unchecked"})
	private Document structToDocument(Struct struct) {
		Document doc = new Document();
		Iterator<Entry<Key, Object>> it = struct.entryIterator();
		while (it.hasNext()) {
			Entry<Key, Object> entry = it.next();
			doc.put(entry.getKey().getString(), toMongo(entry.getValue()));
		}
		return doc;
	}

	/**
	 * Convert a MongoDB Document to a native Lucee Struct.
	 * Native Structs are case-insensitive, matching CFML's behaviour.
	 * Values are converted recursively via toCFML() so nested Documents
	 * become Structs and Lists become Lucee Arrays.
	 */
	private Struct documentToStruct(Document doc) {
		Struct struct = creator.createStruct();
		for (Map.Entry<String, Object> entry : doc.entrySet()) {
			struct.setEL(caster.toKey(entry.getKey(), null), toCFML(entry.getValue()));
		}
		return struct;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object toMongo(Object obj) {
		if (obj instanceof List || decision.isArray(obj)) {
			List list = caster.toList(obj, null);
			ArrayList rtn = new ArrayList();
			for (Object item : list) {
				rtn.add(toMongo(item));
			}
			return rtn;
		}
		if (obj instanceof Date) {
			return new Date(((Date) obj).getTime());
		}
		if (decision.isStruct(obj)) {
			Struct struct = caster.toStruct(obj, null);
			if (struct != null) return structToDocument(struct);
		}
		if (obj instanceof Map) {
			return toMongoDocument((Map) obj);
		}
		if (obj instanceof AggregationOutputImpl) return ((AggregationOutputImpl) obj).getIterable();
		if (obj instanceof CommandResultImpl) return ((CommandResultImpl) obj).getDocument();
		if (obj instanceof CursorImpl) return ((CursorImpl) obj).getCursor();
		if (obj instanceof DBObjectImpl) return ((DBObjectImpl) obj).getDocument();
		if (obj instanceof DBCollectionImpl) return ((DBCollectionImpl) obj).getCollection();
		if (obj instanceof DBCursorImpl) return ((DBCursorImpl) obj).getIterable();
		if (obj instanceof DBImpl) return ((DBImpl) obj).getDatabase();
		if (obj instanceof ObjectIdImpl) return ((ObjectIdImpl) obj).getObjectId();
		return obj;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public Document toMongoDocument(Map map) {
		Document doc = new Document();
		for (Object raw : map.entrySet()) {
			Entry e = (Entry) raw;
			doc.put(caster.toString(e.getKey(), null), toMongo(e.getValue()));
		}
		return doc;
	}

	public Object[] toNativeMongoArray(Object object) {
		List list = caster.toList(object, null);
		if (list != null) {
			Object[] arr = new Object[list.size()];
			int index = 0;
			for (Object item : list) {
				arr[index++] = toMongo(item);
			}
			return arr;
		}
		return new Object[]{toMongo(object)};
	}

	public DumpData __toDumpData(Object obj, PageContext pageContext, int maxlevel, DumpProperties dp) {
		if (obj instanceof Dumpable)
			return ((Dumpable) obj).toDumpData(pageContext, maxlevel, dp);
		if (CFMLEngineFactory.getInstance().getDecisionUtil().isSimpleValue(obj))
			return new SimpleDumpData(caster.toString(obj, null));
		return new SimpleDumpData("");
	}

	public static Set<Entry<String, Object>> entrySet(Collection coll) {
		Iterator<Entry<Key, Object>> it = coll.entryIterator();
		HashSet<Entry<String, Object>> set = new HashSet<Entry<String, Object>>();
		while (it.hasNext()) {
			Entry<Key, Object> e = it.next();
			set.add(new CollectionMapEntry(coll, e.getKey(), e.getValue()));
		}
		return set;
	}

	public static java.util.Collection<?> values(Collection coll) {
		ArrayList<Object> arr = new ArrayList<Object>();
		Iterator<Object> it = coll.valueIterator();
		while (it.hasNext()) {
			arr.add(it.next());
		}
		return arr;
	}

	public void putAll(Collection coll, Map map) {
		for (Object raw : map.entrySet()) {
			Map.Entry entry = (Map.Entry) raw;
			coll.setEL(caster.toKey(entry.getKey(), null), entry.getValue());
		}
	}

	public static class CollectionMapEntry implements Map.Entry<String, Object> {
		private Collection.Key key;
		private Object value;
		private Collection coll;

		public CollectionMapEntry(Collection coll, Collection.Key key, Object value) {
			this.coll = coll;
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() { return key.getString(); }

		@Override
		public Object getValue() { return value; }

		@Override
		public Object setValue(Object value) {
			Object old = this.value;
			coll.setEL(key, value);
			this.value = value;
			return old;
		}
	}
}
