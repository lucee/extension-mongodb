<cfscript>
	function MongoDBObject(){
		var DBObject = createobject("java","com.mongodb.BasicDBObjectBuilder").start();
		var key = "";

		for (key in arguments) {
			DBObject.append( key, arguments[key] )
		}

		return DBObject.get();
	}
</cfscript>
