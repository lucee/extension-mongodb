package org.opencfmlfoundation.mongodb;

import railo.runtime.PageContext;
import railo.runtime.dump.DumpData;
import railo.runtime.dump.DumpProperties;
import railo.runtime.dump.DumpTable;
import railo.runtime.exp.PageException;
import railo.runtime.type.Struct;

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
		// getServerUsed();
		if(methodName.equals("getServerUsed")) {
			checkArgLength("getServerUsed",args,0,0);
			return toCFML(cr.getServerUsed());
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
