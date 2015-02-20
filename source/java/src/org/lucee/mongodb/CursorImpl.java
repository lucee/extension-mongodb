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


import org.lucee.mongodb.support.CursorImplSupport;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;

import com.mongodb.Cursor;
import com.mongodb.DBObject;

public class CursorImpl extends CursorImplSupport {

	private static final long serialVersionUID = 20150220001L;

	private Cursor cursor;

	public CursorImpl(Cursor cursor) {
		this.cursor=cursor;
	}

	@Override
	public boolean hasNext() {
		return cursor.hasNext();
	}

	@Override
	public Object next() {
		return toCFML(cursor.next());
	}

	@Override
	public void remove() {
		cursor.remove();
	}

	@Override
	public Object get(PageContext pc, Key key, Object defaultValue) {
		return defaultValue;
	}

	@Override
	public Object get(PageContext pc, Key key) throws PageException {
		throw exp.createApplicationException("the cursor has no property with the name "+key);
	}

	@Override
	public Object set(PageContext pc, Key propertyName, Object value) throws PageException {
		throw exp.createApplicationException("not possible to set a property to the cursor");
	}

	@Override
	public Object setEL(PageContext pc, Key propertyName, Object value) {
		return value;
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		// getCursorId
		if(methodName.equals("getCursorId")) {
			checkArgLength("getCursorId",args,0,0);
			return toCFML(cursor.getCursorId());
		}
		// getServerAddress
		if(methodName.equals("getServerAddress")) {
			checkArgLength("getServerAddress",args,0,0);
			return toCFML(cursor.getServerAddress());
		}
		// hasNext
		if(methodName.equals("hasNext")) {
			checkArgLength("hasNext",args,0,0);
			return toCFML(cursor.hasNext());
		}
		// next
		if(methodName.equals("next")) {
			checkArgLength("next",args,0,0);
			return toCFML(cursor.next());
		}
		// remove
		if(methodName.equals("remove")) {
			checkArgLength("remove",args,0,0);
			cursor.remove();
			return null;
		}
		
		String supportedFunctions=
			"getCursorId,getServerAddress,hasNext,next,remove";
			
		throw exp.createExpressionException("function ["+methodName+"] is not supported, supported functions are ["+supportedFunctions+"]");
	
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}
	
	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable table = new DumpTable("struct","#339933","#8e714e","#000000");
	    table.setTitle("Cursor");
	    table.appendRow(0,
				__toDumpData("Cannot display data of Cursor", pageContext,maxlevel,dp)
		);
		return table;
	}

	public Cursor getCursor() { 
		return cursor;
	}

}
