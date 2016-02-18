<cfscript>
	include template="properties.cfm";

	r = new testbox.system.TestBox(directory="tests");
	echo(r.run());
</cfscript>