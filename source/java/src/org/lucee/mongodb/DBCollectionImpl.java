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
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import org.lucee.mongodb.support.DBCollectionImplSupport;
import org.lucee.mongodb.util.print;

import com.mongodb.DBCollection;
import com.mongodb.Cursor;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.AggregationOptions;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.BulkWriteException;
import com.mongodb.BulkWriteError;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;

public class DBCollectionImpl extends DBCollectionImplSupport {

	private DBCollection coll;

	public DBCollectionImpl(DBCollection coll) {
		this.coll=coll;
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
		if(methodName.equals("aggregate")) {
			boolean hasOptions = false;
			AggregationOptions options = null;
			int len=checkArgLength("aggregate",args,1,-1); // no length limitation
			List<DBObject> pipeline = new ArrayList<DBObject>();
			// Pipeline array as single argument
			if(len==1 && decision.isArray(args[0])) {
				Array arr = caster.toArray(args[0]);
				if(arr.size()==0)
					throw exp.createApplicationException("the array passed to the function aggregate needs at least 1 element");

				Iterator<Object> it = arr.valueIterator();
				while(it.hasNext()){
					pipeline.add(toDBObject(it.next()));
				}
			}
			else {
				// First argument is pipeline of operations, second argument is struct of options --> returns cursor!
				if(len==2 && decision.isArray(args[0]) && decision.isStruct(args[1])){
					Array arr = caster.toArray(args[0]);
					Iterator<Object> it = arr.valueIterator();
					while(it.hasNext()){
						pipeline.add(toDBObject(it.next()));
					}

					hasOptions = true;
					// options builder
					AggregationOptions.Builder optbuilder = AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR);

					DBObject dboOpts = toDBObject(args[1]);
					if (dboOpts.containsField("allowDiskUse")){
						if (!decision.isBoolean(dboOpts.get("allowDiskUse")))
							throw exp.createApplicationException("allowDiskUse in options must be boolean value");

						optbuilder = optbuilder.allowDiskUse(caster.toBooleanValue(dboOpts.get("allowDiskUse")));
					}
					if (dboOpts.containsField("cursor")){
						if (!decision.isStruct(dboOpts.get("cursor")))
							throw exp.createApplicationException("cursor in options must be struct with optional key batchSize");

						DBObject cursoropts = toDBObject(dboOpts.get("cursor"));
						if (cursoropts.containsField("batchSize")){
							if (!decision.isNumeric(cursoropts.get("batchSize")))
								throw exp.createApplicationException("cursor.batchSize in options must be integer");

							optbuilder = optbuilder.batchSize(caster.toIntValue(cursoropts.get("batchSize")));
						}
					}

					options = optbuilder.build();
				}
				// First argument is first operation, second argument is array of additional operations
				else if(len==2 && decision.isArray(args[1])){
					Array arr = caster.toArray(args[1]);
					pipeline.add(toDBObject(args[0]));
					Iterator<Object> it = arr.valueIterator();
					while(it.hasNext()){
						pipeline.add(toDBObject(it.next()));
					}
				}
				// N arguments of pipeline operations
				else {
					for(int i=0;i<len;i++){
						pipeline.add(toDBObject(args[i]));
					}
				}
			}

			if (hasOptions){
				// returns Cursor - requires >= MongoDB 2.6
				return toCFML(coll.aggregate(pipeline,options));
			} else {
				// returns AggregationOutput
				return toCFML(coll.aggregate(pipeline));
			}
		}
		// count
		if(methodName.equals("count")) {
			int len=checkArgLength("count",args,0,1);
			if(len==0) {
				return toCFML(coll.count());
			}
			else if(len==1){
				return toCFML(coll.count(toDBObject(args[0])));
			}
		}
		// dataSize
		if(methodName.equals("dataSize")) {
			checkArgLength("dataSize",args,0,0);
			return toCFML(coll.getStats().get("size"));
		}

		// distinct
		if(methodName.equals("distinct")) {
			int len=checkArgLength("distinct",args,1,2);
			if(len==1){
				return toCFML(coll.distinct(
					caster.toString(args[0])
				));
			}
			else if(len==2){
				return toCFML(coll.distinct(
					caster.toString(args[0]),
					toDBObject(args[1])
				));
			}
		}
		// drop
		if(methodName.equals("drop")) {
			checkArgLength("drop",args,0,0);
			coll.drop();
			return null;
		}

		// dropIndex
		if(methodName.equals("dropIndex")) {
			checkArgLength("dropIndex",args,1,1);
			DBObject dbo = toDBObject(args[0], null);
			if(dbo!=null) coll.dropIndex(dbo);
			else coll.dropIndex(caster.toString(args[0]));

			return null;
		}
		// dropIndexes
		if(methodName.equals("dropIndexes")) {
			int len=checkArgLength("dropIndexes",args,0,1);
			if(len==0){
				coll.dropIndexes();
				return null;
			}
			else if(len==1){
				coll.dropIndexes(caster.toString(args[0]));
				return null;
			}
		}

