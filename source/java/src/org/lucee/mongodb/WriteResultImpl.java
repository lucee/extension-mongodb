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
package org.lucee.mongodb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Objects;
import lucee.runtime.type.Struct;

import org.bson.Document;
import org.lucee.mongodb.support.DBObjectImplSupport;

public class WriteResultImpl extends DBObjectImplSupport {

	private Document doc;

	public WriteResultImpl(Document doc) {
		this.doc = doc;
	}

	// WriteResult Java API — accessed via Lucee's reflection-based method dispatch
	public long getN() {
		Object v = doc.get("n");
		return v instanceof Number ? ((Number) v).longValue() : 0L;
	}

	public boolean isOk() {
		return Boolean.TRUE.equals(doc.get("acknowledged"));
	}

	public boolean isUpdateOfExisting() {
		return Boolean.TRUE.equals(doc.get("updatedExisting"));
	}

	public String getError() { return null; }

	@Override
	public int size() { return doc.size(); }

	@Override
	public Key[] keys() {
		List<Key> list = new ArrayList<Key>();
		for (String k : doc.keySet()) list.add(caster.toKey(k, null));
		return list.toArray(new Key[list.size()]);
	}

	@Override
	public Iterator<Key> keyIterator() {
		return new KeyIterator(caster, doc.keySet().iterator());
	}

	@Override
	public Object remove(Key key) throws PageException {
		if (!doc.containsKey(key.getString()))
			throw exp.createApplicationException("There is no key [" + key + "] in the WriteResult");
		return doc.remove(key.getString());
	}

	@Override
	public Object removeEL(Key key) { return doc.remove(key.getString()); }

	public Object remove(Key key, Object defaultValue) {
		Object rtn = doc.remove(key.getString());
		return rtn == null ? defaultValue : rtn;
	}

	@Override
	public void clear() { doc.clear(); }

	@Override
	public Object get(String key) throws PageException {
		if (doc.containsKey(key)) return toCFML(doc.get(key));
		throw exp.createApplicationException("There is no key [" + key + "] in the WriteResult");
	}

	@Override
	public Object get(String key, Object defaultValue) {
		if (doc.containsKey(key)) return toCFML(doc.get(key));
		return defaultValue;
	}

	@Override
	public Object set(String key, Object value) throws PageException {
		doc.put(key, toMongo(value));
		return value;
	}

	@Override
	public Object setEL(String key, Object value) {
		doc.put(key, toMongo(value));
		return value;
	}

	@Override
	public Collection duplicate(boolean deepCopy) {
		return new WriteResultImpl(new Document(doc));
	}

	@Override
	public boolean containsKey(String key) { return doc.containsKey(key); }

	@Override
	public Iterator<String> keysAsStringIterator() { return doc.keySet().iterator(); }

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		// WriteResult API — must be handled here because implementing Objects causes
		// Lucee to route all method calls through call() rather than Java reflection
		String name = methodName.getLowerString();
		if (name.equals("getn")) {
			checkArgLength("getN", args, 0, 0);
			return getN();
		}
		if (name.equals("isok")) {
			checkArgLength("isOk", args, 0, 0);
			return isOk();
		}
		if (name.equals("isupdateofexisting")) {
			checkArgLength("isUpdateOfExisting", args, 0, 0);
			return isUpdateOfExisting();
		}
		if (name.equals("geterror")) {
			checkArgLength("getError", args, 0, 0);
			return getError();
		}
		// Delegate standard CFML struct methods to a native Lucee Struct
		Struct nativeStruct = toNativeStruct();
		if (nativeStruct instanceof Objects) {
			return ((Objects) nativeStruct).call(pc, methodName, args);
		}
		throw exp.createApplicationException("function " + methodName + " does not exist on WriteResultImpl");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		Struct nativeStruct = toNativeStruct();
		if (nativeStruct instanceof Objects) {
			return ((Objects) nativeStruct).callWithNamedValues(pc, methodName, args);
		}
		throw new UnsupportedOperationException("named arguments not supported");
	}

	@Override
	public Set keySet() { return doc.keySet(); }

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		return _toDumpTable("WriteResult", pageContext, maxlevel, dp);
	}

	public long sizeOf() { return 0; }

	private Struct toNativeStruct() {
		Struct struct = creator.createStruct();
		for (String key : doc.keySet()) {
			struct.setEL(caster.toKey(key, null), toCFML(doc.get(key)));
		}
		return struct;
	}
}
