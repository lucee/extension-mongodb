package org.opencfmlfoundation.mongodb;

import org.bson.types.ObjectId;

import railo.runtime.PageContext;
import railo.runtime.exp.PageException;
import railo.runtime.ext.function.Function;

public class MongoDBId implements Function {

	private static final long serialVersionUID = 2766144594043935912L;
	
	public static Object call(PageContext pc) throws PageException {
		return new ObjectIdImpl(ObjectId.get());
	}
}