		// createIndex
		if(methodName.equals("createIndex") || methodName.equals("ensureIndex")) {
			int len=checkArgLength("createIndex",args,1,3);
			if(len==1){
				DBObject dbo = toDBObject(args[0], null);
				if(dbo!=null) coll.createIndex(dbo);
				else coll.createIndex(caster.toString(args[0]));
				return null;
			}
			if(len==2){
				DBObject p1 = toDBObject(args[0]);
				DBObject p2 = toDBObject(args[1], null);
				if(p2!=null) coll.createIndex(p1,p2);
				else coll.createIndex(p1,caster.toString(args[1]));
				return null;
			}
			else if(len==3){
				coll.createIndex(
						toDBObject(args[0]),
						caster.toString(args[1]),
						caster.toBooleanValue(args[2])
				);
				return null;
			}
		}

		// getStats
		if(methodName.equals("getStats") || methodName.equals("stats")) {
			checkArgLength("getStats",args,0,0);
			return toCFML(coll.getStats());
		}

		// getIndexes
		if(methodName.equals("getIndexes") || methodName.equals("getIndexInfo")) {
			checkArgLength(methodName.getString(),args,0,0);
			return toCFML(coll.getIndexInfo());
		}

		// getWriteConcern
		if(methodName.equals("getWriteConcern")) {
			checkArgLength("getWriteConcern",args,0,0);
			return toCFML(coll.getWriteConcern());
		}

		// find
		if(methodName.equals("find")) {
			int len=checkArgLength("find",args,0,3);
			DBCursor cursor=null;
			if(len==0) {
				cursor=coll.find();
			}
			else if(len==1){
				cursor=coll.find(
					toDBObject(args[0])
				);
			}
			else if(len==2){
				cursor=coll.find(
					toDBObject(args[0]),
					toDBObject(args[1])
				);
			}
			else if(len==3){
				cursor=coll.find(
					toDBObject(args[0]),
					toDBObject(args[1])
				).skip(caster.toIntValue(args[2]));
			}

			return toCFML(cursor);
		}
		// findOne
		else if(methodName.equals("findOne")) {
			int len=checkArgLength("findOne",args,0,3);
			DBObject obj=null;
			if(len==0) {
				obj=coll.findOne();
			}
			else if(len==1){
				DBObject arg1 = toDBObject(args[0],null);
				if(arg1!=null)obj=coll.findOne(arg1);
				else obj=coll.findOne(args[0]);

			}
			else if(len==2){
				DBObject arg1 = toDBObject(args[0],null);
				if(arg1!=null) obj=coll.findOne(arg1,toDBObject(args[1]));
				else obj=coll.findOne(args[0],toDBObject(args[1]));
			}
			else if(len==3){
				obj=coll.findOne(
					toDBObject(args[0]),
					toDBObject(args[1]),
					toDBObject(args[2])
				);
			}
			return toCFML(obj);
		}
		// findAndRemove
		if(methodName.equals("findAndRemove")) {
			checkArgLength("findAndRemove",args,1,1);
			DBObject obj = coll.findAndRemove(toDBObject(args[0]));
			return toCFML(obj);
		}
		// findAndModify
		if(methodName.equals("findAndModify")) {
			int len=checkArgLength("findAndModify",args,2,3);
			DBObject obj=null;
			if(len==2){
				obj=coll.findAndModify(
					toDBObject(args[0]),
					toDBObject(args[1])
				);
			}
			if(len==3){
				obj=coll.findAndModify(
					toDBObject(args[0]),
					toDBObject(args[1]),
					toDBObject(args[2])
				);
			}
			// TODO more options

			return toCFML(obj);
		}

		//group
		/*
			TODO: needs GroupCommand
		if(methodName.equals("group")) {
			int len=checkArgLength("group",args,1,1);
			if(len==1){
				return toCFML(coll.group(
					toDBObject(args[0])
				));
			}
		}*/

		// insert
		if(methodName.equals("insert")) {
			checkArgLength("insert",args,1,1);
			return toCFML(coll.insert(
					toDBObjectArray(args[0]))
				);
		}

