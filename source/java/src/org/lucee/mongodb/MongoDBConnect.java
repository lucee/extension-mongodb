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

import com.mongodb.MongoException;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;

public class MongoDBConnect implements Function {

	private static final long serialVersionUID = 2766144594043935912L;

	public static Object call(PageContext pc, String dbName) throws PageException {
		return call(pc, dbName, null, 0);
	}
	public static Object call(PageContext pc, String dbName, String host) throws PageException {
		return call(pc, dbName, host, 0);
	}
	public static Object call(PageContext pc, String dbName, String host, double port) throws PageException {
		try {
			return DBImpl.getInstance(dbName,host, (int)port,false);
		}
		catch (MongoException e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}
}
