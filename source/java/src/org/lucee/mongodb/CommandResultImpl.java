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

import com.mongodb.CommandResult;

public class CommandResultImpl extends DBObjectImpl {

	private CommandResult cr;

	public CommandResultImpl(CommandResult cr) {
		super(cr);
		this.cr=cr;
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		// getErrorMessage();
		if(methodName.equals("getErrorMessage")) {
			checkArgLength("getErrorMessage",args,0,0);
			return toCFML(cr.getErrorMessage());
		}
		// getException();
		if(methodName.equals("getException")) {
			checkArgLength("getException",args,0,0);
			return toCFML(cr.getException());
		}
		// ok();
		if(methodName.equals("ok")) {
			checkArgLength("ok",args,0,0);
			return toCFML(cr.ok());
		}
		// throwOnError();
		if(methodName.equals("throwOnError")) {
			checkArgLength("throwOnError",args,0,0);
			cr.throwOnError();
			return null;
		}
		return super.call(pc, methodName, args);
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName,Struct args) throws PageException {
		if(args.isEmpty()) return call(pc, methodName, new Object[0]);
		return super.callWithNamedValues(pc, methodName, args);
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable dt= (DumpTable) super.toDumpData(pageContext, maxlevel, dp);
		dt.setTitle("CommandResult");
		return dt;
	}



}
