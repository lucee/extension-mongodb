<cfcomponent extends="Cache">
    <cfset fields=array(
			field(	displayName="Mongo Connect String",
					name="uri",
					defaultValue="mongodb://localhost:27017",
					required=true,
					description="MongoDB connection URI. Include authentication and replica set info if needed.",
					type="textarea"
				),
			field(	displayName="Database",
					name="database",
					defaultValue="luceecache",
					required=true,
					description="The name of the database that will be used to store the data.",
					type="text"
				),
			field(	displayName="Collection",
					name="collection",
					defaultValue="",
					required=true,
					description="The name of the collection in the database above that will be used to store the data.",
					type="text"
				),
			field(	displayName="Persists over server restart",
					name="persist",
					values="true,false",
					defaultValue=true,
					required=true,
					description="",
					type="radio"
				)
		)>

	<cffunction name="getClass" returntype="string">
    	<cfreturn "org.lucee.mongodb.cache.MongoDBCache">
    </cffunction>
    
	<cffunction name="getLabel" returntype="string" output="no">
    	<cfreturn "MongoDB (Beta)">
    </cffunction>

	<cffunction name="getDescription" returntype="string" output="no">
    	<cfreturn "This is the MongoDB Cache implementation for Lucee. This allows you to cache objects, primitives and queries in a MongoDB server that can be used as a cache.">
    </cffunction>

</cfcomponent>