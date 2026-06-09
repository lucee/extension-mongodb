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

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;

import org.bson.Document;

import com.mongodb.MongoCommandException;

public class CommandResultImpl extends DBObjectImpl {

	public CommandResultImpl(Document doc) {
		super(doc);
	}

	public Document getDocument() {
		return super.getDocument();
	}

	private boolean isOk() {
		Object ok = getDocument().get("ok");
		if (ok instanceof Number) return ((Number) ok).doubleValue() == 1.0;
		return Boolean.TRUE.equals(ok);
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		if (methodName.equals("getErrorMessage")) {
			checkArgLength("getErrorMessage", args, 0, 0);
			if (isOk()) return toCFML(null);
			return toCFML(getDocument().getString("errmsg"));
		}
		if (methodName.equals("getException")) {
			checkArgLength("getException", args, 0, 0);
			if (isOk()) return toCFML(null);
			String errmsg = getDocument().getString("errmsg");
			int code = getDocument().getInteger("code", 0);
			return toCFML(new MongoCommandException(
				new org.bson.BsonDocument("$err", new org.bson.BsonString(errmsg != null ? errmsg : "unknown")), null));
		}
		if (methodName.equals("ok")) {
			checkArgLength("ok", args, 0, 0);
			return toCFML(isOk());
		}
		if (methodName.equals("throwOnError")) {
			checkArgLength("throwOnError", args, 0, 0);
			if (!isOk()) {
				String errmsg = getDocument().getString("errmsg");
				throw exp.createApplicationException("MongoDB command failed: " + (errmsg != null ? errmsg : "unknown error"));
			}
			return null;
		}
		return super.call(pc, methodName, args);
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		if (args.isEmpty()) return call(pc, methodName, new Object[0]);
		return super.callWithNamedValues(pc, methodName, args);
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable dt = (DumpTable) super.toDumpData(pageContext, maxlevel, dp);
		dt.setTitle("CommandResult");
		return dt;
	}
}
