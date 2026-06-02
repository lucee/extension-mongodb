# MongoDB Extension for Lucee Server

Welcome to the Lucee Server MongoDB Integration Extension source code repository.

[Lucee](https://www.lucee.org) is an open-source (LGPL 2.1) CFML engine written in Java.
[MongoDB](https://www.mongodb.com) is a document-oriented NoSQL database.

This extension connects Lucee to MongoDB via the official MongoDB Java driver, handles
all CFML ↔ BSON type conversions, and exposes a MongoDB-shell-like API directly in CFML.

---

## Requirements

| Dependency | Version |
|---|---|
| Lucee | 5.x or 6.x |
| MongoDB Java driver (bundled) | 5.5.1 |
| MongoDB server | 4.4 or later recommended |
| Java | 11 or later |

---

## Built-in Functions

### `MongoDBConnect(dbName [, host, port])` or `MongoDBConnect(dbName, uri)`

Connects to MongoDB and returns a database handle. Connections are pooled — repeated
calls with the same host/port or URI return a cached `MongoClient` rather than opening
a new connection pool.

```cfml
// host + port
db = MongoDBConnect("mydb", "localhost", 27017);

// MongoDB URI (recommended — supports auth, replica sets, Atlas, etc.)
db = MongoDBConnect("mydb", "mongodb://localhost:27017");
db = MongoDBConnect("mydb", "mongodb://user:pwd@host1,host2/mydb?replicaSet=rs0");
```

### `MongoDBID([initArg])`

Creates a MongoDB `ObjectId`. Called with no arguments it generates a new ID; pass a
hex string or a date to create a deterministic one.

```cfml
id  = MongoDBID();                          // new random ObjectId
id  = MongoDBID("56be2538ddd75f08acde1e46"); // from hex string
id  = MongoDBID(now());                      // from date (for range queries)

writeOutput(id.toString());   // hex string
writeOutput(id.getDate());    // java.util.Date of the embedded timestamp
```

---

## Basic Usage

```cfml
db = MongoDBConnect("mydb", "mongodb://localhost:27017");

// access a collection via struct notation
coll = db["myCollection"];

// or via method
coll = db.getCollection("myCollection");

// insert
coll.insert({"name": "Alice", "score": 42});
coll.insert([
    {"name": "Bob",   "score": 17},
    {"name": "Carol", "score": 99}
]);

// find
var doc = coll.findOne({"name": "Alice"});
writeOutput(doc.name);   // case-insensitive key access

// find with filter, projection, sort, limit
var cursor = coll.find({"score": {"$gt": 10}}, {"name": 1, "_id": 0})
                 .sort(["score": -1])
                 .limit(10);
while (cursor.hasNext()) {
    var row = cursor.next();
    writeOutput(row.name & "<br>");
}

// update
coll.update({"name": "Alice"}, {"$set": {"score": 50}});

// remove
coll.remove({"name": "Bob"});

// count
writeOutput(coll.count());                  // all documents
writeOutput(coll.count({"score": {"$gt": 10}})); // with filter

// distinct
var names = coll.distinct("name");
var highScorers = coll.distinct("name", {"score": {"$gt": 50}});

// aggregate
var results = coll.aggregate([
    {"$group": {"_id": "$category", "total": {"$sum": "$score"}}},
    {"$sort":  {"total": -1}}
], {});
while (results.hasNext()) {
    var row = results.next();
    writeOutput(row._id & ": " & row.total & "<br>");
}

// switch databases using the same connection pool
var otherDb = db.getSiblingDB("otherdb"); // also: getSisterDB()
```

---

## Key Ordering in Compound Specs

MongoDB treats field order as significant in compound index specs and multi-key sort
documents. Use Lucee's **ordered struct** syntax (square brackets) wherever key order
matters:

```cfml
// sort: age ascending, then name descending within each age group
coll.find().sort(["age": 1, "name": -1]);

// compound index with a specific key order
coll.createIndex(["age": 1, "name": 1]);
```

Unordered curly-brace structs `{"age": 1, "name": -1}` may not preserve insertion order
and should only be used where field order is irrelevant (e.g. filter documents).

---

## Data Type Mapping

| MongoDB / BSON | CFML / Lucee |
|---|---|
| Document | Native Lucee Struct (case-insensitive) |
| Array | Native Lucee Array |
| ObjectId | `ObjectIdImpl` (has `.toString()`, `.getDate()`) |
| String / Boolean / Number | Native CFML scalar |
| Date | `java.util.Date` |
| null / undefined | `null` |

Documents returned from all query and aggregation operations are **native, case-insensitive
Lucee Structs**, so `doc.Name`, `doc.name`, and `doc.NAME` all refer to the same field.
Nested documents and arrays are also converted recursively.

---

## Write Results

`insert()`, `update()`, `save()`, and `remove()` all return a `WriteResult` object:

```cfml
var result = coll.update({"_id": 1}, {"$set": {"active": true}});
writeOutput(result.getN());               // number of documents affected
writeOutput(result.isUpdateOfExisting()); // true if an existing doc was modified
writeOutput(result.isOk());              // true if acknowledged
```

---

## Indexes

```cfml
// simple
coll.createIndex("fieldName");

// with options — all IndexOptions fields are supported
coll.createIndex(["field": 1], {
    "name":                   "my_index",
    "unique":                 true,
    "sparse":                 false,
    "partialFilterExpression": {"status": {"$eq": "active"}},
    "expireAfterSeconds":     3600,          // TTL index
    "weights":                {"title": 10, "body": 1},  // text index
    "defaultLanguage":        "english"
});

// drop by name or by spec doc
coll.dropIndex("my_index");
coll.dropIndex(["field": 1]);  // ordered struct — must match stored key order

// drop all non-_id indexes
coll.dropIndexes();

// list indexes
var indexes = coll.getIndexes(); // array of structs
```

---

## Caching

The extension can be used as a Lucee cache provider backed by MongoDB. Configure it in
the Lucee admin under **Server → Cache** and select the MongoDB cache type.

---

## `cfdump` Behaviour

Dumping a database handle or collection object shows **metadata only** — it never
retrieves or displays documents:

- **DB dump** — shows database name and a sorted list of collection names.
- **Collection dump** — shows namespace, document count, write concern, and index names.

---

## Breaking Changes from Driver 3.x

Applications upgrading from the previous 3.x-based extension should be aware of the
following removals:

| Feature | Status |
|---|---|
| `mapReduce()` | Removed — use `aggregate()` with `$group` instead |
| `eval()` | Removed in MongoDB 4.2+ — use aggregation pipelines |
| `addUser()` / `removeUser()` | Removed — manage users via the MongoDB shell or admin commands |
| `MongoDBObject()` BIF | Removed — use Lucee's ordered struct syntax `["key": val, ...]` |
| `DBCursor.count()` with limit/skip | **Behaviour change** — `count()` now returns the total documents matching the filter, ignoring `limit()`/`skip()`; `size()` respects them |
| `addOption()`, `setOptions()`, `getOptions()`, `resetOptions()` on DB/cursor | Removed |
| `snapshot()` on cursor | Removed |
| `background` index build option | Accepted but silently ignored by MongoDB 4.2+ |
| `dropDups` index option | Removed in MongoDB 3.0 — silently ignored |

---

## Contributing / Building

Requirements: JDK 11+, Apache Ant.

```bash
# build the extension
JAVA_HOME=/path/to/jdk11 ant -buildfile build.xml

# output
target/mongodb-extension-5.5.1.1.lex
```

Install the `.lex` file via the Lucee Server Administrator (**Extensions → Applications**)
or drop it into `{lucee-server}/context/extensions/available/`.

---

## Running Tests

Tests use [TestBox](https://testbox.ortusbooks.com/) and require a running MongoDB instance.

**Configuration** — copy `tests/properties.cfm` and populate:

```cfml
variables.mongoDB = {
    server: "localhost",
    port:   27017,
    user:   "",
    pass:   ""
};
```

Alternatively set environment variables: `MONGODB_SERVER`, `MONGODB_PORT`,
`MONGODB_USERNAME`, `MONGODB_PASSWORD`.

A [CommandBox](https://www.ortussolutions.com/products/commandbox) `server.json` is
included for spinning up a local Lucee 6 test server:

```bash
box start
box testbox run
```

---

## License

Copyright (c) 2021, Lucee Association Switzerland. All rights reserved.

Licensed under the GNU Lesser General Public License v2.1 or later.
See [http://www.gnu.org/licenses/lgpl-2.1.html](http://www.gnu.org/licenses/lgpl-2.1.html).
