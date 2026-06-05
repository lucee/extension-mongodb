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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.lucee.mongodb.support.DBCollectionImplSupport;

import com.mongodb.client.DistinctIterable;
import com.mongodb.client.ListSearchIndexesIterable;
import com.mongodb.client.model.SearchIndexModel;
import com.mongodb.client.model.SearchIndexType;

import com.mongodb.MongoNamespace;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteManyModel;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

public class DBCollectionImpl extends DBCollectionImplSupport {

	private MongoCollection<Document> coll;
	private MongoDatabase db;

	public DBCollectionImpl(MongoCollection<Document> coll, MongoDatabase db) {
		this.coll = coll;
		this.db = db;
	}

	@Override
	public Object get(PageContext pc, Key key, Object defaultValue) {
		throw new UnsupportedOperationException("there are no properties for this DBCollection!");
	}

	@Override
	public Object get(PageContext pc, Key key) throws PageException {
		throw new UnsupportedOperationException("there are no properties for this DBCollection!");
	}

	@Override
	public Object set(PageContext pc, Key propertyName, Object value) throws PageException {
		throw new UnsupportedOperationException("you cannot set a property to DBCollection!");
	}

	@Override
	public Object setEL(PageContext pc, Key propertyName, Object value) {
		throw new UnsupportedOperationException("you cannot set a property to DBCollection!");
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {

		// aggregate
		if (methodName.equals("aggregate")) {
			int len = checkArgLength("aggregate", args, 1, -1);
			List<Document> pipeline = new ArrayList<Document>();
			boolean allowDiskUse = false;
			Integer batchSize = null;

			if (len == 1 && decision.isArray(args[0])) {
				Array arr = caster.toArray(args[0]);
				if (arr.size() == 0)
					throw exp.createApplicationException("the array passed to aggregate needs at least 1 element");
				Iterator<Object> it0 = arr.valueIterator();
				while (it0.hasNext()) pipeline.add(toDocument(it0.next()));
			} else if (len == 2 && decision.isArray(args[0]) && decision.isStruct(args[1])) {
				Array arr = caster.toArray(args[0]);
				Iterator<Object> it0 = arr.valueIterator();
				while (it0.hasNext()) pipeline.add(toDocument(it0.next()));
				Document opts = toDocument(args[1]);
				if (opts.containsKey("allowDiskUse")) allowDiskUse = caster.toBooleanValue(opts.get("allowDiskUse"));
				if (opts.containsKey("cursor")) {
					Document cursor = toDocument(opts.get("cursor"), null);
					if (cursor != null && cursor.containsKey("batchSize"))
						batchSize = caster.toIntValue(cursor.get("batchSize"));
				}
			} else if (len == 2 && decision.isArray(args[1])) {
				pipeline.add(toDocument(args[0]));
				Array arr = caster.toArray(args[1]);
				Iterator<Object> it0 = arr.valueIterator();
				while (it0.hasNext()) pipeline.add(toDocument(it0.next()));
			} else {
				for (int i = 0; i < len; i++) pipeline.add(toDocument(args[i]));
			}

			var agg = coll.aggregate(pipeline);
			if (allowDiskUse) agg = agg.allowDiskUse(true);
			if (batchSize != null) agg = agg.batchSize(batchSize);

			// $out and $merge are write stages: the driver requires toCollection() to
			// execute them.  Calling iterator() instead silently skips the write and
			// returns the pipeline's input documents — wrong on both counts.
			Document lastStage = pipeline.get(pipeline.size() - 1);
			if (lastStage.containsKey("$out") || lastStage.containsKey("$merge")) {
				agg.toCollection();
				return new AggregationOutputImpl(); // write executed; no documents returned
			}

			return toCFML(agg);
		}

		// count
		if (methodName.equals("count")) {
			int len = checkArgLength("count", args, 0, 1);
			if (len == 0) return toCFML(coll.countDocuments());
			return toCFML(coll.countDocuments(toDocument(args[0])));
		}

		// dataSize
		if (methodName.equals("dataSize")) {
			checkArgLength("dataSize", args, 0, 0);
			return toCFML(getCollStats().get("size"));
		}

		// distinct
		if (methodName.equals("distinct")) {
			int len = checkArgLength("distinct", args, 1, 2);
			String field = caster.toString(args[0]);
			// Object.class has no registered codec in driver 5.x; BsonValue.class works for any field type
			DistinctIterable<BsonValue> di = coll.distinct(field, BsonValue.class);
			if (len == 2) di = di.filter(toDocument(args[1]));
			List<Object> result = new ArrayList<Object>();
			for (BsonValue bv : di) {
				result.add(bsonValueToJava(bv));
			}
			return toCFML(result);
		}

		// drop
		if (methodName.equals("drop")) {
			checkArgLength("drop", args, 0, 0);
			coll.drop();
			return null;
		}

		// dropIndex
		if (methodName.equals("dropIndex")) {
			checkArgLength("dropIndex", args, 1, 1);
			Document dbo = toDocument(args[0], null);
			if (dbo != null) coll.dropIndex(dbo);
			else coll.dropIndex(caster.toString(args[0]));
			return null;
		}

		// dropIndexes
		if (methodName.equals("dropIndexes")) {
			int len = checkArgLength("dropIndexes", args, 0, 1);
			if (len == 0) { coll.dropIndexes(); return null; }
			coll.dropIndex(caster.toString(args[0]));
			return null;
		}

		// createIndex / ensureIndex
		if (methodName.equals("createIndex") || methodName.equals("ensureIndex")) {
			int len = checkArgLength("createIndex", args, 1, 3);
			if (len == 1) {
				Document dbo = toDocument(args[0], null);
				if (dbo != null) coll.createIndex(dbo);
				else coll.createIndex(new Document(caster.toString(args[0]), 1));
				return null;
			}
			if (len == 2) {
				Document keys = toDocument(args[0]);
				Document opts = toDocument(args[1], null);
				if (opts != null) {
					IndexOptions idxOpts = new IndexOptions();
					if (opts.containsKey("name"))
						idxOpts.name(caster.toString(opts.get("name")));
					if (opts.containsKey("unique"))
						idxOpts.unique(caster.toBooleanValue(opts.get("unique")));
					if (opts.containsKey("sparse"))
						idxOpts.sparse(caster.toBooleanValue(opts.get("sparse")));
					if (opts.containsKey("background"))
						// MongoDB 4.2+ ignores this (all builds are non-blocking), but the
						// driver still accepts and forwards it, so pass it through
						idxOpts.background(caster.toBooleanValue(opts.get("background")));
					if (opts.containsKey("expireAfterSeconds"))
						idxOpts.expireAfter(caster.toLongValue(opts.get("expireAfterSeconds")), TimeUnit.SECONDS);
					if (opts.containsKey("partialFilterExpression")) {
						Document pfe = toDocument(opts.get("partialFilterExpression"), null);
						if (pfe != null) idxOpts.partialFilterExpression(pfe);
					}
					if (opts.containsKey("weights")) {
						Document w = toDocument(opts.get("weights"), null);
						if (w != null) idxOpts.weights(w);
					}
					if (opts.containsKey("defaultLanguage"))
						idxOpts.defaultLanguage(caster.toString(opts.get("defaultLanguage")));
					if (opts.containsKey("languageOverride"))
						idxOpts.languageOverride(caster.toString(opts.get("languageOverride")));
					if (opts.containsKey("hidden"))
						idxOpts.hidden(caster.toBooleanValue(opts.get("hidden")));
					if (opts.containsKey("wildcardProjection")) {
						Document wp = toDocument(opts.get("wildcardProjection"), null);
						if (wp != null) idxOpts.wildcardProjection(wp);
					}
					// "dropDups" was removed in MongoDB 3.0 and is not in IndexOptions;
					// silently ignore it so existing callers don't break
					coll.createIndex(keys, idxOpts);
				} else {
					coll.createIndex(keys, new IndexOptions().name(caster.toString(args[1])));
				}
				return null;
			}
			if (len == 3) {
				coll.createIndex(toDocument(args[0]),
					new IndexOptions().name(caster.toString(args[1])).unique(caster.toBooleanValue(args[2])));
				return null;
			}
		}

		// createSearchIndex — Atlas Search and Vector Search (Atlas only)
		// createSearchIndex(definition)
		// createSearchIndex(name, definition)
		// createSearchIndex(name, definition, type)  — type: "search" | "vectorSearch"
		if (methodName.equals("createSearchIndex")) {
			int len = checkArgLength("createSearchIndex", args, 1, 3);
			if (len == 1) {
				return toCFML(coll.createSearchIndex(toDocument(args[0])));
			} else if (len == 2) {
				return toCFML(coll.createSearchIndex(caster.toString(args[0]), toDocument(args[1])));
			} else {
				// Type must go through SearchIndexModel — no 3-arg createSearchIndex on the driver
				SearchIndexType siType = resolveSearchIndexType(caster.toString(args[2], "search"));
				List<String> names = coll.createSearchIndexes(
					java.util.Collections.singletonList(
						new SearchIndexModel(caster.toString(args[0]), toDocument(args[1]), siType)));
				return toCFML(names.isEmpty() ? "" : names.get(0));
			}
		}

		// createSearchIndexes(array of {name, definition [, type]} structs)
		if (methodName.equals("createSearchIndexes")) {
			checkArgLength("createSearchIndexes", args, 1, 1);
			Array siArr = caster.toArray(args[0]);
			List<SearchIndexModel> models = new ArrayList<SearchIndexModel>();
			Iterator<Object> siIt = siArr.valueIterator();
			while (siIt.hasNext()) {
				lucee.runtime.type.Struct sct = caster.toStruct(siIt.next(), null);
				if (sct == null) continue;
				Document def = toDocument(sct.get("definition", null));
				Object nameObj = sct.get("name", null);
				Object typeObj = sct.get("type", null);
				SearchIndexType siType = resolveSearchIndexType(
					typeObj != null ? caster.toString(typeObj, "search") : "search");
				models.add(nameObj != null
					? new SearchIndexModel(caster.toString(nameObj), def, siType)
					: new SearchIndexModel(def));
			}
			return toCFML(coll.createSearchIndexes(models));
		}

		// listSearchIndexes([name]) — returns array of index-definition structs
		if (methodName.equals("listSearchIndexes")) {
			int len = checkArgLength("listSearchIndexes", args, 0, 1);
			ListSearchIndexesIterable<Document> siIterable = coll.listSearchIndexes();
			if (len == 1) siIterable = siIterable.name(caster.toString(args[0]));
			List<Document> siResult = new ArrayList<Document>();
			siIterable.into(siResult);
			return toCFML(siResult);
		}

		// updateSearchIndex(name, definition)
		if (methodName.equals("updateSearchIndex")) {
			checkArgLength("updateSearchIndex", args, 2, 2);
			coll.updateSearchIndex(caster.toString(args[0]), toDocument(args[1]));
			return null;
		}

		// dropSearchIndex(name)
		if (methodName.equals("dropSearchIndex")) {
			checkArgLength("dropSearchIndex", args, 1, 1);
			coll.dropSearchIndex(caster.toString(args[0]));
			return null;
		}

		// getStats / stats
		if (methodName.equals("getStats") || methodName.equals("stats")) {
			checkArgLength("getStats", args, 0, 0);
			return toCFML(getCollStats());
		}

		// getIndexes / getIndexInfo
		if (methodName.equals("getIndexes") || methodName.equals("getIndexInfo")) {
			checkArgLength(methodName.getString(), args, 0, 0);
			List<Document> indexes = new ArrayList<Document>();
			coll.listIndexes().into(indexes);
			return toCFML(indexes);
		}

		// getWriteConcern
		if (methodName.equals("getWriteConcern")) {
			checkArgLength("getWriteConcern", args, 0, 0);
			return new WriteConcernImpl(coll.getWriteConcern());
		}

		// find
		if (methodName.equals("find")) {
			int len = checkArgLength("find", args, 0, 3);
			FindIterable<Document> cursor;
			Document findFilter = null;
			if (len == 0) {
				cursor = coll.find();
			} else if (len == 1) {
				findFilter = toDocument(args[0]);
				cursor = coll.find(findFilter);
			} else if (len == 2) {
				findFilter = toDocument(args[0]);
				cursor = coll.find(findFilter).projection(toDocument(args[1]));
			} else {
				findFilter = toDocument(args[0]);
				cursor = coll.find(findFilter).projection(toDocument(args[1])).skip(caster.toIntValue(args[2]));
			}
			return new DBCursorImpl(cursor, coll, findFilter);
		}

		// findOne
		if (methodName.equals("findOne")) {
			int len = checkArgLength("findOne", args, 0, 3);
			Document result;
			if (len == 0) {
				result = coll.find().first();
			} else if (len == 1) {
				Document filter = toDocument(args[0], null);
				result = filter != null ? coll.find(filter).first() : coll.find().first();
			} else if (len == 2) {
				Document filter = toDocument(args[0], null);
				Document projection = toDocument(args[1]);
				result = filter != null
					? coll.find(filter).projection(projection).first()
					: coll.find().projection(projection).first();
			} else {
				result = coll.find(toDocument(args[0])).projection(toDocument(args[1])).sort(toDocument(args[2])).first();
			}
			return toCFML(result);
		}

		// findAndRemove
		if (methodName.equals("findAndRemove")) {
			checkArgLength("findAndRemove", args, 1, 1);
			Document result = coll.findOneAndDelete(toDocument(args[0]));
			return toCFML(result);
		}

		// findAndModify
		if (methodName.equals("findAndModify")) {
			int len = args == null ? 0 : args.length;
			if (len != 2 && len != 3 && len != 7)
				throw exp.createApplicationException("findAndModify needs 2, 3 or 7 arguments, but got " + len);
			if (len == 2) {
				Document q2 = toDocument(args[0]);
				Document u2 = toDocument(args[1]);
				if (isUpdateOperatorDoc(u2)) return toCFML(coll.findOneAndUpdate(q2, u2));
				else return toCFML(coll.findOneAndReplace(q2, u2));
			} else if (len == 3) {
				Document q3 = toDocument(args[0]);
				Document u3 = toDocument(args[1]);
				Document sort3 = toDocument(args[2]);
				if (isUpdateOperatorDoc(u3)) {
					return toCFML(coll.findOneAndUpdate(q3, u3, new FindOneAndUpdateOptions().sort(sort3)));
				} else {
					return toCFML(coll.findOneAndReplace(q3, u3, new com.mongodb.client.model.FindOneAndReplaceOptions().sort(sort3)));
				}
			} else {
				// (query, fields, sort, remove, update, returnNew, upsert)
				Document query = toDocument(args[0]);
				Document fields = toDocument(args[1], null);
				Document sort = toDocument(args[2], null);
				boolean remove = caster.toBooleanValue(args[3]);
				Document update = toDocument(args[4], null);
				boolean returnNew = caster.toBooleanValue(args[5]);
				boolean upsert = caster.toBooleanValue(args[6]);
				if (remove) {
					FindOneAndDeleteOptions opts = new FindOneAndDeleteOptions();
					if (sort != null) opts.sort(sort);
					if (fields != null) opts.projection(fields);
					return toCFML(coll.findOneAndDelete(query, opts));
				} else if (isUpdateOperatorDoc(update)) {
					FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions()
						.returnDocument(returnNew ? ReturnDocument.AFTER : ReturnDocument.BEFORE)
						.upsert(upsert);
					if (sort != null) opts.sort(sort);
					if (fields != null) opts.projection(fields);
					return toCFML(coll.findOneAndUpdate(query, update, opts));
				} else {
					com.mongodb.client.model.FindOneAndReplaceOptions opts = new com.mongodb.client.model.FindOneAndReplaceOptions()
						.returnDocument(returnNew ? ReturnDocument.AFTER : ReturnDocument.BEFORE)
						.upsert(upsert);
					if (sort != null) opts.sort(sort);
					if (fields != null) opts.projection(fields);
					return toCFML(coll.findOneAndReplace(query, update, opts));
				}
			}
		}

		// insert
		if (methodName.equals("insert")) {
			checkArgLength("insert", args, 1, 1);
			Document[] docs = toDBObjectArray(args[0]);
			Document result = new Document();
			if (docs.length == 1) {
				coll.insertOne(docs[0]);
				result.put("n", 1);
			} else {
				List<Document> list = new ArrayList<Document>();
				for (Document d : docs) list.add(d);
				coll.insertMany(list);
				result.put("n", docs.length);
			}
			result.put("acknowledged", true);
			return new WriteResultImpl(result);
		}

		// insertMany
		if (methodName.equals("insertMany")) {
			int len = checkArgLength("insertMany", args, 1, 2);
			boolean ordered = true;
			WriteConcern wc = coll.getWriteConcern();

			if (len == 2) {
				Document opts = toDocument(args[1]);
				if (opts.containsKey("ordered")) ordered = caster.toBooleanValue(opts.get("ordered"));
				if (opts.containsKey("writeconcern")) {
					WriteConcern newWc = toWriteConcern(opts.get("writeconcern"), null);
					if (newWc != null) wc = newWc;
				}
			}

			Array arr = caster.toArray(args[0]);
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			List<Map> writeErrors = new ArrayList<Map>();

			if (arr.size() == 0) {
				result.put("nInserted", 0);
				result.put("writeErrors", writeErrors);
				result.put("acknowledged", true);
				return toCFML(result);
			}

			List<WriteModel<Document>> models = new ArrayList<WriteModel<Document>>();
			Iterator<Object> it1 = arr.valueIterator();
			while (it1.hasNext()) {
				models.add(new InsertOneModel<Document>(toDocument(it1.next())));
			}

			BulkWriteResult bulkResult;
			try {
				bulkResult = coll.withWriteConcern(wc).bulkWrite(models, new BulkWriteOptions().ordered(ordered));
			} catch (MongoBulkWriteException e) {
				bulkResult = e.getWriteResult();
				for (BulkWriteError err : e.getWriteErrors()) {
					Map<String, Object> errItem = new LinkedHashMap<String, Object>();
					errItem.put("index", err.getIndex() + 1);
					errItem.put("code", err.getCode());
					errItem.put("errmsg", err.getMessage());
					errItem.put("op", err.getDetails().toString());
					writeErrors.add(errItem);
				}
			}
			result.put("acknowledged", bulkResult.wasAcknowledged());
			if (bulkResult.wasAcknowledged()) {
				result.put("nInserted", bulkResult.getInsertedCount());
				result.put("writeErrors", writeErrors);
			}
			return toCFML(result);
		}

		// bulkWrite
		if (methodName.equals("bulkWrite")) {
			int len = checkArgLength("bulkWrite", args, 1, 2);
			boolean ordered = true;
			boolean bypassDocValidation = false;
			WriteConcern wc = coll.getWriteConcern();

			if (len == 2) {
				Document opts = toDocument(args[1]);
				if (opts.containsKey("ordered")) ordered = caster.toBooleanValue(opts.get("ordered"));
				if (opts.containsKey("bypassDocumentValidation"))
					bypassDocValidation = caster.toBooleanValue(opts.get("bypassDocumentValidation"));
				if (opts.containsKey("writeconcern")) {
					WriteConcern newWc = toWriteConcern(opts.get("writeconcern"), null);
					if (newWc != null) wc = newWc;
				}
			}

			Array arr = caster.toArray(args[0]);
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			List<Map> writeErrors = new ArrayList<Map>();

			if (arr.size() == 0) {
				result.put("nInserted", 0); result.put("nMatched", 0);
				result.put("nModified", 0); result.put("nRemoved", 0);
				result.put("writeErrors", writeErrors); result.put("acknowledged", true);
				return toCFML(result);
			}

			List<WriteModel<Document>> models = new ArrayList<WriteModel<Document>>();
			Iterator<Object> it2 = arr.valueIterator();
			while (it2.hasNext()) {
				Document op = toDocument(it2.next());
				String operation = (String) op.get("operation");
				if ("update".equals(operation)) {
					Document upd = toDocument(op.get("update"));
					if (isUpdateOperatorDoc(upd)) models.add(new UpdateManyModel<Document>(toDocument(op.get("query")), upd));
					else models.add(new ReplaceOneModel<Document>(toDocument(op.get("query")), upd));
				} else if ("updateOne".equals(operation)) {
					Document upd = toDocument(op.get("update"));
					if (isUpdateOperatorDoc(upd)) models.add(new UpdateOneModel<Document>(toDocument(op.get("query")), upd));
					else models.add(new ReplaceOneModel<Document>(toDocument(op.get("query")), upd));
				} else if ("remove".equals(operation)) {
					models.add(new DeleteManyModel<Document>(toDocument(op.get("query"))));
				} else if ("removeOne".equals(operation)) {
					models.add(new DeleteOneModel<Document>(toDocument(op.get("query"))));
				} else if ("insert".equals(operation)) {
					models.add(new InsertOneModel<Document>(toDocument(op.get("document"))));
				}
			}

			BulkWriteResult bulkResult;
			try {
				BulkWriteOptions bulkOpts = new BulkWriteOptions().ordered(ordered).bypassDocumentValidation(bypassDocValidation);
				bulkResult = coll.withWriteConcern(wc).bulkWrite(models, bulkOpts);
			} catch (MongoBulkWriteException e) {
				bulkResult = e.getWriteResult();
				for (BulkWriteError err : e.getWriteErrors()) {
					Map<String, Object> errItem = new LinkedHashMap<String, Object>();
					errItem.put("index", err.getIndex() + 1);
					errItem.put("code", err.getCode());
					errItem.put("errmsg", err.getMessage());
					errItem.put("op", err.getDetails().toString());
					writeErrors.add(errItem);
				}
			}
			result.put("acknowledged", bulkResult.wasAcknowledged());
			if (bulkResult.wasAcknowledged()) {
				result.put("nInserted", bulkResult.getInsertedCount());
				result.put("nMatched", bulkResult.getMatchedCount());
				result.put("nModified", bulkResult.getModifiedCount());
				result.put("nRemoved", bulkResult.getDeletedCount());
				result.put("writeErrors", writeErrors);
			}
			return toCFML(result);
		}

		// mapReduce — removed in MongoDB driver 5.x
		if (methodName.equals("mapReduce")) {
			throw exp.createApplicationException("mapReduce() was removed in MongoDB Java driver 5.x. Use the aggregation pipeline with $group instead.");
		}

		// remove
		if (methodName.equals("remove")) {
			checkArgLength("remove", args, 1, 1);
			DeleteResult dr = coll.deleteMany(toDocument(args[0]));
			Document result = new Document();
			result.put("acknowledged", dr.wasAcknowledged());
			result.put("n", dr.wasAcknowledged() ? dr.getDeletedCount() : 0);
			return new WriteResultImpl(result);
		}

		// rename / renameCollection
		if (methodName.equals("rename") || methodName.equals("renameCollection")) {
			int len = checkArgLength(methodName.getString(), args, 1, 2);
			String newName = caster.toString(args[0]);
			String dbName = coll.getNamespace().getDatabaseName();
			MongoNamespace newNs = new MongoNamespace(dbName, newName);
			if (len == 2 && caster.toBooleanValue(args[1])) {
				coll.renameCollection(newNs, new RenameCollectionOptions().dropTarget(true));
			} else {
				coll.renameCollection(newNs);
			}
			return null;
		}

		// save — implemented as upsert
		if (methodName.equals("save")) {
			checkArgLength("save", args, 1, 1);
			Document doc = toDocument(args[0]);
			Document result = new Document();
			if (doc.containsKey("_id")) {
				Document filter = new Document("_id", doc.get("_id"));
				UpdateResult ur = coll.replaceOne(filter, doc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
				result.put("acknowledged", ur.wasAcknowledged());
				result.put("n", ur.wasAcknowledged() ? (ur.getUpsertedId() != null ? 1L : ur.getMatchedCount()) : 0L);
				result.put("updatedExisting", ur.wasAcknowledged() && ur.getMatchedCount() > 0);
			} else {
				InsertOneResult ir = coll.insertOne(doc);
				result.put("acknowledged", ir.wasAcknowledged());
				result.put("n", 1L);
				result.put("updatedExisting", false);
			}
			return new WriteResultImpl(result);
		}

		// setWriteConcern
		if (methodName.equals("setWriteConcern")) {
			checkArgLength("setWriteConcern", args, 1, 1);
			WriteConcern wc = toWriteConcern(args[0], null);
			if (wc != null) coll = coll.withWriteConcern(wc);
			return null;
		}

		// storageSize
		if (methodName.equals("storageSize")) {
			checkArgLength("storageSize", args, 0, 0);
			return toCFML(getCollStats().get("storageSize"));
		}

		// totalIndexSize
		if (methodName.equals("totalIndexSize")) {
			checkArgLength("totalIndexSize", args, 0, 0);
			return toCFML(getCollStats().get("totalIndexSize"));
		}

		// update
		if (methodName.equals("update")) {
			int len = checkArgLength("update", args, 2, 4);
			Document filter = toDocument(args[0]);
			Document update = toDocument(args[1]);
			boolean upsert = len >= 3 && caster.toBooleanValue(args[2]);
			boolean multi = len >= 4 && caster.toBooleanValue(args[3]);
			UpdateResult ur;
			if (isUpdateOperatorDoc(update)) {
				com.mongodb.client.model.UpdateOptions opts = new com.mongodb.client.model.UpdateOptions().upsert(upsert);
				ur = multi ? coll.updateMany(filter, update, opts) : coll.updateOne(filter, update, opts);
			} else {
				// Replacement document: driver 5.x requires replaceOne instead of updateOne
				com.mongodb.client.model.ReplaceOptions opts = new com.mongodb.client.model.ReplaceOptions().upsert(upsert);
				ur = coll.replaceOne(filter, update, opts);
			}
			Document result = new Document();
			result.put("acknowledged", ur.wasAcknowledged());
			result.put("n", ur.wasAcknowledged() ? ur.getMatchedCount() : 0);
			result.put("nModified", ur.wasAcknowledged() ? ur.getModifiedCount() : 0);
			result.put("updatedExisting", ur.wasAcknowledged() && ur.getMatchedCount() > 0);
			return new WriteResultImpl(result);
		}

		String functionNames = "aggregate,count,dataSize,distinct,drop,dropIndex,dropIndexes,createIndex,stats,getIndexes," +
			"createSearchIndex,createSearchIndexes,listSearchIndexes,updateSearchIndex,dropSearchIndex," +
			"getWriteConcern,find,findOne,findAndRemove,findAndModify,insert,insertMany,bulkWrite,remove,rename,save," +
			"setWriteConcern,storageSize,totalIndexSize,update";
		throw exp.createApplicationException("function " + methodName + " does not exist, existing functions are [" + functionNames + "]");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable table = new DumpTable("struct", "#339933", "#8e714e", "#000000");
		table.setTitle("DBCollection");
		table.appendRow(1,
			__toDumpData("namespace",     pageContext, maxlevel, dp),
			__toDumpData(coll.getNamespace().getFullName(), pageContext, maxlevel, dp));
		table.appendRow(1,
			__toDumpData("documentCount", pageContext, maxlevel, dp),
			__toDumpData(coll.countDocuments(), pageContext, maxlevel, dp));
		table.appendRow(1,
			__toDumpData("writeConcern",  pageContext, maxlevel, dp),
			__toDumpData(coll.getWriteConcern().toString(), pageContext, maxlevel, dp));
		List<String> idxNames = new ArrayList<String>();
		for (Document idx : coll.listIndexes()) {
			String name = idx.getString("name");
			if (name != null) idxNames.add(name);
		}
		table.appendRow(1,
			__toDumpData("indexes", pageContext, maxlevel, dp),
			__toDumpData(String.join(", ", idxNames), pageContext, maxlevel, dp));
		return table;
	}

	/** Map a CFML type string ("search" or "vectorSearch") to the driver enum. */
	private static SearchIndexType resolveSearchIndexType(String type) {
		if (type != null && type.equalsIgnoreCase("vectorSearch"))
			return SearchIndexType.vectorSearch();
		return SearchIndexType.search(); // default: Atlas Search
	}

	private static boolean isUpdateOperatorDoc(Document doc) {
		if (doc == null || doc.isEmpty()) return false;
		String firstKey = doc.keySet().iterator().next();
		return firstKey.startsWith("$");
	}

	private Document getCollStats() {
		if (db != null) {
			return db.runCommand(new Document("collStats", coll.getNamespace().getCollectionName()));
		}
		return new Document();
	}

	public MongoCollection<Document> getCollection() {
		return coll;
	}

	/**
	 * Convert a {@link BsonValue} (returned by {@code distinct()} and similar driver APIs) to
	 * a plain Java object that {@link #toCFML} can handle.  Handles all common BSON types
	 * recursively; falls back to the BSON string representation for anything exotic.
	 */
	private Object bsonValueToJava(BsonValue bv) {
		if (bv == null || bv.isNull() || bv.getBsonType() == org.bson.BsonType.UNDEFINED) return null;
		if (bv.isString()) return bv.asString().getValue();
		if (bv.isBoolean()) return bv.asBoolean().getValue();
		if (bv.isInt32()) return (double) bv.asInt32().getValue();
		if (bv.isInt64()) return (double) bv.asInt64().getValue();
		if (bv.isDouble()) return bv.asDouble().getValue();
		if (bv.isDecimal128()) return bv.asDecimal128().getValue().bigDecimalValue().doubleValue();
		if (bv.isObjectId()) return new ObjectIdImpl(bv.asObjectId().getValue());
		if (bv.isDateTime()) return new java.util.Date(bv.asDateTime().getValue());
		if (bv.isDocument()) {
			// Convert BsonDocument -> Document by recursing over entries
			Document doc = new Document();
			for (Map.Entry<String, BsonValue> entry : bv.asDocument().entrySet()) {
				doc.put(entry.getKey(), bsonValueToJava(entry.getValue()));
			}
			return toCFML(doc);
		}
		if (bv.isArray()) {
			List<Object> list = new ArrayList<Object>();
			for (BsonValue item : bv.asArray()) {
				list.add(bsonValueToJava(item));
			}
			return toCFML(list);
		}
		// Fallback: use BSON extended-JSON representation
		return bv.toString();
	}
}
