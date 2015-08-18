MongoDB Extension for Lucee Server
-----------------

Welcome to the Lucee Server MongoDB Integration Extension source code repository.

Lucee Server, or simply Lucee, is a dynamic Java based tag and scripting language used for rapid development from simple to highly sophisticated web applications. Lucee simplifies technologies like webservices (REST,SOAP,HTTP), ORM (Hibernate), searching (Lucene), datasources (MSSQl,Oracle,MySQL ...), caching (infinispan,ehcache,memcached ...) and a lot more. It was never easier to integrate any backend technology with the internet.
Lucee is of course open source (LGPL 2.1) and available for free!

MongoDB (from "humongous") is a cross-platform document-oriented database. Classified as a NoSQL database, MongoDB eschews the traditional table-based relational database structure in favor of JSON-like documents with dynamic schemas (MongoDB calls the format BSON), making the integration of data in certain types of applications easier and faster.

Overview
-----------------
This extension provides Lucee Server with functions for managing connections to MongoDB (using the underlying Java MongoDB driver) and takes care of converting data types from Lucee to Java and vice versa. It is designed to provide MongoDB shell-like dialect directly in Lucee code. For example:

<pre><code>db  = MongoDBConnect("test","localhost", 27017);
or
db  = MongoDBConnect("mongodb://localhost:27017/test");


// use struct notation for collections
cur = db["someCollection"].find();

// alternate getCollection() method
db.getCollection("otherCollection").save(
    {
         "_id":MongoDBID()
        ,"foo":"bar"
    }
)

// unquoted key names can be used if Lucee Language/Compiler setting for dot notation is "keep original case"
db.otherCollection.save(
    {
         _id:MongoDBID()
        ,mongo:"rocks"
    }
)
</code></pre>

MongoDB documentation: https://docs.mongodb.org/manual/reference/command/

Two built-in-functions (BIFs) are also installed with the extension: MongoDBID() and MongoDBObject(). 

<strong>MongoDBID()</strong> creates MongoDB Object ID values and takes no argument, or a single argument of either a <strong>string</strong> (must be valid ObjectID string) or a <strong>date</strong> and returns an ObjectID. See: http://docs.mongodb.org/manual/core/object-id

MongoDBObject() creates an ordered BasicDBObject, which is essentially a struct with keys that iterate in predictable order. This is useful in MongoDB for passing in structs where key order needs to be maintained (such as in sort() or ensureIndex() operations). MongoDBObject() takes any number of arguments and returns a BasicDBObject. Example:

<pre><code>
db.getCollection("test").find({}).sort(MongoDBObject("sortFirst":1, "sortSecond":-1));
</code></pre>


Contributing
-----------------
Two build scripts are included, build.xml for Lucee 5+ and build45.xml for Lucee 4.5. Also included is a local Extension Provider. To use this, unzip the contents of misc/ExtensionProvider.zip to your web root so you have:

<pre><code>/webroot/ExtensionProvider.cfc
/webroot/ext/
</code></pre>

Add your local extension provider in the Lucee server admin. After building this project copy the files in /dist to /webroot/ext/mongodb/. Then use your local extension provider to update the extension.

License/Copyright
-----------------
Copyright (c) 2015, Lucee Association Switzerland. All rights reserved.

This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either  version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.