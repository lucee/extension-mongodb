<!--- 
 *
 * Copyright (c) 2015, Lucee Assosication Switzerland. All rights reserved.
 * Copyright (c) 2014, the Railo Company LLC. All rights reserved.
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
 ---><cfscript>
component extends="org.lucee.cfml.test.LuceeTestCase" labels="mongodb"	{

	include "../properties.cfm";

	// skip closure
	function isNotSupported() {
		variables.mongodb=getCredentials();
		if(!isNull(variables.mongodb.server)) {
			variables.supported=true;
		}
		else 
			variables.supported=false;

		return !variables.supported;
	}

	private struct function getCredentials() {
		// getting the credetials from the enviroment variables
		var mongoDB={};
		if(!isNull(server.system.environment.MONGODB_SERVER) && !isNull(server.system.environment.MONGODB_PORT) && !isNull(server.system.environment.MONGODB_USERNAME) && !isNull(server.system.environment.MONGODB_PASSWORD)) {
			mongoDB.server=server.system.environment.MONGODB_SERVER;
			mongoDB.port=server.system.environment.MONGODB_PORT;
			mongoDB.user=server.system.environment.MONGODB_USERNAME;
			mongoDB.pass=server.system.environment.MONGODB_PASSWORD;
		}
		// getting the credetials from the system variables
		else if(!isNull(server.system.properties.MONGODB_SERVER) && !isNull(server.system.properties.MONGODB_PORT) && !isNull(server.system.properties.MONGODB_USERNAME) && !isNull(server.system.properties.MONGODB_PASSWORD)) {
			mongoDB.server=server.system.properties.MONGODB_SERVER;
			mongoDB.port=server.system.properties.MONGODB_PORT;
			mongoDB.user=server.system.properties.MONGODB_USERNAME;
			mongoDB.pass=server.system.properties.MONGODB_PASSWORD;
		}
		return mongoDB;
	}

	private any function resetTestCollection() {
		var coll = db.getCollection("test");
		var docs = [
			 {"_id":1, "grp":1, "name":"One"}
			,{"_id":2, "grp":1, "name":"Two"}
			,{"_id":3, "grp":1, "name":"Three"}
			,{"_id":4, "grp":2, "name":"Four"}
			,{"_id":5, "grp":2, "name":"Five"}
		];

		coll.drop();
		coll.insert(docs);

		return coll;		
	}
	
	// public function setUp(){}

	public function beforeTests() {
		var uri = "mongodb://#variables.mongoDB.server#:#variables.mongoDB.port#";
		if (!isempty(variables.mongoDB.user) && !isEmpty(variables.mongoDB.pass))
			uri = "mongodb://#variables.mongoDB.user#:#variables.mongoDB.pass#@#variables.mongoDB.server#:#variables.mongoDB.port#";

		db = MongoDBConnect("test",uri);
		variables.mongoMajorVersion = val(listFirst(db.runCommand({"buildInfo": 1}).version, "."));
	}

	private boolean function isTimeSeriesNotSupported() {
		return isNotSupported() || variables.mongoMajorVersion < 5;
	}
	
	//public function afterTests(){}
	

	public void function testConnectByArgs() skip="isNotSupported" {
		var mongo = MongoDBConnect("test",variables.mongoDB.server,variables.mongoDB.port);
		assertEquals("test",mongo.getName());
	}

	public void function testConnectByURI() {
		var uri = "mongodb://#variables.mongoDB.server#:#variables.mongoDB.port#";
		if (!isempty(variables.mongoDB.user) && !isEmpty(variables.mongoDB.pass))
			uri = "mongodb://#variables.mongoDB.user#:#variables.mongoDB.pass#@#variables.mongoDB.server#:#variables.mongoDB.port#";

		var mongo = MongoDBConnect("test",uri);
		assertEquals("test",mongo.getName());
	}

	// skip until authenticate is implemented
	public void function testAuthenticate() skip="true" {
		var mongo = MongoDBConnect("test",variables.mongoDB.server,variables.mongoDB.port);
		mongo.authenticate(variables.mongoDB.user,variables.mongoDB.pass);
	}

	public void function testIdConversion() skip="isNotSupported" {
		var content = {'name':'Susi'};
		db.getCollection("test").insert(content);
		
		//Get by Name
		var id = db['test'].findOne({'name':'Susi'});
		assertEquals("Susi",id.name)
		
		//Get by Id : fails
		var byid = db['test'].findOne({'_id':id});
		assertEquals(isNull(byid),true)
	}

	public void function testMongoDBID() skip="isNotSupported" {
		var id = MongoDBID();
		$assert.key(id,"date");
		$assert.key(id,"timestamp");
		$assert.key(id,"id");

		var dateSeed = now().add("d",-1)
		id = MongoDBID(dateSeed);
		$assert.isEqual(dateSeed,id.getDate());
	
		var idSeed = "56be2538ddd75f08acde1e46";
		id = MongoDBID(idSeed);
		$assert.isEqual(idSeed,id.toString());
	}

	public void function testInsertAndFind() skip="isNotSupported" {
		var coll = db.getCollection("test");
		var docs = [
			 {"_id":1, "name":"One"}
			,{"_id":2, "name":"Two"}
			,{"_id":3, "name":"Three"}
		]

		var moreDocs = [
			 {"_id":4, "name":"Four"}
			,{"_id":5, "name":"Five"}
			,{"_id":6, "name":"Six"}
		]

		// clear out collection, verify empty
		coll.drop();
		$assert.null(coll.findOne());

		// insert docs, verify count
		coll.insert(docs);
		$assert.isEqual( 3, coll.count() );

		coll.insertMany(moreDocs);

		// find a doc, test cursor methods
		var docsFound = coll.find({"name":"One"});
		$assert.isTrue( docsFound.hasNext() );
		$assert.isEqual( 1, docsFound.size() );
		$assert.typeOf( "struct", docsFound.next() );
		$assert.isFalse( docsFound.hasNext() );

		// find with limit and sort
		docsFound = coll.find().sort({"_id":-1}).limit(2);
		$assert.isEqual( 2, docsFound.size() );
		$assert.isEqual( 6, docsFound.count() );
		$assert.isEqual( "Six", docsFound.next().name );
	}

	public void function testCursorBatchSize() skip="isNotSupported" {
		var coll = resetTestCollection(); // 5 documents

		// batchSize() controls how many documents are fetched per server round-trip;
		// it does NOT limit total results — all matching documents are still returned.
		var docs = coll.find().batchSize(2).toArray();
		$assert.isEqual(5, docs.len(),
			"batchSize(2) should not limit total results from a 5-document collection");

		// verify manual iteration also returns all documents
		var cursor = coll.find().batchSize(2);
		var count = 0;
		while (cursor.hasNext()) {
			cursor.next();
			count++;
		}
		$assert.isEqual(5, count,
			"manual iteration with batchSize(2) should visit all 5 documents");

		// batchSize() must be chainable with other cursor methods
		docs = coll.find().batchSize(2).sort(["_id": 1]).limit(3).toArray();
		$assert.isEqual(3, docs.len(),
			"batchSize + sort + limit should return only the limited count");
		$assert.isEqual(1, docs[1]._id); // sort order preserved
		$assert.isEqual(3, docs[3]._id);
	}

	public void function testAggregateBatchSize() skip="isNotSupported" {
		var coll = resetTestCollection(); // 5 documents

		// batchSize can be passed in the options struct to aggregate()
		var results = coll.aggregate(
			[{"$sort": {"_id": 1}}],
			{"cursor": {"batchSize": 2}}
		);
		var count = 0;
		while (results.hasNext()) { results.next(); count++; }
		$assert.isEqual(5, count,
			"aggregate with batchSize option should still return all documents");

		// batchSize can also be called as a method on the returned cursor
		results = coll.aggregate([{"$sort": {"_id": 1}}]).batchSize(2);
		count = 0;
		while (results.hasNext()) { results.next(); count++; }
		$assert.isEqual(5, count,
			"aggregate cursor batchSize() method should still return all documents");
	}

	public void function testCursorNumSeen() skip="isNotSupported" {
		var coll = resetTestCollection(); // 5 documents

		var cursor = coll.find().sort(["_id": 1]);

		// before any iteration, numSeen() must be 0
		$assert.isEqual(0, cursor.numSeen());

		// curr() before first next() must be null
		$assert.isTrue(isNull(cursor.curr()), "curr() should be null before the first next() call");

		// iterate two documents
		cursor.next();
		$assert.isEqual(1, cursor.numSeen());
		cursor.next();
		$assert.isEqual(2, cursor.numSeen());

		// curr() must return the last document returned by next()
		var last = cursor.curr();
		$assert.typeOf("struct", last);
		$assert.isEqual(2, last._id); // second doc (_id=2) was the last next() call

		// numSeen() keeps counting through full iteration
		while (cursor.hasNext()) { cursor.next(); }
		$assert.isEqual(5, cursor.numSeen());
	}

	public void function testCursorGetQuery() skip="isNotSupported" {
		var coll = resetTestCollection();

		// getQuery() returns the filter document passed to find()
		var filter  = {"grp": 1};
		var cursor  = coll.find(filter);
		var query   = cursor.getQuery();
		$assert.typeOf("struct", query);
		$assert.isEqual(1, query.grp);

		// find() with no filter returns an empty struct
		var emptyCursor = coll.find();
		$assert.typeOf("struct", emptyCursor.getQuery());

		// getCollection() returns the collection the cursor was opened against
		var collRef = cursor.getCollection();
		$assert.isTrue(!isNull(collRef), "getCollection() should return a non-null collection reference");
		$assert.isEqual(3, collRef.count({"grp": 1}));
	}

	public void function testProjection() skip="isNotSupported" {
		var coll = resetTestCollection();

		// find() with projection — include name only, explicitly exclude _id
		var docs = coll.find({}, {"name": 1, "_id": 0}).toArray();
		$assert.isEqual(5, docs.len());
		$assert.isTrue(structKeyExists(docs[1], "name"));
		$assert.isFalse(structKeyExists(docs[1], "grp"),  "grp should be excluded by projection");
		$assert.isFalse(structKeyExists(docs[1], "_id"),  "_id should be excluded by projection");

		// findOne() with projection
		var doc = coll.findOne({"_id": 1}, {"name": 1, "_id": 0});
		$assert.isEqual("One", doc.name);
		$assert.isFalse(structKeyExists(doc, "grp"), "grp should be excluded by projection");

		// findOne() 3-arg form: filter + projection + sort
		// highest _id in grp=1 is _id=3 (name="Three")
		doc = coll.findOne({"grp": 1}, {"name": 1, "_id": 0}, {"_id": -1});
		$assert.isEqual("Three", doc.name);
	}

	public void function testUpdate() skip="isNotSupported" {
		var coll = resetTestCollection();

		// single update with criteria
		coll.update({"_id":1}, {"$set":{"updated":true}});
		$assert.isTrue( coll.findOne({"_id":1}).updated );

		// reset data
		coll = resetTestCollection();

		// single update, no criteria 
		coll.update({},{"$set":{"updated":true}});
		$assert.isEqual(1, coll.find({"updated":true}).size());

		// reset data
		coll = resetTestCollection();

		// multi update, no criteria 
		coll.update({},{"$set":{"updated":true}},false,true);
		$assert.isEqual(5, coll.find({"updated":true}).size());
	
		// find and modify
		var doc = coll.findAndModify({"_id":1},{"$set":{"modified":true}});
		$assert.isEqual(1, coll.find({"modified":true}).size());
		$assert.isEqual(1, doc._id);
	}

	public void function testRemove() skip="isNotSupported" {
		var coll = resetTestCollection();

		// remove 1 doc
		coll.remove({"_id":1});
		$assert.isEqual( 4, coll.count() );

		// find and remove 1 doc
		var doc = coll.findAndRemove({"_id":2});
		$assert.isEqual( 2, doc._id );
		$assert.isEqual( 3, coll.count() );

		// remove all docs
		coll.remove({});
		$assert.isEqual( 0, coll.count() );
	}

	public void function testWriteResult() skip="isNotSupported" {
		var coll = resetTestCollection();

		// Successful update — matched and modified
		var result = coll.update({"_id": 1}, {"$set": {"updated": true}});
		$assert.isEqual(1, result.getN());
		$assert.isTrue(result.isUpdateOfExisting());
		$assert.isTrue(result.isOk());

		// Update with no matching document
		result = coll.update({"_id": 99}, {"$set": {"updated": true}});
		$assert.isEqual(0, result.getN());
		$assert.isFalse(result.isUpdateOfExisting());

		// Remove returns n = number of documents deleted
		result = coll.remove({"_id": 2});
		$assert.isEqual(1, result.getN());
		$assert.isTrue(result.isOk());
	}

	public void function testReplacement() skip="isNotSupported" {
		var coll = resetTestCollection();

		// Pass a plain document (no $ operators) — must route to replaceOne(), not updateOne()
		coll.update({"_id": 1}, {"_id": 1, "name": "One Replaced", "replaced": true});
		var doc = coll.findOne({"_id": 1});
		$assert.isEqual("One Replaced", doc.name);
		$assert.isTrue(structKeyExists(doc, "replaced") && doc.replaced);
		// "grp" was not in the replacement document so it should be gone entirely
		$assert.isFalse(structKeyExists(doc, "grp"),
			"full replacement should remove fields absent from the replacement document");

		// findAndModify() with a replacement document
		var before = coll.findAndModify({"_id": 2}, {"_id": 2, "name": "Two Replaced"});
		$assert.isEqual("Two", before.name); // returnNew defaults to false — pre-update doc returned
		$assert.isEqual("Two Replaced", coll.findOne({"_id": 2}).name);
	}

	public void function testBulkWrite() skip="isNotSupported" {
		var coll = resetTestCollection();

		coll.setWriteConcern("ACKNOWLEDGED");

		// insert 3 docs
		coll.bulkWrite([
			 {"operation":"insert", "document":{"_id":6, "grp":3, "name":"Six"}}
			,{"operation":"insert", "document":{"_id":7, "grp":3, "name":"Seven"}}
			,{"operation":"insert", "document":{"_id":8, "grp":3, "name":"Eight"}}
		]);
		$assert.isEqual( 8, coll.count() );

		// update 2 docs
		coll.bulkWrite([
			 {"operation":"updateOne", "query":{"_id":6}, "update":{"$set":{"name":"Six Edited"}}}
			,{"operation":"updateOne", "query":{"_id":7}, "update":{"$set":{"name":"Seven Edited"}}}
		]);
		$assert.isEqual( "Six Edited", coll.findOne({"_id":6}).name );
		$assert.isEqual( "Seven Edited", coll.findOne({"_id":7}).name );

		var bwResult = coll.bulkWrite([
			 {"operation":"update", "query":{"grp":3}, "update":{"$set":{"updated":true}}}
		]);

		$assert.isEqual( 3, bwResult.nModified );
		$assert.isEqual( 3, coll.count({"updated":true}) );

		// update one & remove a couple docs
		var bwResult = coll.bulkWrite([
			 {"operation":"updateOne", "query":{"_id":6}, "update":{"$set":{"name":"Six"}}}
			,{"operation":"removeOne", "query":{"_id":7}}
			,{"operation":"removeOne", "query":{"_id":8}}
		],{"bypassDocumentValidation":true});
		$assert.isEqual( 1, bwResult.nModified );
		$assert.isEqual( 2, bwResult.nRemoved );
		$assert.isEqual( "Six", coll.findOne({"_id":6}).name );
		$assert.isEqual( 6, coll.count() );
	}

	public void function testNestedDocuments() skip="isNotSupported" {
		var coll = db.getCollection("test_nested");
		coll.drop();

		coll.insert({
			"_id"    : 1,
			"name"   : "test",
			"address": {"street": "123 Main St", "city": "Anytown"},
			"tags"   : ["mongodb", "cfml", "lucee"],
			"scores" : [
				{"subject": "math",    "score": 95},
				{"subject": "english", "score": 87}
			]
		});

		var doc = coll.findOne({"_id": 1});

		// Nested document must come back as a case-insensitive Lucee struct
		$assert.typeOf("struct", doc.address);
		$assert.isEqual("123 Main St", doc.address.STREET); // uppercase key access
		$assert.isEqual("Anytown",     doc.address.City);   // mixed-case key access

		// Top-level array must be a native Lucee array
		$assert.typeOf("array", doc.tags);
		$assert.lengthOf(doc.tags, 3);
		$assert.isEqual("cfml", doc.tags[2]);

		// Array of nested documents
		$assert.typeOf("array",  doc.scores);
		$assert.typeOf("struct", doc.scores[1]);
		$assert.isEqual(95,        doc.scores[1].score);
		$assert.isEqual("english", doc.scores[2].SUBJECT); // uppercase key access on nested struct

		coll.drop();
	}

	public void function testAggregateMerge() skip="isNotSupported" {
		var source = resetTestCollection();               // 5 docs in "test"
		var target = db.getCollection("test_merge_out");
		target.drop();

		// $merge writes matching documents to the target collection and
		// produces no output — the returned cursor must be empty
		var result = source.aggregate([
			{"$match": {"grp": 1}},
			{"$merge": {"into": "test_merge_out"}}
		]);

		// cursor must report no documents (write stage, nothing returned)
		$assert.isFalse(result.hasNext(),
			"$merge pipeline should return an empty cursor");
		$assert.lengthOf(result.results(), 0,
			"$merge pipeline results() should return an empty array");

		// the write must actually have happened: grp=1 has 3 documents
		$assert.isEqual(3, target.count(),
			"$merge should have written 3 documents to the target collection");

		// $out behaves the same way — replaces the entire target collection
		var out = db.getCollection("test_out");
		out.drop();
		result = source.aggregate([
			{"$match": {"grp": 2}},
			{"$out": "test_out"}
		]);
		$assert.isFalse(result.hasNext(), "$out pipeline should return an empty cursor");
		$assert.isEqual(2, out.count(),
			"$out should have written 2 documents to the output collection");

		// cleanup
		target.drop();
		out.drop();
	}

	public void function testAggregateResults() skip="isNotSupported" {
		var coll = resetTestCollection();

		// aggregate with N... structs as arguments returns AggregationResult
		var results = coll.aggregate({"$group":{"_id":"$grp", "vals":{"$push":"$name"}}});
		$assert.typeOf( "array", results.results() );
		$assert.lengthOf( results.results(), 2 );

		// aggregate with array of pipeline operations as single argument returns AggregationResult
		var results = coll.aggregate([{"$group":{"_id":"$grp", "vals":{"$push":"$name"}}}]);
		$assert.typeOf( "array", results.results() );
		$assert.lengthOf( results.results(), 2 );
	}

	public void function testAggregateCursor() skip="isNotSupported" {
		var coll = resetTestCollection();

		// aggregate with array of pipeline operations as first argument with struct options as second argument returns Cursor
		var results = coll.aggregate([{"$group":{"_id":"$grp", "vals":{"$push":"$name"}}},{"$sort":{"_id":1}}],{});
		$assert.isTrue( results.hasNext() );
		$assert.lengthOf( results.next().vals, 3 );
	}

	public void function testWriteConcern() skip="isNotSupported" {
		var coll = resetTestCollection();

		coll.setWriteConcern("UNACKNOWLEDGED");

		var wc = coll.getWriteConcern();
		$assert.isFalse(wc.isAcknowledged());
	}

	public void function testIndexing() skip="isNotSupported" {
		var coll = resetTestCollection();

		// get indexes
		var idx = coll.getIndexes();
		$assert.typeOf("array",idx);

		// create indexes
		coll.createIndex("grp");
		coll.createIndex({"name":1},{"name":"name"});
		idx = coll.getIndexes();
		$assert.lengthOf(idx, 3);

		// index size
		var idxSize = coll.totalIndexSize();
		$assert.typeOf("numeric", idxSize);

		// drop index by name
		coll.dropIndex("name");
		idx = coll.getIndexes();
		$assert.lengthOf(idx,2); // only _id + grp indexes should remain after dropIndex('name');

		// drop all indexes
		coll.dropIndexes();
		idx = coll.getIndexes();
		$assert.lengthOf(idx,1); // only _id index should remain after dropIndexes();
	}

	public void function testIndexOptions() skip="isNotSupported" {
		var coll = resetTestCollection();

		// helper: find a single index by name in the listing
		var findIdx = function(name) {
			var all = coll.getIndexes();
			var match = all.filter(function(i) { return structKeyExists(i, "name") && i.name == name; });
			return match;
		};

		// --- unique ---
		coll.createIndex({"name": 1}, {"name": "idx_unique_name", "unique": true});
		var match = findIdx("idx_unique_name");
		$assert.lengthOf(match, 1);
		$assert.isTrue(structKeyExists(match[1], "unique") && match[1].unique,
			"unique index should be flagged in index definition");

		// the constraint must actually be enforced (name "One" already in the collection)
		var threw = false;
		try {
			coll.insert({"_id": 99, "grp": 9, "name": "One"});
		} catch(any e) {
			threw = true;
		}
		$assert.isTrue(threw, "unique index should reject a duplicate key");
		coll.dropIndex("idx_unique_name");

		// --- sparse ---
		coll.createIndex({"optional_field": 1}, {"name": "idx_sparse", "sparse": true});
		match = findIdx("idx_sparse");
		$assert.lengthOf(match, 1);
		$assert.isTrue(structKeyExists(match[1], "sparse") && match[1].sparse,
			"sparse index should be flagged in index definition");
		coll.dropIndex("idx_sparse");

		// --- partialFilterExpression ---
		coll.createIndex({"grp": 1}, {
			"name": "idx_partial",
			"partialFilterExpression": {"grp": {"$gt": 1}}
		});
		match = findIdx("idx_partial");
		$assert.lengthOf(match, 1);
		$assert.isTrue(structKeyExists(match[1], "partialFilterExpression"),
			"index definition should contain partialFilterExpression");
		coll.dropIndex("idx_partial");

		// --- TTL (expireAfterSeconds) ---
		coll.createIndex({"name": 1}, {"name": "idx_ttl", "expireAfterSeconds": 3600});
		match = findIdx("idx_ttl");
		$assert.lengthOf(match, 1);
		$assert.isTrue(structKeyExists(match[1], "expireAfterSeconds"),
			"TTL index should carry expireAfterSeconds in its definition");
		$assert.isEqual(3600, match[1].expireAfterSeconds);
		coll.dropIndex("idx_ttl");

		// --- text index with weights ---
		coll.createIndex({"name": "text"}, {
			"name": "idx_text",
			"weights": {"name": 10}
		});
		match = findIdx("idx_text");
		$assert.lengthOf(match, 1);
		$assert.isTrue(structKeyExists(match[1], "weights"),
			"text index with weights should expose weights in its definition");
		coll.dropIndex("idx_text");

		// --- drop by specification document (not by name string) ---
		// Use a single-field key to avoid CFML struct key-ordering ambiguity:
		// compound spec docs may serialize in a different order than MongoDB stored them.
		coll.createIndex({"grp": 1}); // auto-named by MongoDB
		$assert.lengthOf(coll.getIndexes(), 2); // _id + grp
		coll.dropIndex({"grp": 1}); // spec doc instead of name string
		$assert.lengthOf(coll.getIndexes(), 1); // only _id should remain
	}

	public void function testKeyOrderPreservation() skip="isNotSupported" {
		var coll = resetTestCollection();

		// --- createIndex: ordered struct key order must reach MongoDB ---
		// MongoDB auto-names an index from the key spec in the exact order it
		// receives the fields, so the generated name is a reliable signal.
		// Use reverse-alphabetical order (b before a) to distinguish a preserved
		// ordering ("b_1_a_1") from an accidentally alphabetical one ("a_1_b_1").
		coll.createIndex(["b": 1, "a": 1]); // ordered struct — b must come first
		var idx = coll.getIndexes();
		var match = idx.filter(function(i) {
			return structKeyExists(i, "name") && (i.name == "b_1_a_1" || i.name == "a_1_b_1");
		});
		$assert.lengthOf(match, 1);
		$assert.isEqual("b_1_a_1", match[1].name,
			"ordered struct must preserve key order: b should precede a in the index spec");
		coll.dropIndex("b_1_a_1");

		// --- sort: ordered struct key order determines sort priority ---
		// sort(["grp":1, "name":1]) means grp is the primary sort key.
		// grp=1 docs in name-asc order:  One, Three, Two
		// grp=2 docs in name-asc order:  Five, Four
		// If key order were swapped, name would become the primary key and the
		// first result would be "Five" (alphabetically first), not "One".
		var docs = coll.find().sort(["grp": 1, "name": 1]).toArray();
		$assert.isEqual(5, docs.len());
		$assert.isEqual("One",   docs[1].name);
		$assert.isEqual("Three", docs[2].name);
		$assert.isEqual("Two",   docs[3].name);
		$assert.isEqual("Five",  docs[4].name);
		$assert.isEqual("Four",  docs[5].name);
	}

	public void function testCollectionUtils() skip="isNotSupported" {
		var coll = resetTestCollection();

		$assert.typeOf("struct", coll.stats());
		$assert.typeOf("numeric", coll.dataSize());		
		$assert.typeOf("numeric", coll.storageSize());		
	}

	public void function testCreateCollectionWithValidator() skip="isNotSupported" {
		// JSON Schema validator — rejects documents that violate the schema
		var coll = db.getCollection("test_validated");
		coll.drop();

		db.createCollection("test_validated", {
			"validator": {
				"$jsonSchema": {
					"bsonType": "object",
					"required": ["name", "age"],
					"properties": {
						"name": {"bsonType": "string"},
						"age":  {"bsonType": "number", "minimum": 0}
					}
				}
			},
			"validationLevel":  "strict",
			"validationAction": "error"
		});
		// Note: use "number" not "int" — CFML represents all numerics as double,
		// so values like 30 arrive at MongoDB as 30.0 (BSON double).
		// "bsonType: int" would reject them; "number" accepts int, long, and double.

		// valid document should insert without error
		coll.insert({"name": "Alice", "age": 30});
		$assert.isEqual(1, coll.count());

		// invalid document (missing required "age") should be rejected
		var threw = false;
		try {
			coll.insert({"name": "Bob"});
		} catch(any e) {
			threw = true;
		}
		$assert.isTrue(threw, "validator should reject a document missing required field 'age'");
		$assert.isEqual(1, coll.count(), "rejected insert must not change document count");

		coll.drop();
	}

	public void function testCreateTimeSeriesCollection() skip="isTimeSeriesNotSupported" {
		var coll = db.getCollection("test_timeseries");
		coll.drop();

		db.createCollection("test_timeseries", {
			"timeseries": {
				"timeField":   "timestamp",
				"metaField":   "sensor",
				"granularity": "seconds"
			},
			"expireAfterSeconds": 86400
		});

		// verify the collection exists and can accept time series documents
		coll = db.getCollection("test_timeseries");
		coll.insert({"timestamp": now(), "sensor": "A", "temperature": 21.5});
		$assert.isEqual(1, coll.count());

		// verify the collection is listed and appears in the database
		var names = db.getCollectionNames();
		$assert.isTrue(names.findNoCase("test_timeseries") > 0,
			"time series collection should appear in getCollectionNames()");

		coll.drop();
	}

	public void function testDecimal128() skip="isNotSupported" {
		var coll = resetTestCollection();

		// Produce a Decimal128 value server-side via $convert so we don't need to
		// create a Java object from CFML. The driver returns it as org.bson.types.Decimal128.
		var results = coll.aggregate([
			{"$limit": 1},
			{"$project": {
				"_id": 0,
				"decVal": {"$convert": {"input": "9.99", "to": "decimal"}},
				"large": {"$convert": {"input": "123456789012345.67", "to": "decimal"}}
			}}
		]).results();

		$assert.isEqual(1, results.len());
		var row = results[1];

		// Decimal128 must arrive as a CFML numeric, not a raw Java object
		$assert.typeOf("numeric", row.decVal,
			"Decimal128 field should be converted to a CFML numeric");
		$assert.isEqual(9.99, row.decVal);

		$assert.typeOf("numeric", row.large,
			"large Decimal128 field should also be a CFML numeric");
		$assert.isEqual(123456789012345.67, row.large);
	}

	public void function testGroupAndDistinct() skip="isNotSupported" {
		var coll = resetTestCollection();
		$assert.isEqual(2, coll.distinct("grp").len());
	}

	public void function testCountWithFilter() skip="isNotSupported" {
		var coll = resetTestCollection();

		$assert.isEqual(5, coll.count());              // all documents
		$assert.isEqual(3, coll.count({"grp": 1}));   // grp=1 only
		$assert.isEqual(2, coll.count({"grp": 2}));   // grp=2 only
		$assert.isEqual(0, coll.count({"grp": 99}));  // no match
	}

	public void function testDistinctWithFilter() skip="isNotSupported" {
		var coll = resetTestCollection();

		// Filter down to grp > 1 — only grp=2 should appear
		var groups = coll.distinct("grp", {"grp": {"$gt": 1}});
		$assert.isEqual(1, groups.len());
		$assert.isEqual(2, groups[1]);

		// Distinct names within a specific group
		var names = coll.distinct("name", {"grp": 1});
		$assert.isEqual(3, names.len()); // One, Two, Three
	}

	public void function testSiblingDB() skip="isNotSupported" {
		var siblingName = "lucee_mongo_ext_test_sibling";

		// getSiblingDB (MongoDB shell name) and getSisterDB (Java driver name) are aliases
		var sibling = db.getSiblingDB(siblingName);
		$assert.isEqual(siblingName, sibling.getName());

		// Data written to the sibling DB must not appear in the main DB
		var mainColl    = resetTestCollection(); // 5 docs in "test" db
		var siblingColl = sibling.getCollection("test");
		siblingColl.drop();
		siblingColl.insert({"_id": 1, "name": "in sibling"});

		$assert.isEqual(5, mainColl.count());    // main db unaffected
		$assert.isEqual(1, siblingColl.count()); // sibling db has exactly its own doc

		// getSisterDB must resolve to the same database
		var sister = db.getSisterDB(siblingName);
		$assert.isEqual(1, sister.getCollection("test").count());

		// cleanup
		sibling.dropDatabase();
	}

	/**
	 * Atlas Search / Vector Search index management.
	 * skip="true" because these operations require an Atlas cluster — they will
	 * throw on local MongoDB.  Change skip to "isNotSupported" when running
	 * against an Atlas-connected test server.
	 */
	public void function testSearchIndexes() skip="true" {
		var coll = resetTestCollection();

		// --- Atlas Search index (default type = "search") ---
		var searchName = coll.createSearchIndex("test_search_idx", {
			"mappings": {"dynamic": true}
		});
		$assert.isEqual("test_search_idx", searchName,
			"createSearchIndex should return the index name");

		// --- Vector Search index ---
		var vectorName = coll.createSearchIndex("test_vector_idx", {
			"fields": [
				{
					"type":          "vector",
					"path":          "embedding",
					"numDimensions": 4,
					"similarity":    "cosine"
				}
			]
		}, "vectorSearch");
		$assert.isEqual("test_vector_idx", vectorName);

		// --- listSearchIndexes ---
		// Atlas index builds are async; in a real test you'd poll until queryable.
		// Here we just assert the call succeeds and returns an array.
		var indexes = coll.listSearchIndexes();
		$assert.typeOf("array", indexes);

		// listSearchIndexes(name) filters to a specific index
		var filtered = coll.listSearchIndexes("test_search_idx");
		$assert.typeOf("array", filtered);

		// --- updateSearchIndex ---
		coll.updateSearchIndex("test_search_idx", {
			"mappings": {"dynamic": false}
		});

		// --- createSearchIndexes (batch) ---
		var names = coll.createSearchIndexes([
			{
				"name":       "batch_search",
				"definition": {"mappings": {"dynamic": true}},
				"type":       "search"
			},
			{
				"name": "batch_vector",
				"definition": {
					"fields": [
						{"type": "vector", "path": "vec", "numDimensions": 2, "similarity": "euclidean"}
					]
				},
				"type": "vectorSearch"
			}
		]);
		$assert.typeOf("array", names);
		$assert.isEqual(2, names.len());

		// --- dropSearchIndex ---
		coll.dropSearchIndex("test_search_idx");
		coll.dropSearchIndex("test_vector_idx");
		coll.dropSearchIndex("batch_search");
		coll.dropSearchIndex("batch_vector");
	}

	public void function testRename() skip="isNotSupported" {
		var coll = resetTestCollection();
		coll.rename("test2");
		$assert.isEqual(5, db["test2"].count());

		db["test2"].drop();				
	}
}
</cfscript>