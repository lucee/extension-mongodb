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

import lucee.runtime.exp.PageException;
import lucee.runtime.op.Castable;
import lucee.runtime.type.dt.DateTime;

public class CastableSupport extends ObjectSupport implements Castable {


	@Override
	public String castToString(String defaultValue) {
		 return defaultValue;
	}

	@Override
	public final boolean castToBooleanValue() throws PageException {
        throw exp.createExpressionException("can't cast Complex Object Type to a boolean value");
	}

	@Override
	public final Boolean castToBoolean(Boolean defaultValue) {
        return defaultValue;
	}

	@Override
	public final double castToDoubleValue() throws PageException {
        throw exp.createExpressionException("can't cast Complex Object Type to a number value");
	}

	@Override
	public final double castToDoubleValue(double defaultValue) {
       return defaultValue;
	}

	@Override
	public final DateTime castToDateTime() throws PageException {
        throw exp.createExpressionException("can't cast Complex Object Type to a Date");
	}

	@Override
	public final DateTime castToDateTime(DateTime defaultValue) {
		return defaultValue;
	}

	@Override
	public String castToString() throws PageException {
		 throw exp.createExpressionException("Can't cast Complex Object to a String");
	}
	
	
    @Override
	public final int compareTo(boolean b) throws PageException {
		throw exp.createExpressionException("can't compare Complex Object Type with a boolean value");
	}

	@Override
	public final int compareTo(DateTime dt) throws PageException {
		throw exp.createExpressionException("can't compare Complex Object Type with a DateTime Object");
	}

	@Override
	public final int compareTo(double d) throws PageException {
		throw exp.createExpressionException("can't compare Complex Object Type with a numeric value");
	}

	@Override
	public final int compareTo(String str) throws PageException {
		throw exp.createExpressionException("can't compare Complex Object Type Struct with a String");
	}

}
