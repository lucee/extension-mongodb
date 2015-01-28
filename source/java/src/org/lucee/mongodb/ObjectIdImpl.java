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
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.lucee.mongodb.support.CollObsSupport;
import org.lucee.mongodb.support.ObjectSupport;
import org.lucee.mongodb.support.CollObsSupport.EntryImpl;

import com.mongodb.DB;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Castable;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.util.Cast;

public class ObjectIdImpl extends CollObsSupport implements Castable {
	
	private static final Object NULL = new Object();
	private ObjectId id;
	private static List<Key> keys;

	public ObjectIdImpl(ObjectId id){
		this.id=id;
		if(keys==null){
			keys=new ArrayList<Collection.Key>();
			keys.add(creator.createKey("inc"));
			keys.add(creator.createKey("machine"));
			keys.add(creator.createKey("time"));
			keys.add(creator.createKey("timeSecond"));
			keys.add(creator.createKey("id"));
		}
	}

	@Override
	public String castToString() throws PageException {
		return id.toString();
	}

	@Override
	public String castToString(String defaultValue) {
		return id.toString();
	}

	@Override
	public int size() {
		return keys.size();
	}

	@Override
	public Key[] keys() {
		return keys.toArray(new Key[keys.size()]);
	}

	@Override
	public Object remove(Key key) throws PageException {
		throw exp.createApplicationException("cannot remove keys from ObjectId");
	}

	@Override
	public Object removeEL(Key key) {
		return null;
	}

	// TODO @Override
	public Object remove(Key key, Object defaultValue) {
		return defaultValue;
	}

	@Override
	public void clear() {
		throw new RuntimeException(exp.createApplicationException("cannot clear ObjectId"));
		
	}

	@Override
	public Object get(String key) throws PageException {
		Object value = get(key,NULL);
		if(value!=NULL) return value;
		throw exp.createApplicationException("key "+key+" does not exists, supported keys are [inc, machine, time, timeSecond, id]");
	}

	@Override
	public Object get(String key, Object defaultValue) {
		if("inc".equalsIgnoreCase(key)) return toCFML(id.getInc());
		else if("machine".equalsIgnoreCase(key)) return toCFML(id.getMachine());
		else if("time".equalsIgnoreCase(key)) return toCFML(id.getTime());
		else if("timeSecond".equalsIgnoreCase(key)) return toCFML(id.getTimeSecond());
		else if("id".equalsIgnoreCase(key)) return toCFML(id.toString());
		return defaultValue;
	}

	@Override
	public Object set(String key, Object value) throws PageException {
		throw exp.createApplicationException("cannot set a key to ObjectId");
	}

	@Override
	public Object setEL(String key, Object value) {
		throw new RuntimeException(exp.createApplicationException("cannot set a key to ObjectId"));
	}

	@Override
	public Collection duplicate(boolean deepCopy) {
		return new ObjectIdImpl(new ObjectId(id.toByteArray()));
	}

	@Override
	public boolean containsKey(String key) {
		return get(key,NULL)!=NULL;
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		return _toDumpTable("ObjectId", pageContext, maxlevel, dp);
	}

	@Override
	public Iterator<Key> keyIterator() {
		return keys.iterator();
	}

	@Override
	public Iterator<String> keysAsStringIterator() {
		Iterator<Key> it = keys.iterator();
		List<String> rtn=new ArrayList<String>();
		while(it.hasNext()){
			rtn.add(it.next().getString());
		}
		return rtn.iterator();
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		// getInc
		if(methodName.equals("getInc")) {
			checkArgLength("getInc",args,0,0);
			return toCFML(id.getInc());
		}
		// getMachine
		else if(methodName.equals("getMachine")) {
			checkArgLength("getMachine",args,0,0);
			return toCFML(id.getMachine());
		}
		// getTime
		else if(methodName.equals("getTime")) {
			checkArgLength("getTime",args,0,0);
			return toCFML(id.getTime());
		}
		// getTimeSecond
		else if(methodName.equals("getTimeSecond")) {
			checkArgLength("getTimeSecond",args,0,0);
			return toCFML(id.getTimeSecond());
		}
		// getClass
		else if(methodName.equals("getClass")) {
			checkArgLength("getClass",args,0,0);
			return toCFML(id.getClass());
		}
		// toString
		else if(methodName.equals("toString")) {
			checkArgLength("toString",args,0,0);
			return toCFML(id.toString());
		}
		
		throw new UnsupportedOperationException("function "+methodName+" not supported");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}

	public ObjectId getObjectId() {
		return id;
	}
	
	
}
