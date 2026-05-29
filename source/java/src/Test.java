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
import org.bson.Document;
import org.lucee.mongodb.util.aprint;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;

public class Test {
	public static void main(String[] args) {
		MongoClient client = MongoClients.create("mongodb://localhost:27017");

		System.out.println("--- databases ---");
		for (String s : client.listDatabaseNames()) {
			System.out.println(s);
		}

		MongoDatabase db = client.getDatabase("test");

		System.out.println("--- collection names ---");
		for (String s : db.listCollectionNames()) {
			System.out.println(s);
		}

		MongoCollection<Document> coll = db.getCollection("test");
		aprint.e(coll.countDocuments());

		Document myDoc = coll.find().first();
		System.out.println(myDoc);

		Document doc = new Document("name", "MongoDB")
			.append("type", "database")
			.append("count", 1)
			.append("info", new Document("x", 203).append("y", 102));
		coll.insertOne(doc);

		try (MongoCursor<Document> cur = coll.find().iterator()) {
			while (cur.hasNext()) {
				System.out.println(cur.next());
			}
		}
	}
}
