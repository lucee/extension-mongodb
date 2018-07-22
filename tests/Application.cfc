component {

	this.cache.connections["MongoDBTestCache"] = {
		  class: 'org.lucee.mongodb.cache.MongoDBCache'
		, storage: true
		, custom: {"uri":"mongodb://localhost:27017/test","collection":"testcache","persist":"true"}
		, default: 'object'
	};

}