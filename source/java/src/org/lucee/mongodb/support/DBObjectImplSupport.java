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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.lucee.mongodb.DBCollectionImpl;
import org.lucee.mongodb.DBObjectImpl;
import org.lucee.mongodb.support.CollObsSupport.EntryImpl;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Struct;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.util.Cast;

public abstract class DBObjectImplSupport extends CollObsSupport implements Struct {


	@Override
	public final Object clone() {
		return duplicate(true);
	}

	@Override
	public final boolean containsKey(Object key) {
		try {
			return containsKey(caster.toKey(key));
		} catch (PageException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final Object get(Object key) {
		try {
			return get(caster.toKey(key),null);
		} catch (PageException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final Object remove(Object key) {
		try {
			return remove(caster.toString(key));
		} catch (PageException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object put(Object key, Object value) {
		try {
			return setEL(caster.toKey(key),value);
		} catch (PageException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final boolean containsValue(Object value) {
		throw new UnsupportedOperationException("this operation is not suppored");
	}

	@Override
	public final Set entrySet() {
		return entrySet(this);
	}

	@Override
	public final java.util.Collection values() {
		return values(this);
	}

	@Override
	public final boolean isEmpty() {
		return size()==0;
	}

	@Override
	public final void putAll(Map m) {
		putAll(this, m);
	}
	
	
	/*public static class ValueIterator implements Iterator<Object> {
		
		private final Iterator<String> it;
		private DBObjectImpl obj;

		/**
		 * constructor for the class
		 * @param arr Base Array
		 * /
		public ValueIterator(DBObjectImpl obj,Iterator<String> it) {
			this.obj=obj;
			this.it=it;
		}

		/**
		 * @see java.util.Iterator#remove()
		 * /
		public void remove() {
			throw new UnsupportedOperationException("this operation is not suppored");
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 * /
		public boolean hasNext() {
			return it.hasNext();
		}

		/**
		 * @see java.util.Iterator#next()
		 * /
		public Object next() {
			return obj.get(it.next(),null);
		}
	}
	public static class EntryIterator implements Iterator<Entry<Key, Object>> {
		
		private final Iterator<String> it;
		private final Collection coll;
		private final Cast caster;

		/**
		 * constructor for the class
		 * @param caster2 
		 * @param arr Base Array
		 * /
		public EntryIterator(Cast caster, Collection coll,Iterator<String> it) {
			this.caster=caster;
			this.coll=coll;
			this.it=it;
		}

		/**
		 * @see java.util.Iterator#remove()
		 * /
		public void remove() {
			throw new UnsupportedOperationException("this operation is not suppored");
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 * /
		public boolean hasNext() {
			return it.hasNext();
		}

		/**
		 * @see java.util.Iterator#next()
		 * /
		public Entry<Key, Object> next() {
			Key k = caster.toKey(it.next(),null);
			return new EntryImpl(coll, k, coll.get(k,null)) ;
		}
	}
	*/

	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		return _toDumpTable("DBObject", pageContext, maxlevel, dp);
	}
}
