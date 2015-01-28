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


import org.lucee.mongodb.support.DBCursorImplSupport;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class DBCursorImpl extends DBCursorImplSupport {

	private static final long serialVersionUID = -741186782338986790L;

	private DBCursor cursor;

	public DBCursorImpl(DBCursor cursor) {
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
		// cursor.addOptions
		if(methodName.equals("addOption")) {
			checkArgLength("addOption",args,1,1);
			return toCFML(cursor.addOption(caster.toIntValue(args[0])));
		}
		// addSpecial
		if(methodName.equals("addSpecial")) {
			checkArgLength("addSpecial",args,2,2);
			return toCFML(cursor.addSpecial(caster.toString(args[0]), toMongo(args[1])));
		}
		// batchSize
		if(methodName.equals("batchSize")) {
			checkArgLength("batchSize",args,1,1);
			return toCFML(cursor.batchSize(caster.toIntValue(args[0])));
		}
		// copy
		if(methodName.equals("copy")) {
			checkArgLength("copy",args,0,0);
			return toCFML(cursor.copy());
		}
		// count
		if(methodName.equals("count")) {
			checkArgLength("count",args,0,0);
			return toCFML(cursor.count());
		}
		// curr
		if(methodName.equals("curr")) {
			checkArgLength("curr",args,0,0);
			return toCFML(cursor.curr());
		}
		// explain
		if(methodName.equals("explain")) {
			checkArgLength("explain",args,0,0);
			return toCFML(cursor.explain());
		}
		// getCollection
		if(methodName.equals("getCollection")) {
			checkArgLength("getCollection",args,0,0);
			return toCFML(cursor.getCollection());
		}
		// getCursorId
		if(methodName.equals("getCursorId")) {
			checkArgLength("getCursorId",args,0,0);
			return toCFML(cursor.getCursorId());
		}
		// getDecoderFactory
		if(methodName.equals("getDecoderFactory")) {
			checkArgLength("getDecoderFactory",args,0,0);
			return toCFML(cursor.getDecoderFactory());
		}
		// getKeysWanted
		if(methodName.equals("getKeysWanted")) {
			checkArgLength("getKeysWanted",args,0,0);
			return toCFML(cursor.getKeysWanted());
		}
		// getOptions
		if(methodName.equals("getOptions")) {
			checkArgLength("getOptions",args,0,0);
			return toCFML(cursor.getOptions());
		}
		// getQuery
		if(methodName.equals("getQuery")) {
			checkArgLength("getQuery",args,0,0);
			return toCFML(cursor.getQuery());
		}
		// getReadPreference
		if(methodName.equals("getReadPreference")) {
			checkArgLength("getReadPreference",args,0,0);
			return toCFML(cursor.getReadPreference());
		}
		// getServerAddress
		if(methodName.equals("getServerAddress")) {
			checkArgLength("getServerAddress",args,0,0);
			return toCFML(cursor.getServerAddress());
		}
		// getSizes
		if(methodName.equals("getSizes")) {
			checkArgLength("getSizes",args,0,0);
			return toCFML(cursor.getSizes());
		}
		// hasNext
		if(methodName.equals("hasNext")) {
			checkArgLength("hasNext",args,0,0);
			return toCFML(cursor.hasNext());
		}
		// itcount
		if(methodName.equals("itcount")) {
			checkArgLength("itcount",args,0,0);
			return toCFML(cursor.itcount());
		}
		// iterator
		if(methodName.equals("iterator")) {
			checkArgLength("iterator",args,0,0);
			return toCFML(cursor.iterator());
		}
		// length
		if(methodName.equals("length")) {
			checkArgLength("length",args,0,0);
			return toCFML(cursor.length());
		}
		// next
		if(methodName.equals("next")) {
			checkArgLength("next",args,0,0);
			return toCFML(cursor.next());
		}
		// numGetMores
		if(methodName.equals("numGetMores")) {
			checkArgLength("numGetMores",args,0,0);
			return toCFML(cursor.numGetMores());
		}
		// numSeen
		if(methodName.equals("numSeen")) {
			checkArgLength("numSeen",args,0,0);
			return toCFML(cursor.numSeen());
		}
		// resetOptions
		if(methodName.equals("resetOptions")) {
			checkArgLength("resetOptions",args,0,0);
			return toCFML(cursor.resetOptions());
		}
		// size
		if(methodName.equals("size")) {
			checkArgLength("size",args,0,0);
			return toCFML(cursor.size());
		}
		// snapshot
		if(methodName.equals("snapshot")) {
			checkArgLength("snapshot",args,0,0);
			return toCFML(cursor.snapshot());
		}
		// toArray
		if(methodName.equals("toArray")) {
			checkArgLength("toArray",args,0,1);
			if(args.length==0)return toCFML(cursor.toArray()); 
			return toCFML(cursor.toArray(caster.toIntValue(args[0])));
		}
		// close
		if(methodName.equals("close")) {
			checkArgLength("close",args,0,0);
			cursor.close();
			return null;
		}
		// remove
		if(methodName.equals("remove")) {
			checkArgLength("remove",args,0,0);
			cursor.remove();
			return null;
		}

		// hint
		if(methodName.equals("hint")) {
			checkArgLength("hint",args,1,1);
			DBObject dbo = toDBObject(args[0], null);
			if(dbo!=null)
				return toCFML(cursor.hint(dbo));
			return toCFML(cursor.hint(caster.toString(args[0])));
		}
		// limit
		if(methodName.equals("limit")) {
			checkArgLength("limit",args,1,1);
			return toCFML(cursor.limit(caster.toIntValue(args[0])));
		}
		// setOptions
		if(methodName.equals("setOptions")) {
			checkArgLength("setOptions",args,1,1);
			return toCFML(cursor.setOptions(caster.toIntValue(args[0])));
		}
		// skip
		if(methodName.equals("skip")) {
			checkArgLength("skip",args,1,1);
			return toCFML(cursor.skip(caster.toIntValue(args[0])));
		}
		// sort
		if(methodName.equals("sort")) {
			checkArgLength("sort",args,1,1);
			return toCFML(cursor.sort(toDBObject(args[0])));
		}
		
		String supportedFunctions=
			"addOption,addSpecial,batchSize,copy,count,curr,explain,getCollection,getCursorId,getDecoderFactory,getKeysWanted,getOptions," +
			"getQuery,getReadPreference,getServerAddress,getSizes,hasNext,itcount,iterator,length,next,numGetMores,numSeen,resetOptions," +
			"size,snapshot,toArray,close,remove,hint,limit,setOptions,skip,sort";
			
		throw exp.createExpressionException("function ["+methodName+"] is not supported, supported functions are ["+supportedFunctions+"]");
			
		
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}
	
	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		//List<DBObject> arr = cursor.toArray();
		//Iterator<DBObject> it = arr.iterator();
		DumpTable table = new DumpTable("struct","#339933","#8e714e","#000000");
		//if(arr.size()>10 && dp.getMetainfo())table.setComment("Entries:"+arr.size());
	    table.setTitle("DBCursor");
	    table.appendRow(0,
				__toDumpData("Cannot display data of Cursor", pageContext,maxlevel,dp)
		);
		/*DBObject obj;
		while(it.hasNext()) {
			obj = it.next();
				table.appendRow(0,
						__toDumpData(toCFML(obj), pageContext,maxlevel,dp)
				);
		}*/
		return table;
	}
	

	public DBCursor getDBCursor() { 
		return cursor;
	}


}
