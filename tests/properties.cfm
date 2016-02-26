<cfscript>
request.cache.mongodb.class="org.lucee.mongodb.cache.MongoDBCache";
request.cache.mongodb.custom={
	'collection':'testcache',
	'password':'',
	'connectionsPerHost':'10',
	'database':'test',
	'hosts':'localhost:27017',
	'persist':'true',
	'username':''};

server.system.properties.MONGODB_SERVER = "localhost";
server.system.properties.MONGODB_PORT = 27017;
server.system.properties.MONGODB_USERNAME = "";
server.system.properties.MONGODB_PASSWORD = "";

request.webadminpassword = "";
</cfscript>