<!---
 *
 * Copyright (c) 2015, Lucee Association Switzerland. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 *
 ---><cfsetting showdebugoutput="no">
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
// or
// db=MongoDBConnect("test","mongodb://localhost:27017");


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

