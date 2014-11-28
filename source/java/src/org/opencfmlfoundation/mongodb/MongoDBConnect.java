package org.opencfmlfoundation.mongodb;

import java.net.UnknownHostException;

import railo.loader.engine.CFMLEngineFactory;
import railo.runtime.PageContext;
import railo.runtime.exp.PageException;
import railo.runtime.ext.function.Function;

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
		catch (UnknownHostException e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}
}
