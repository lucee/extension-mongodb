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

import org.bson.Document;
import org.lucee.mongodb.support.ObjectSupport;

import com.mongodb.client.AggregateIterable;

public class AggregationOutputImpl extends ObjectSupport {

	private AggregateIterable<Document> iterable;

	public AggregationOutputImpl(AggregateIterable<Document> iterable) {
		this.iterable = iterable;
	}

	public Object results() {
		ArrayList<Object> rtn = new ArrayList<Object>();
		for (Document doc : iterable) {
			rtn.add(new DBObjectImpl(doc));
		}
		return toCFML(rtn);
	}

	public AggregateIterable<Document> getIterable() {
		return iterable;
	}
}
