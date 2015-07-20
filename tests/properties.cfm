<cfscript>

request.cache.mongodb.class="org.lucee.mongodb.cache.MongoDBCache";
request.cache.mongodb.custom={
	'collection':'test',
	'password':'',
	'connectionsPerHost':'10',
	'database':'test',
	'hosts':'localhost:27017',
	'persist':'true',
	'username':''};


</cfscript>