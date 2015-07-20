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
import java.util.Map.Entry;
import java.util.Set;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Objects;
import lucee.runtime.util.Cast;

public abstract class CollObsSupport extends CastableSupport implements Collection,Objects {


	@Override
	public Object clone() {
		return duplicate(true);
	}
	


	@Override
	public final Object get(Key key) throws PageException {
		return get(key.getString());
	}

	@Override
	public final Object get(Key key, Object defaultValue) {
		return get(key.getString(), defaultValue);
	}

	@Override
	public final Object get(PageContext pc, Key key) throws PageException {
		return get(key);
	}

	@Override
	public final Object get(PageContext pc, Key key, Object defaultValue) {
		return get(key,defaultValue);
	}

	@Override
	public final Object set(Key key, Object value) throws PageException {
		return set(key.getString(), value);
	}

	@Override
	public final Object setEL(Key key, Object value) {
		return setEL(key.getString(), value);
	}

	@Override
	public final Object set(PageContext pc, Key propertyName, Object value) throws PageException {
		return set(propertyName, value);
	}

	@Override
	public final Object setEL(PageContext pc, Key propertyName, Object value) {
		return setEL(propertyName, value);
	}

	@Override
	public final boolean containsKey(Key key) {
		return containsKey(key.getString());
	}
	
	public DumpTable _toDumpTable(String title,PageContext pageContext, int maxlevel, DumpProperties dp) {
		Key[] keys = keys();
		DumpTable table = new DumpTable("struct","#339933","#8e714e","#000000");
		if(size()>10 && dp.getMetainfo())table.setComment("Entries:"+size());
	    table.setTitle(title);
		maxlevel--;
		int maxkeys=dp.getMaxKeys();
		int index=0;
		for(int i=0;i<keys.length;i++) {
			if(keyValid(dp,maxlevel,keys[i])){
				if(maxkeys<=index++)break;
				table.appendRow(1,
						__toDumpData(keys[i].toString(),pageContext,maxlevel,dp),
						__toDumpData(get(keys[i],null), pageContext,maxlevel,dp)
				);
			}
		}
		return table;
	}



	private static boolean keyValid(DumpProperties props,int level, Collection.Key key) {
		if(props.getMaxlevel()-level>1) return true;
		
		// show
		Set set = props.getShow();
		if(set!=null && !set.contains(key.getLowerString()))
			return false;
		
		// hide
		set = props.getHide();
		if(set!=null && set.contains(key.getLowerString()))
			return false;
		
		return true;
	}
	


	@Override
	public final Iterator<?> getIterator() {
		return keyIterator();
	}

	@Override
	public final Iterator<Object> valueIterator() {
		return new ValueIterator(this,keyIterator());
	}

	@Override
	public final Iterator<Entry<Key, Object>> entryIterator() {
		return new EntryIterator(this, keyIterator());
	}

	
	
	public static class KeyIterator implements Iterator<Collection.Key> {
		
		private final Iterator it;
		private final Cast caster;

		/**
		 * constructor for the class
		 * @param caster2 
		 * @param arr Base Array
		 */
		public KeyIterator(Cast caster, Iterator it) {
			this.caster=caster;
			this.it=it;
		}

		/**
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			it.remove();
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return it.hasNext();
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		public Collection.Key next() {
			return caster.toKey(it.next(),null);
		}
	}

	public static class EntryImpl implements Entry<Key, Object> {

		private final Key k;
		private Object v;
		private Collection coll;

		public EntryImpl(Collection coll,Key k,Object v){
			this.coll=coll;
			this.k=k;
			this.v=v;
		}
		
		@Override
		public Key getKey() { return k; }

		@Override
		public Object getValue() { return v; }

		@Override
		public Object setValue(Object value) {
			coll.setEL(k, value);
			Object tmp=v;
			v=value;
			return tmp;
		}
		
	}
	
	public class ValueIterator implements Iterator<Object> {
		
		private final Iterator<Key> it;
		private final Collection coll;

		/**
		 * constructor for the class
		 * @param arr Base Array
		 */
		public ValueIterator(Collection coll,Iterator<Key> it) {
			this.coll=coll;
			this.it=it;
		}

		/**
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException("this operation is not suppored");
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return it.hasNext();
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		public Object next() {
			return toCFML(coll.get(it.next(),null));
		}
	}
	
	public class EntryIterator implements Iterator<Entry<Key, Object>> {
		
		private final Iterator<Key> it;
		private final Collection coll;

		/**
		 * constructor for the class
		 * @param caster2 
		 * @param arr Base Array
		 */
		public EntryIterator(Collection coll,Iterator<Key> it) {
			this.coll=coll;
			this.it=it;
		}

		/**
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException("this operation is not suppored");
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return it.hasNext();
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		public Entry<Key, Object> next() {
			Key k = it.next();
			return new EntryImpl(coll, k, toCFML(coll.get(k,null))) ;
		}
	}
}
