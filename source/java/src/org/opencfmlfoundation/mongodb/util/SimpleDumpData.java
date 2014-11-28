package org.opencfmlfoundation.mongodb.util;

import railo.loader.engine.CFMLEngineFactory;
import railo.runtime.dump.DumpData;

public class SimpleDumpData implements DumpData {

	private String data;

	public SimpleDumpData(String data) {
		this.data=data;
	}
	public SimpleDumpData(double data) {
		this.data=CFMLEngineFactory.getInstance().getCastUtil().toString(data);
	}

	public SimpleDumpData(boolean data) {
		this.data=CFMLEngineFactory.getInstance().getCastUtil().toString(data);
	}
	
	@Override
	public String toString() {
		return data;
	}
}
