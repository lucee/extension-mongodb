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
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Objects;
import lucee.runtime.type.Struct;

import org.bson.Document;
import org.lucee.mongodb.support.DBObjectImplSupport;

public class DBObjectImpl extends DBObjectImplSupport {

	private Document doc;

	public DBObjectImpl(Document doc) {
		if (doc == null) throw new RuntimeException("Document cannot be null");
		this.doc = doc;
	}

	@Override
	public int size() {
		return doc.size();
	}

	@Override
	public Key[] keys() {
		List<Key> list = new ArrayList<Key>();
		for (String k : doc.keySet()) {
			list.add(caster.toKey(k, null));
		}
		return list.toArray(new Key[list.size()]);
	}

	@Override
	public Iterator<Key> keyIterator() {
		return new KeyIterator(caster, doc.keySet().iterator());
	}

	@Override
	public Object remove(Key key) throws PageException {
		String resolved = resolveKey(key.getString());
		if (resolved == null)
			throw exp.createApplicationException("There is no key [" + key + "] in the Document");
		return doc.remove(resolved);
	}

	@Override
	public Object removeEL(Key key) {
		String resolved = resolveKey(key.getString());
		if (resolved == null) return null;
		return doc.remove(resolved);
	}

	public Object remove(Key key, Object defaultValue) {
		String resolved = resolveKey(key.getString());
		if (resolved == null) return defaultValue;
		Object rtn = doc.remove(resolved);
		return rtn == null ? defaultValue : rtn;
	}

	@Override
	public void clear() {
		doc.clear();
	}

	@Override
	public Object get(String key) throws PageException {
		String resolved = resolveKey(key);
		if (resolved != null) return toCFML(doc.get(resolved));
		throw exp.createApplicationException("There is no key [" + key + "] in the Document");
	}

	@Override
	public Object get(String key, Object defaultValue) {
		String resolved = resolveKey(key);
		if (resolved != null) return toCFML(doc.get(resolved));
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
	public final Collection duplicate(boolean deepCopy) {
		return new DBObjectImpl(new Document(doc));
	}

	@Override
	public boolean containsKey(String key) {
		return resolveKey(key) != null;
	}

	private String resolveKey(String key) {
		if (doc.containsKey(key)) return key;
		for (String k : doc.keySet()) {
			if (k.equalsIgnoreCase(key)) return k;
		}
		return null;
	}

	@Override
	public Iterator<String> keysAsStringIterator() {
		return doc.keySet().iterator();
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		if (methodName.equals("containsField") || methodName.equals("containsKey")) {
			checkArgLength("containsField", args, 1, 1);
			return toCFML(resolveKey(caster.toString(args[0])) != null);
		}
		if (methodName.equals("get")) {
			int len = checkArgLength("get", args, 1, 2);
			String key = caster.toString(args[0]);
			String resolved = resolveKey(key);
			if (len == 2) {
				return resolved != null ? toCFML(doc.get(resolved)) : args[1];
			}
			if (resolved == null)
				throw exp.createApplicationException("There is no key [" + key + "] in the Document");
			return toCFML(doc.get(resolved));
		}
		if (methodName.equals("isPartialObject")) {
			checkArgLength("isPartialObject", args, 0, 0);
			return toCFML(false);
		}
		if (methodName.equals("markAsPartialObject")) {
			checkArgLength("markAsPartialObject", args, 0, 0);
			return null;
		}
		if (methodName.equals("removeField") || methodName.equals("remove")) {
			checkArgLength("removeField", args, 1, 1);
			String resolved = resolveKey(caster.toString(args[0]));
			return toCFML(resolved != null ? doc.remove(resolved) : null);
		}
		if (methodName.equals("toMap")) {
			checkArgLength("toMap", args, 0, 0);
			return toCFML(doc);
		}
		// Delegate unrecognized methods to a native Lucee Struct so that standard
		// CFML struct functions (each, keyArray, find, etc.) work on document results
		Struct nativeStruct = toNativeStruct();
		if (nativeStruct instanceof Objects) {
			return ((Objects) nativeStruct).call(pc, methodName, args);
		}
		throw exp.createApplicationException("function " + methodName + " does not exist on DBObjectImpl");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		Struct nativeStruct = toNativeStruct();
		if (nativeStruct instanceof Objects) {
			return ((Objects) nativeStruct).callWithNamedValues(pc, methodName, args);
		}
		throw new UnsupportedOperationException("named arguments are not supported");
	}

	private Struct toNativeStruct() {
		Struct struct = creator.createStruct();
		for (String key : doc.keySet()) {
			struct.setEL(caster.toKey(key, null), toCFML(doc.get(key)));
		}
		return struct;
	}

	@Override
	public Set keySet() {
		return doc.keySet();
	}

	public Document getDocument() {
		return doc;
	}

	public long sizeOf() {
		return 0;
	}
}
