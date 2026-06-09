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
import com.mongodb.client.MongoCursor;

public class AggregationOutputImpl extends ObjectSupport {

	private AggregateIterable<Document> iterable;
	private MongoCursor<Document> cursor;

	/** Normal constructor — wraps a live AggregateIterable for iteration. */
	public AggregationOutputImpl(AggregateIterable<Document> iterable) {
		this.iterable = iterable;
	}

	/**
	 * Empty-result constructor used after a {@code $merge} or {@code $out} stage
	 * has been executed via {@code toCollection()}.  Those stages write to a
	 * collection and produce no output documents; this instance reports itself
	 * as already exhausted so callers get an empty result without a second
	 * round-trip to the server.
	 */
	public AggregationOutputImpl() {
		this.iterable = null;
	}

	private MongoCursor<Document> getCursor() {
		if (cursor == null) cursor = iterable.iterator();
		return cursor;
	}

	public boolean hasNext() {
		if (iterable == null) return false;
		return getCursor().hasNext();
	}

	public Object next() {
		if (iterable == null) throw new java.util.NoSuchElementException("aggregation produced no output documents");
		return toCFML(getCursor().next());
	}

	public void close() {
		if (cursor != null) {
			cursor.close();
			cursor = null;
		}
	}

	/**
	 * Set the number of documents to return per server round-trip.
	 * Must be called before iteration begins.
	 * Returns {@code this} for chaining: {@code coll.aggregate([...]).batchSize(100).hasNext()}.
	 */
	public AggregationOutputImpl batchSize(int size) {
		if (iterable != null) iterable = iterable.batchSize(size);
		return this;
	}

	/**
	 * The driver does not expose a getBatchSize() getter on AggregateIterable.
	 * Returns 0 as a safe default, consistent with DBCursorImpl.getBatchSize().
	 */
	public int getBatchSize() {
		return 0;
	}

	public Object results() {
		if (iterable == null) return toCFML(new ArrayList<Object>());
		ArrayList<Object> rtn = new ArrayList<Object>();
		for (Document doc : iterable) {
			rtn.add(toCFML(doc));
		}
		return toCFML(rtn);
	}

	public AggregateIterable<Document> getIterable() {
		return iterable;
	}
}
