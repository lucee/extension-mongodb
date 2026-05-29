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
		if (!doc.containsKey(key.getString()))
			throw exp.createApplicationException("There is no key [" + key + "] in the Document");
		return doc.remove(key.getString());
	}

	@Override
	public Object removeEL(Key key) {
		return doc.remove(key.getString());
	}

	public Object remove(Key key, Object defaultValue) {
		Object rtn = doc.remove(key.getString());
		return rtn == null ? defaultValue : rtn;
	}

	@Override
	public void clear() {
		doc.clear();
	}

	@Override
	public Object get(String key) throws PageException {
		if (doc.containsKey(key)) return toCFML(doc.get(key));
		throw exp.createApplicationException("There is no key [" + key + "] in the Document");
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
	public final Collection duplicate(boolean deepCopy) {
		return new DBObjectImpl(new Document(doc));
	}

	@Override
	public boolean containsKey(String key) {
		return doc.containsKey(key);
	}

	@Override
	public Iterator<String> keysAsStringIterator() {
		return doc.keySet().iterator();
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		if (methodName.equals("containsField") || methodName.equals("containsKey")) {
			checkArgLength("containsField", args, 1, 1);
			return toCFML(doc.containsKey(caster.toString(args[0])));
		}
		if (methodName.equals("get")) {
			checkArgLength("get", args, 1, 1);
			return toCFML(doc.get(caster.toString(args[0])));
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
			return toCFML(doc.remove(caster.toString(args[0])));
		}
		if (methodName.equals("toMap")) {
			checkArgLength("toMap", args, 0, 0);
			return toCFML(doc);
		}
		String functionNames = "containsField,get,isPartialObject,markAsPartialObject,removeField,toMap";
		throw exp.createApplicationException("function " + methodName + " does not exist, existing functions are [" + functionNames + "]");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		throw new UnsupportedOperationException("named arguments are not supported yet!");
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
