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

import java.util.Date;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;

import org.bson.types.ObjectId;

public class MongoDBId implements Function {

	private static final long serialVersionUID = 2766144594043935912L;

	public static Object call(PageContext pc) throws PageException {
		return call(pc, null);
	}

	public static Object call(PageContext pc, Object initArg) throws PageException {
		if (initArg != null && !(initArg instanceof Date) && !(initArg instanceof String)) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil().createApplicationException(
				"First argument to MongoDbId is invalid type. Acceptable types are datetime and string");
		}
		ObjectId newID = ObjectId.get();
		if (initArg instanceof Date) {
			// ObjectId(Date) was removed in driver 5.x; reconstruct from timestamp
			Date date = CFMLEngineFactory.getInstance().getCastUtil().toDate(initArg, pc.getTimeZone());
			int timestamp = (int) (date.getTime() / 1000L);
			byte[] bytes = ObjectId.get().toByteArray();
			bytes[0] = (byte) (timestamp >> 24);
			bytes[1] = (byte) (timestamp >> 16);
			bytes[2] = (byte) (timestamp >> 8);
			bytes[3] = (byte) timestamp;
			newID = new ObjectId(bytes);
		} else if (initArg instanceof String) {
			String initArgString = CFMLEngineFactory.getInstance().getCastUtil().toString(initArg);
			if (!ObjectId.isValid(initArgString)) {
				throw CFMLEngineFactory.getInstance().getExceptionUtil().createApplicationException(
					"String [" + initArgString + "] passed as MongoDbId argument is not a valid ObjectID string");
			}
			newID = new ObjectId(initArgString);
		}
		return new ObjectIdImpl(newID);
	}
}
