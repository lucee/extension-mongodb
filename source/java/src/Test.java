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
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Set;

import org.lucee.mongodb.DBImpl;
import org.lucee.mongodb.util.aprint;
import org.lucee.mongodb.util.print;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;


public class Test {
	public static void main(String[] args) throws UnknownHostException {
		
		
		MongoClient client = new MongoClient("localhost", 27017);
		
		// databases
		System.out.println("--- databases ---");
		for (String s : client.getDatabaseNames()) {
			System.out.println(s);
		}
		
		DB db = client.getDB("test");

		System.out.println(db.collectionExists("test"));
		System.out.println(db.collectionExists("Test"));
		
		// collection names
		System.out.println("--- collection names ---");
		
		Set<String> colls = db.getCollectionNames();
		for (String s : colls) {
			System.out.println(s);
		}

		aprint.e(db.collectionExists("test"));
		aprint.e(db.collectionExists("TEST"));
		aprint.e(db.collectionExists("Test"));
		aprint.e(db.collectionExists("ttest"));
		aprint.e(db.getCollection("test").count());
		aprint.e(db.getCollection("Test").count());
		aprint.e(db.getCollectionFromString("test").count());
		aprint.e(db.getCollectionFromString("Test").count());
		
		if(true) return;
		
		DBCollection coll = db.getCollection("test");
		DBObject myDoc = coll.findOne();
		System.out.println(myDoc);
		
		BasicDBObject doc = new BasicDBObject("name", "MongoDB").
        append("type", "database").
        append("count", 1)
       .append("info", new BasicDBObject("x", 203).append("y", 102));
		coll.insert(doc);
		
		DBCursor cur = coll.find();
		while(cur.hasNext()){
			System.out.println(cur.next());
		}
		
		coll = db.getCollection("TEST");
		myDoc = coll.findOne();
		System.out.println(myDoc);
		
		doc = new BasicDBObject("name", "MongoDB").
        append("type", "database").
        append("count", 1)
       .append("info", new BasicDBObject("x", 203).append("y", 102));
		coll.insert(doc);
		
		
	}
}
