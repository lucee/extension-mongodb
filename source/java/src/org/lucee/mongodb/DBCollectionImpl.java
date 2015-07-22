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

import java.util.Iterator;

import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Struct;

import org.lucee.mongodb.support.DBCollectionImplSupport;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

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
			int len=checkArgLength("aggregate",args,1,-1); // no length limitation
			DBObject firstArg;
			DBObject[] addArgs;
			// Array
			if(len==1 && decision.isArray(args[0])) {
				Array arr = caster.toArray(args[0]);
				if(arr.size()==0)
					throw exp.createApplicationException("the array passed to the function aggregate needs at least 1 element");

				Iterator<Object> it = arr.valueIterator();
				firstArg=toDBObject(it.next());
				addArgs=new DBObject[arr.size()-1];
				int i=0;
				while(it.hasNext()){
					addArgs[i++]=toDBObject(it.next());
				}
			}
			else {
				firstArg=toDBObject(args[0]);
				// Second argument is array
				if(len==2 && decision.isArray(args[1])){
					addArgs=toDBObjectArray(args[1]);
				}
				else {
					addArgs=new DBObject[len-1];
					for(int i=1;i<len;i++){
						addArgs[i-1]=toDBObject(args[i]);
					}
				}
			}
			return toCFML(coll.aggregate(firstArg, addArgs));
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

		//mapReduce
		/*
			TODO: needs MapReduceCommand
		if(methodName.equals("mapReduce")) {
			int len=checkArgLength("mapReduce",args,1,1);
			if(len==1){
				return toCFML(coll.mapReduce(
					toDBObject(args[0])
				));
			}
		}*/

		//mapReduce
		if(methodName.equals("mapReduce")) {
			int len=checkArgLength("mapReduce",args,1,1);
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


		String functionNames = "aggregate,dataSize,distinct,drop,dropIndex,dropIndexes,createIndex,stats,getIndexes,find,findOne,findAndModify," +
		"group,insert,mapReduce,remove,rename,save,storageSize,totalIndexSize,update";

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
