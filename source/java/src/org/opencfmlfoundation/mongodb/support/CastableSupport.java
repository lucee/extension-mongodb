package org.opencfmlfoundation.mongodb.support;

import railo.runtime.exp.PageException;
import railo.runtime.op.Castable;
import railo.runtime.type.dt.DateTime;

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
