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
 ---><cfcomponent>

	<cfset variables.previousJars=[
	"mongo-java-driver-2.13.0.jar","mongo-java-driver-2.12.4.jar","mongo-java-driver-3.0.3.jar","mongo-java-driver-3.2.0.jar","mongo-java-driver-3.4.2.jar",
	"railo-mongodb-v01.jar","railo-mongodb-v02.jar","railo-mongodb-v03.jar","railo-mongodb-v04.jar",
	"mongodb-extension-1.0.0.1.jar","mongodb-extension-1.0.0.2.jar","mongodb-extension-1.0.0.3.jar","mongodb-extension-1.0.0.4.jar","mongodb-extension-1.0.0.5.jar","mongodb-extension-1.0.0.6.jar","mongodb-extension-1.0.0.7.jar","mongodb-extension-1.0.0.8.jar","mongodb-extension-1.0.0.9.jar",
	"mongodb-extension-1.0.0.10.jar","mongodb-extension-1.0.0.11.jar","mongodb-extension-1.0.0.12.jar","mongodb-extension-1.0.0.13.jar","mongodb-extension-1.0.0.14.jar","mongodb-extension-1.0.0.15.jar","mongodb-extension-1.0.0.16.jar","mongodb-extension-1.0.0.17.jar","mongodb-extension-1.0.0.18.jar","mongodb-extension-1.0.0.19.jar",
	"mongodb-extension-1.0.0.20.jar","mongodb-extension-1.0.0.21.jar","mongodb-extension-1.0.0.22.jar","mongodb-extension-1.0.0.23.jar","mongodb-extension-1.0.0.24.jar","mongodb-extension-1.0.0.25.jar","mongodb-extension-1.0.0.26.jar","mongodb-extension-1.0.0.27.jar","mongodb-extension-1.0.0.28.jar","mongodb-extension-1.0.0.29.jar",
	"mongodb-extension-1.0.0.30.jar","mongodb-extension-1.0.0.31.jar","mongodb-extension-1.0.0.32.jar","mongodb-extension-1.0.0.33.jar","mongodb-extension-1.0.0.34.jar","mongodb-extension-1.0.0.35.jar","mongodb-extension-1.0.0.36.jar","mongodb-extension-1.0.0.37.jar","mongodb-extension-1.0.0.38.jar","mongodb-extension-1.1.0.45.jar",
	"mongodb-extension-1.1.0.46.jar","mongodb-extension-1.1.0.47.jar","mongodb-extension-3.2.0.48.jar","mongodb-extension-3.2.2.52.jar"
	]>
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

		<!--- functions --->
		<cfset local.dir=path&"functions/">
		<cfset local.trg=expandPath('{lucee-server-directory}/library/function')>
		<cfif directoryExists(dir)>
			<cfdirectory action="list" directory="#dir#" name="local.qry" recurse="yes">
			<cfloop query="#qry#">
				<cfset local.fullpath="#qry.directory#/#qry.name#">
				<cfif fileExists(fullpath) and qry.name NEQ ".DS_Store">
					<cfset template=qry.name>
					<cffile action="copy"
						source="#fullpath#"
						destination="#trg#/#template#">
				</cfif>
			</cfloop>
		</cfif>

        <cfadmin
        	action="updateContext"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            source="#path#context/admin/cdriver/MongoDBCache.cfc"
            destination="admin/cdriver/MongoDBCache.cfc">

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

		<!--- functions --->
		<cfset local.dir=path&"functions/">
		<cfset local.trg=expandPath('{lucee-server-directory}/library/function')>
		<cfif directoryExists(dir)>
			<cfdirectory action="list" directory="#dir#" name="local.qry" recurse="yes">
			<cfloop query="#qry#">
				<cfset local.fullpath="#local.trg#/#qry.name#">
				<cfif fileExists(fullpath)>
					<cfset template=qry.name>
					<cffile action="delete"
						file="#fullpath#">
				</cfif>
			</cfloop>
		</cfif>

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

        <cfadmin
        	action="removeContext"
            type="#request.adminType#"
            password="#session["password"&request.adminType]#"
            destination="admin/cdriver/MongoDBCache.cfc">

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