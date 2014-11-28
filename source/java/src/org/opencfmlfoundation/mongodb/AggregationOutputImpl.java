package org.opencfmlfoundation.mongodb;

import java.util.ArrayList;
import java.util.Iterator;

import org.opencfmlfoundation.mongodb.support.ObjectSupport;

import com.mongodb.AggregationOutput;
import com.mongodb.DBObject;
import com.mongodb.ServerAddress;

public class AggregationOutputImpl extends ObjectSupport {

	private AggregationOutput ao;

	public AggregationOutputImpl(AggregationOutput ao) {
		this.ao=ao;
	}

	public Object getCommand() {
		return toCFML(ao.getCommand());
	}

	public Object getCommandResult() {
		return toCFML(ao.getCommandResult());
	}

	public ServerAddress getServerUsed() {
		return ao.getServerUsed();
	}

	public Iterable results() {
		Iterator<DBObject> it = ao.results().iterator();
		ArrayList<Object> rtn=new ArrayList<Object>();
		while(it.hasNext()){
			rtn.add(new DBObjectImpl(it.next()));
		}
		return rtn;
	}

	public String toString() {
		return ao.toString();
	}

	public AggregationOutput getAggregationOutput(){
		return ao;
	}
}
