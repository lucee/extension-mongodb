package org.lucee.mongodb;

import org.bson.types.ObjectId;

import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;

public class MongoDBId implements Function {

	private static final long serialVersionUID = 2766144594043935912L;
	
	public static Object call(PageContext pc) throws PageException {
		return new ObjectIdImpl(ObjectId.get());
	}
}
