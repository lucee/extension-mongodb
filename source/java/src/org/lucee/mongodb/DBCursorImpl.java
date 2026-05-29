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
import java.util.List;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;

import org.bson.Document;
import org.lucee.mongodb.support.DBCursorImplSupport;

import com.mongodb.ServerCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

public class DBCursorImpl extends DBCursorImplSupport {

	private static final long serialVersionUID = -741186782338986790L;

	private FindIterable<Document> iterable;
	private MongoCursor<Document> cursor;

	public DBCursorImpl(FindIterable<Document> iterable) {
		this.iterable = iterable;
	}

	private MongoCursor<Document> cursor() {
		if (cursor == null) cursor = iterable.iterator();
		return cursor;
	}

	@Override
	public boolean hasNext() {
		return cursor().hasNext();
	}

	@Override
	public Object next() {
		return toCFML(cursor().next());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove is not supported on FindIterable");
	}

	@Override
	public Object get(PageContext pc, Key key, Object defaultValue) {
		return defaultValue;
	}

	@Override
	public Object get(PageContext pc, Key key) throws PageException {
		throw exp.createApplicationException("the cursor has no property with the name " + key);
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
		if (methodName.equals("batchSize")) {
			checkArgLength("batchSize", args, 1, 1);
			iterable.batchSize(caster.toIntValue(args[0]));
			return toCFML(this);
		}
		if (methodName.equals("limit")) {
			checkArgLength("limit", args, 1, 1);
			iterable.limit(caster.toIntValue(args[0]));
			return toCFML(this);
		}
		if (methodName.equals("skip")) {
			checkArgLength("skip", args, 1, 1);
			iterable.skip(caster.toIntValue(args[0]));
			return toCFML(this);
		}
		if (methodName.equals("sort")) {
			checkArgLength("sort", args, 1, 1);
			iterable.sort(toDocument(args[0]));
			return toCFML(this);
		}
		if (methodName.equals("hint")) {
			checkArgLength("hint", args, 1, 1);
			Document dbo = toDocument(args[0], null);
			if (dbo != null) iterable.hint(dbo);
			else iterable.hintString(caster.toString(args[0]));
			return toCFML(this);
		}
		if (methodName.equals("hasNext")) {
			checkArgLength("hasNext", args, 0, 0);
			return toCFML(cursor().hasNext());
		}
		if (methodName.equals("next")) {
			checkArgLength("next", args, 0, 0);
			return toCFML(cursor().next());
		}
		if (methodName.equals("getCursorId")) {
			checkArgLength("getCursorId", args, 0, 0);
			ServerCursor sc = cursor().getServerCursor();
			return toCFML(sc != null ? sc.getId() : 0L);
		}
		if (methodName.equals("getServerAddress")) {
			checkArgLength("getServerAddress", args, 0, 0);
			return toCFML(cursor().getServerAddress() != null ? cursor().getServerAddress().toString() : null);
		}
		if (methodName.equals("toArray")) {
			checkArgLength("toArray", args, 0, 1);
			List<Object> result = new ArrayList<Object>();
			for (Document doc : iterable) {
				result.add(toCFML(doc));
				if (args.length == 1 && result.size() >= caster.toIntValue(args[0])) break;
			}
			return toCFML(result);
		}
		if (methodName.equals("close")) {
			checkArgLength("close", args, 0, 0);
			if (cursor != null) { cursor.close(); cursor = null; }
			return null;
		}
		if (methodName.equals("iterator")) {
			checkArgLength("iterator", args, 0, 0);
			return toCFML(cursor());
		}
		if (methodName.equals("getBatchSize")) {
			checkArgLength("getBatchSize", args, 0, 0);
			return toCFML(0);
		}
		// count/size: iterate to count since FindIterable has no count() in 5.x
		if (methodName.equals("count") || methodName.equals("size") || methodName.equals("length") || methodName.equals("itcount")) {
			checkArgLength(methodName.getString(), args, 0, 0);
			int n = 0;
			for (Document ignored : iterable) n++;
			return toCFML(n);
		}
		if (methodName.equals("explain")) {
			checkArgLength("explain", args, 0, 0);
			return toCFML(iterable.explain());
		}
		// Methods that were removed in 5.x
		if (methodName.equals("addOption") || methodName.equals("addSpecial") || methodName.equals("copy") ||
			methodName.equals("curr") || methodName.equals("getCollection") || methodName.equals("getDecoderFactory") ||
			methodName.equals("getKeysWanted") || methodName.equals("getOptions") || methodName.equals("getQuery") ||
			methodName.equals("getReadPreference") || methodName.equals("numSeen") || methodName.equals("resetOptions") ||
			methodName.equals("snapshot") || methodName.equals("setOptions")) {
			throw exp.createApplicationException("cursor." + methodName + "() was removed in MongoDB Java driver 5.x");
		}

		String supportedFunctions = "batchSize,limit,skip,sort,hint,hasNext,next,getCursorId,getServerAddress,toArray,close,iterator,getBatchSize,count,size,explain";
		throw exp.createExpressionException("function [" + methodName + "] is not supported, supported functions are [" + supportedFunctions + "]");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable table = new DumpTable("struct", "#339933", "#8e714e", "#000000");
		table.setTitle("DBCursor");
		table.appendRow(0, __toDumpData("Cannot display data of Cursor", pageContext, maxlevel, dp));
		return table;
	}

	public FindIterable<Document> getIterable() {
		return iterable;
	}
}
