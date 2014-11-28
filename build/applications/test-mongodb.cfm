<cfsetting showdebugoutput="no">
<!---
<cffunction name="valueEquals">
	<cfargument name="left" required="yes">
	<cfargument name="right" required="yes">
    <cf_valueEquals left="#left#" right="#right#">
</cffunction>
 
<cf_valueEquals left="" right="">
--->
<!---
<cfquery datasource="test_ms">
select 'test' as test
</cfquery>
--->
<cfscript>

// for this we do a build in function
db=MongoDBConnect("test","localhost", 27017);	


dump("------ DB -------");
	dump(var:db, expand:false);
	dump(db.getCollectionNames());
	dump(db.getName());
	
	dump(structCount(db));
	loop collection="#db#" index="n" item="v" {
		dump(var:v,label:n, expand:false);
	}
	
dump("------ Collection ""test"" -------");
	dump(var:db.test, expand:false);
	dump(var:db.TEST, expand:false);
	dump(var:db['test'], expand:false);
	
	dump(var:db.test.find(), expand:false);
	dump(var:db.test.findOne(), expand:false);

dump("------ Collection ""test2"" -------");	
	db.test2={};
	db.test2.insert({susi:"Sorglos"});
	dump(db.test2);
	dump(db.test2.findOne().susi);
	id=db.test2.findOne()._id;

dump("------ Collection ""test2._id"" -------");
	dump(id);
	dump(id&"");
	dump(id.inc);
	dump(id.time);
	dump(id.machine);
	dump(id.getMachine());
	// id.susi=1;
	// x=id.susi;
</cfscript>

