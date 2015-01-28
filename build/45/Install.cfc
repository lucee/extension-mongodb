<cfcomponent>
	<cfset variables.previousJars=["railo-mongodb-v01.jar","railo-mongodb-v02.jar","railo-mongodb-v03.jar","railo-mongodb-v04.jar"]>
    <cfset variables.previousTLDs=[]>
    


    <cffunction name="install" returntype="string" output="no"
    	hint="called from Lucee to install application">
    	<cfargument name="error" type="struct">
        <cfargument name="path" type="string">
        <cfargument name="config" type="struct">


		
		<!--- flds/tlds --->
		<cfloop list="fld,tld" item="local._type">
			<cfset local.dir=path&"#_type#s/">
			<cfif directoryExists(dir)>
				<cfdirectory action="list" directory="#dir#" filter="*.#_type#" name="local.qry">
				<cfloop query="#qry#">
					<cfset attr[_type]="#dir#/#qry.name#">
					<cfadmin 
			           action="update#_type#"
			           type="#request.adminType#"
			           password="#session["password"&request.adminType]#"
			           attributeCollection="#attr#">
				</cfloop>
			</cfif>
		</cfloop>
		
		<!--- jars --->
		<cfset local.dir=path&"jars/">
		<cfif directoryExists(dir)>
			<cfdirectory action="list" directory="#dir#" filter="*.jar" name="local.qry">
			<cfloop query="#qry#">
				<cfadmin 
					action="updateJar"
					type="#request.adminType#"
					password="#session["password"&request.adminType]#"
					jar="#dir#/#qry.name#">
			</cfloop>
		</cfif>
		
		<!--- applications --->
		<cfset templates=[]>
		<cfset local.dir=path&"applications/">
		<cfset local.trg=expandPath('{web-root-directory}')>
		<cfif directoryExists(dir)>
			<cfdirectory action="list" directory="#dir#" name="local.qry" recurse="yes">
			<cfloop query="#qry#">
				<cfset local.fullpath="#qry.directory#/#qry.name#">
				<cfif fileExists(fullpath) and qry.name NEQ ".DS_Store">
				
					<cfset template=qry.name>
					<cfset arrayAppend(templates,template)>
					
					<cffile action="copy" 
						source="#fullpath#"
						destination="#trg#/#template#">
				</cfif>
			</cfloop>
		</cfif>
		
		<cfset msg='The Extension is now successful installed. You need to restart Lucee before you can use this Extension.'>
		<cfif arrayLen(templates)>
		<cfset msg&="We added some templates for testing the Extension [#arrayToList(templates)#]">
		</cfif>
        
        <cfreturn msg>
        
    </cffunction>
    	
     <cffunction name="update" returntype="string" output="no"
    	hint="called from Lucee to update a existing application">
		<cfset removeOlderJars()>
		<cfset removeOlderTLDs()>
		<cfreturn install(argumentCollection=arguments)>
    </cffunction>
    
    
    <cffunction name="uninstall" returntype="string" output="no"
    	hint="called from Lucee to uninstall application">
    	<cfargument name="path" type="string">
        <cfargument name="config" type="struct">
		
		<!--- flds --->
		<cfloop list="fld,tld" item="local._type">
			<cfset local.dir=path&"#_type#s/">
			<cfif directoryExists(dir)>
				<cfdirectory action="list" directory="#dir#" filter="*.#_type#" name="local.qry">
				<cfloop query="#qry#">
					<cfadmin 
			            action="remove#_type#"
			            type="#request.adminType#"
			            password="#session["password"&request.adminType]#"
			            name="#qry.name#">
				</cfloop>
			</cfif>
		</cfloop>

		<!--- jars --->
		<cfset local.dir=path&"jars/">
		<cfif directoryExists(dir)>
			<cfdirectory action="list" directory="#dir#" filter="*.jar" name="local.qry">
			<cfloop query="#qry#">
				<cfadmin 
					action="removeJar"
	            	type="#request.adminType#"
            		password="#session["password"&request.adminType]#"
            		jar="#qry.name#">
			</cfloop>
		</cfif>
        
	   <!--- remove jar
        
		<cfset file="#config.mixed.destination#/test-mongodb.cfm">
       
        <cfif FileExists(file)>
        	<cffile action="delete" file="#file#">
        </cfif> --->
		
        <cfreturn 'The Extension is now successful removed'>
    </cffunction>
	
	<cffunction name="removeOlderJars" returntype="void" output="no" access="private">
    	<cfloop from="1" to="#arrayLen(variables.previousJars)#" index="i">
            <cftry>
				<cfadmin 
					action="removeJar"
	            	type="#request.adminType#"
            		password="#session["password"&request.adminType]#"
            		jar="#variables.previousJars[i]#">
                <cfcatch></cfcatch>
            </cftry>
        </cfloop>
    </cffunction>
    
    <cffunction name="removeOlderTLDs" returntype="void" output="no" access="private">
    	<cfloop from="1" to="#arrayLen(variables.previousTLDs)#" index="i">
            <cftry>
                <cfadmin 
                    action="removeTLD"
                    type="#request.adminType#"
                    password="#session["password"&request.adminType]#"
                    tld="#variables.previousTLDs[i]#">
                <cfcatch></cfcatch>
            </cftry>
        </cfloop>
    </cffunction>
    
    <cffunction name="validate" returntype="string" output="no"
    	hint="called from Lucee to install application">
    	<cfargument name="error" type="struct">
        <cfargument name="path" type="string">
        <cfargument name="config" type="struct">
        <cfargument name="step" type="numeric">
        
    </cffunction>
<cfscript>
private function mani(string path,string key){
	var lcl="";
	var rest="";
	var f="";
	var l="";
	loop file="#path#" item="local.line" {
		line=trim(line);
		lcl=lcase(line);
		if(left(lcl,len(key))==key) {
			rest=trim(mid(line,len(key)+1));
			if(left(rest,1)==':') {
				rest=trim(mid(rest,2));
				f=left(rest,1);
				l=right(rest,1);
				if(f=='"' && l=='"') return mid(rest,2,len(rest)-2);
				if(f=="'" && l=="'") return mid(rest,2,len(rest)-2);
				return rest;
			}
		}
			
	}
	return "";
}
</cfscript>
</cfcomponent>