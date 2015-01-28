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

import java.util.ArrayList;
import java.util.Iterator;

import org.lucee.mongodb.support.ObjectSupport;

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
