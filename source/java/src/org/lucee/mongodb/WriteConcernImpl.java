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
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Objects;
import lucee.runtime.type.Struct;

import com.mongodb.WriteConcern;

import org.lucee.mongodb.support.CastableSupport;

/**
 * CFML-callable wrapper around a MongoDB {@link WriteConcern}.
 *
 * Because this class implements {@link Objects}, Lucee routes all CFML method
 * calls through {@link #call} rather than Java reflection. Every method that
 * CFML code may invoke must therefore be handled explicitly here.
 */
public class WriteConcernImpl extends CastableSupport implements Objects {

	private WriteConcern wc;

	public WriteConcernImpl(WriteConcern wc) {
		this.wc = wc;
	}

	public WriteConcern getWriteConcern() {
		return wc;
	}

	// --- Objects property stubs (WriteConcern has no CFML-visible properties) ---

	@Override
	public Object get(PageContext pc, Key key, Object defaultValue) {
		return defaultValue;
	}

	@Override
	public Object get(PageContext pc, Key key) throws PageException {
		throw exp.createApplicationException("WriteConcern has no property [" + key + "]");
	}

	@Override
	public Object set(PageContext pc, Key propertyName, Object value) throws PageException {
		throw exp.createApplicationException("Cannot set properties on a WriteConcern object");
	}

	@Override
	public Object setEL(PageContext pc, Key propertyName, Object value) {
		return value;
	}

	// --- Method dispatch ---

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		String name = methodName.getLowerString();

		if (name.equals("isacknowledged")) {
			checkArgLength("isAcknowledged", args, 0, 0);
			return wc.isAcknowledged();
		}
		if (name.equals("tostring")) {
			checkArgLength("toString", args, 0, 0);
			return wc.toString();
		}
		if (name.equals("getw")) {
			checkArgLength("getW", args, 0, 0);
			// getWObject() returns Integer or String depending on how the concern was set
			Object w = wc.getWObject();
			return toCFML(w);
		}
		if (name.equals("getwstring")) {
			checkArgLength("getWString", args, 0, 0);
			return wc.getWString();
		}
		if (name.equals("getjournal")) {
			checkArgLength("getJournal", args, 0, 0);
			Boolean j = wc.getJournal();
			return j != null ? (Object) j : null;
		}

		throw exp.createApplicationException(
			"function [" + methodName + "] does not exist on WriteConcern; " +
			"supported functions are [isAcknowledged, getW, getWString, getJournal, toString]");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		if (args.isEmpty()) return call(pc, methodName, new Object[0]);
		throw new UnsupportedOperationException("named arguments are not supported on WriteConcernImpl");
	}

	/** Let WriteConcern stringify naturally when CFML coerces it to a String. */
	@Override
	public String castToString(String defaultValue) {
		return wc.toString();
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable table = new DumpTable("struct", "#339933", "#8e714e", "#000000");
		table.setTitle("WriteConcern");
		table.appendRow(0, __toDumpData(wc.toString(), pageContext, maxlevel, dp));
		return table;
	}
}
