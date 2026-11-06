component extends="org.lucee.cfml.test.LuceeTestCase" labels="mongodb" {
	include "../properties.cfm";

	// Keep unauthenticated local MongoDB supported, but skip unconfigured environments.

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
		// Read credentials from environment variables (empty user/password are valid).
		var mongoDB={};
		if(!isNull(server.system.environment.MONGODB_SERVER) && !isNull(server.system.environment.MONGODB_PORT) && !isNull(server.system.environment.MONGODB_USERNAME) && !isNull(server.system.environment.MONGODB_PASSWORD)) {
			mongoDB.server=server.system.environment.MONGODB_SERVER;
			mongoDB.port=server.system.environment.MONGODB_PORT;
			mongoDB.user=server.system.environment.MONGODB_USERNAME;
			mongoDB.pass=server.system.environment.MONGODB_PASSWORD;
		}
		// Fall back to system properties for local runs.
		else if(!isNull(server.system.properties.MONGODB_SERVER) && !isNull(server.system.properties.MONGODB_PORT) && !isNull(server.system.properties.MONGODB_USERNAME) && !isNull(server.system.properties.MONGODB_PASSWORD)) {
			mongoDB.server=server.system.properties.MONGODB_SERVER;
			mongoDB.port=server.system.properties.MONGODB_PORT;
			mongoDB.user=server.system.properties.MONGODB_USERNAME;
			mongoDB.pass=server.system.properties.MONGODB_PASSWORD;
		}
		return mongoDB;
	}

	public function setUp() {
		if (isNotSupported()) return;
		var uri = "mongodb://#variables.mongoDB.server#:#variables.mongoDB.port#";
		if (!isEmpty(variables.mongoDB.user) && !isEmpty(variables.mongoDB.pass))
			uri = "mongodb://#variables.mongoDB.user#:#variables.mongoDB.pass#@#variables.mongoDB.server#:#variables.mongoDB.port#";
		variables.db = MongoDBConnect("test", uri);
		variables.collectionName = "coverage_" & lcase(replace(createUUID(), "-", "", "all"));
		variables.coll = variables.db.getCollection(variables.collectionName);
	}

	public function tearDown() {
		if (!isNull(variables.coll)) variables.coll.drop();
	}

	public void function testSaveInsertAndReplace() skip="isNotSupported" {
		var result = variables.coll.save({"_id":1, "name":"first", "obsolete":true});
		assertTrue(result.isOk());
		assertFalse(result.isUpdateOfExisting());
		assertEquals(1, result.getN());
		result = variables.coll.save({"_id":1, "name":"replacement"});
		assertTrue(result.isUpdateOfExisting());
		assertEquals(1, variables.coll.count());
		var doc = variables.coll.findOne({"_id":1});
		assertEquals("replacement", doc.name);
		assertFalse(structKeyExists(doc, "obsolete"));
		variables.coll.save({"name":"generated id"});
		assertEquals(2, variables.coll.count());
		assertEquals(24, len(variables.coll.findOne({"name":"generated id"})._id.toString()));
	}

	public void function testUpdateUpserts() skip="isNotSupported" {
		variables.coll.update({"_id":1}, {"$set":{"name":"operator"}}, true);
		assertEquals("operator", variables.coll.findOne({"_id":1}).name);
		variables.coll.update({"_id":2}, {"_id":2, "name":"replacement"}, true);
		assertEquals("replacement", variables.coll.findOne({"_id":2}).name);
		assertEquals(2, variables.coll.count());
		variables.coll.update({"_id":3}, {"$set":{"name":"not inserted"}}, false);
		assertTrue(isNull(variables.coll.findOne({"_id":3})));
	}

	public void function testFindAndModifyReturnNewAndUpsert() skip="isNotSupported" {
		// Seven-argument form: filter, projection, sort, remove, update, returnNew, upsert.
		var doc = variables.coll.findAndModify({"_id":1}, {}, {}, false,
			{"$set":{"value":10}}, true, true);
		assertEquals(10, doc.value);
		doc = variables.coll.findAndModify({"_id":1}, {"value":1, "_id":0}, {}, false,
			{"$inc":{"value":5}}, true, false);
		assertEquals(15, doc.value);
		assertFalse(structKeyExists(doc, "_id"));
		// Replacement branch must also honor returnNew and upsert.
		doc = variables.coll.findAndModify({"_id":2}, {}, {}, false,
			{"_id":2, "value":20}, true, true);
		assertEquals(20, doc.value);
		assertEquals(2, variables.coll.count());
	}

	public void function testFindAndModifyRemoveWithSortAndProjection() skip="isNotSupported" {
		variables.coll.insert([{"_id":1, "value":10}, {"_id":2, "value":20}]);
		var doc = variables.coll.findAndModify({}, {"value":1, "_id":0}, {"value":-1},
			true, {}, false, false);
		assertEquals(20, doc.value);
		assertFalse(structKeyExists(doc, "_id"));
		assertEquals(1, variables.coll.count());
		assertTrue(isNull(variables.coll.findOne({"_id":2})));
	}

	public void function testCursorSkipAndBoundedArray() skip="isNotSupported" {
		variables.coll.insert([{"_id":1}, {"_id":2}, {"_id":3}, {"_id":4}, {"_id":5}]);
		var cursor = variables.coll.find().sort({"_id":1}).skip(2).limit(2);
		try {
			assertEquals(5, cursor.count()); // ignores skip/limit, unlike size/length/itcount
			assertEquals(2, cursor.size());
			assertEquals(2, cursor.length());
			assertEquals(2, cursor.itcount());
			var docs = cursor.toArray();
			assertEquals(3, docs[1]._id);
			assertEquals(4, docs[2]._id);
			docs = variables.coll.find().sort({"_id":1}).skip(1).toArray(2);
			assertEquals(2, arrayLen(docs));
			assertEquals(2, docs[1]._id);
			assertEquals(3, docs[2]._id);
		} finally {
			cursor.close();
		}
	}

	public void function testCursorHintsAndExplain() skip="isNotSupported" {
		variables.coll.insert([{"_id":1, "value":10}, {"_id":2, "value":20}]);
		variables.coll.createIndex({"value":1}, {"name":"value_index"});
		var docs = variables.coll.find({"value":{"$gte":10}}).hint("value_index").toArray();
		assertEquals(2, arrayLen(docs));
		docs = variables.coll.find({"value":20}).hint({"value":1}).toArray();
		assertEquals(2, docs[1]._id);
		var plan = variables.coll.find({"value":20}).hint("value_index").explain();
		assertTrue(isStruct(plan.queryPlanner));
		assertTrue(findNoCase("value_index", serializeJSON(plan.queryPlanner)) > 0);
	}

	public void function testDateBooleanAndObjectIdRoundTrip() skip="isNotSupported" {
		var stamp = createDateTime(2024, 1, 2, 3, 4, 5);
		var id = MongoDBID();
		variables.coll.insert({"_id":id, "stamp":stamp, "enabled":true, "disabled":false});
		var doc = variables.coll.findOne({"_id":MongoDBID(id.toString())});
		assertEquals(id.toString(), doc._id.toString());
		assertTrue(isDate(doc.stamp));
		assertEquals(0, dateCompare(stamp, doc.stamp, "s"));
		assertTrue(doc.enabled);
		assertFalse(doc.disabled);
		// Verify actual BSON types on the server, not just CFML coercion on read.
		assertEquals(1, variables.coll.count({"stamp":{"$type":"date"},
			"enabled":{"$type":"bool"}, "disabled":{"$type":"bool"}, "_id":{"$type":"objectId"}}));
	}

	public void function testCollectionLifecycleAndCommands() skip="isNotSupported" {
		assertFalse(variables.db.collectionExists(variables.collectionName));
		variables.db.createCollection(variables.collectionName);
		assertTrue(variables.db.collectionExists(variables.collectionName));
		variables.db.getCollectionFromString(variables.collectionName).insert({"_id":1});
		assertEquals(1, variables.coll.count());
		assertTrue(arrayFindNoCase(variables.db.getCollectionNames(), variables.collectionName) > 0);
		var result = variables.db.command({"ping":1});
		assertTrue(result.ok());
		assertTrue(isNull(result.getErrorMessage()));
		assertTrue(isNull(result.getException()));
		result.throwOnError();
		assertTrue(variables.db.getStats().collections >= 1);
		variables.coll.drop();
		assertFalse(variables.db.collectionExists(variables.collectionName));
	}
}