		// insertMany(required array documents, struct options) valid options keys are string "writeconcern", boolean "ordered"
		if(methodName.equals("insertMany")) {
			int len = checkArgLength("insert",args,1,2);
			BulkWriteOperation bulk = coll.initializeOrderedBulkOperation();
			WriteConcern wc = coll.getWriteConcern();

			if (len==2) {
				DBObject dboOpts = toDBObject(args[1]);
				if (dboOpts.containsField("ordered")){
					if (!decision.isBoolean(dboOpts.get("ordered")))
						throw exp.createApplicationException("ordered in options must be boolean value");

					if (!caster.toBooleanValue(dboOpts.get("ordered"))) {
						bulk = coll.initializeUnorderedBulkOperation();
					}
				}

				if (dboOpts.containsField("writeconcern")){
					WriteConcern newWc = WriteConcern.valueOf(caster.toString(dboOpts.get("writeconcern")));
					if (newWc != null) {
						wc = newWc;
					}
				}
			}

			Map result = new HashMap();
			BulkWriteResult bulkResult;
			List<Map> writeErrors = new ArrayList();
			
			Array arr = caster.toArray(args[0]);
			if(arr.size()==0) {
				result.put("nInserted",0);	
				result.put("writeErrors",writeErrors);	
				result.put("acknowledged",true);
				return toCFML(result);	
			}

			Iterator<Object> it = arr.valueIterator();
			while(it.hasNext()){
				bulk.insert(toDBObject(it.next()));
			}

			try {
				bulkResult = bulk.execute(wc);
			} catch (BulkWriteException e) {
				Map bulkErrorItem;
				BulkWriteError bulkError;
	
				bulkResult = e.getWriteResult();
				List<BulkWriteError> errors = e.getWriteErrors();

				Iterator<BulkWriteError> jj = errors.iterator();
				while (jj.hasNext()) {
					bulkErrorItem = new HashMap();
					bulkError = jj.next();
					bulkErrorItem.put("index",(bulkError.getIndex()+1)); // +1 so we get index of item in CFML array
					bulkErrorItem.put("code",bulkError.getCode());
					bulkErrorItem.put("errmsg",bulkError.getMessage());
					bulkErrorItem.put("op",bulkError.getDetails());
					writeErrors.add( bulkErrorItem );
				}
			}

			result.put("acknowledged", bulkResult.isAcknowledged());
			if (bulkResult.isAcknowledged()) {
				result.put("nInserted", bulkResult.getInsertedCount());
				result.put("writeErrors", writeErrors);
			}

			return toCFML(result);
		}

		//mapReduce
		if(methodName.equals("mapReduce")) {
			int len=checkArgLength("mapReduce",args,4,4);
			if(len==4){
				return toCFML(coll.mapReduce(
					caster.toString(args[0]),
					caster.toString(args[1]),
					caster.toString(args[2]),
					toDBObject(args[3])
				));
			}
		}

		// remove
		if(methodName.equals("remove")) {
			checkArgLength("remove",args,1,1);
			return toCFML(coll.remove(toDBObject(args[0])));

		}

		// rename
		if(methodName.equals("rename") || methodName.equals("renameCollection")) {
			int len=checkArgLength(methodName.getString(),args,1,2);
			if(len==1){
				return toCFML(coll.rename(
					caster.toString(args[0])
				));
			}
			else if(len==2){
				return toCFML(coll.rename(
						caster.toString(args[0]),
						caster.toBooleanValue(args[1])
					));
			}
		}

		// save
		if(methodName.equals("save")) {
			checkArgLength("save",args,1,1);
			return toCFML(coll.save(
					toDBObject(args[0]))
				);
		}

		// setWriteConcern
		if(methodName.equals("setWriteConcern")) {
			checkArgLength("setWriteConcern",args,1,1);
			WriteConcern wc = WriteConcern.valueOf(caster.toString(args[0]));
			if (wc != null) {
				coll.setWriteConcern(wc);
			}
			return null;
		}

		// storageSize
		if(methodName.equals("storageSize")) {
			checkArgLength("storageSize",args,0,0);
			return toCFML(coll.getStats().get("storageSize"));
		}

		// totalIndexSize
		if(methodName.equals("totalIndexSize")) {
			checkArgLength("totalIndexSize",args,0,0);
			return toCFML(coll.getStats().get("totalIndexSize"));
		}

		// update
		if(methodName.equals("update")) {
			int len = checkArgLength("update",args,2,4);
			if(len==2){
				return toCFML(coll.update(
					toDBObject(args[0]),
					toDBObject(args[1])
				));
			}
			else if(len==3){
				return toCFML(coll.update(
					toDBObject(args[0]),
					toDBObject(args[1]),
					caster.toBooleanValue(args[2]),
					false
				));
			}
			else if(len==4){
				return toCFML(coll.update(
					toDBObject(args[0]),
					toDBObject(args[1]),
					caster.toBooleanValue(args[2]),
					caster.toBooleanValue(args[3])
				));
			}
		}


		String functionNames = "aggregate,count,dataSize,distinct,drop,dropIndex,dropIndexes,createIndex,stats,getIndexes,getWriteConcern,find,findOne,findAndRemove,findAndModify," +
		"group,insert,insertMany,mapReduce,remove,rename,save,setWriteConcern,storageSize,totalIndexSize,update";

		throw exp.createApplicationException("function "+methodName+" does not exist existing functions are ["+functionNames+"]");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}


	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DBCursor cursor = coll.find();
		Iterator<DBObject> it = cursor.iterator();
		DumpTable table = new DumpTable("struct","#339933","#8e714e","#000000");
		table.setTitle("DBCollection");

		maxlevel--;
		DBObject obj;
		while(it.hasNext()) {
			obj = it.next();
			table.appendRow(0,
					__toDumpData(toCFML(obj), pageContext,maxlevel,dp)
				);
		}
		return table;
	}

	public DBCollection getDBCollection() {
		return coll;
	}

}
