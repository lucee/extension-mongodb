<cfscript>
request.cache.mongodb.class="org.lucee.mongodb.cache.MongoDBCache";
request.cache.mongodb.custom={
	 'collection':'testcache'
	,'database':'test'
	,'uri':'mongodb://localhost:27017'
	,'persist':true
};

server.system.properties.MONGODB_SERVER = "localhost";
server.system.properties.MONGODB_PORT = 27017;
server.system.properties.MONGODB_USERNAME = "";
server.system.properties.MONGODB_PASSWORD = "";

request.webadminpassword = "";
</cfscript>