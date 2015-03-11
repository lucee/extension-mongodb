<!--- 
 *
 * Copyright (c) 2015, Lucee Assosication Switzerland. All rights reserved.*
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
 ---><cfscript>
component extends="org.lucee.cfml.test.LuceeTestCase"	{

	//public function setUp(){}

	public void function test(){
		createCache();

		// store a value to the cache that expires after a second
		cachePut(id="cacheTestTemp", value=now(), timeSpan=createTimespan(0,0,0,1));
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id="cacheTestTemp");
		assertTrue(!isNull(value) && isDate(value));
		sleep(2000);
		var value = cacheGet(id="cacheTestTemp");
		assertTrue(isNull(value));




/*
if (isdefined("value")) {
	htmlhead text='<meta http-equiv="Refresh" content="1;url=TestCacheTimeout.cfm?cache=#url.cache#&timeout=#url.timeout#">';
	writeoutput('still cached: ');
} else {
	writeoutput('disappeared from cache: ');
}
writeoutput('#TimeFormat( elapsed, "m" )# Minutes, #TimeFormat( elapsed, "s" )# Seconds <br>');





		setting showdebugoutput="no" requesttimeout="1000";
		http result="local.result" url="#createURL("Issue0001/set.cfm")#" addtoken="true";
		assertEquals(true,isDate(result.filecontent.trim()));
		http result="local.result" url="#createURL("Issue0001/get.cfm")#" addtoken="true";
		sleep(10000);
		http method="get" result="local.result" url="#createURL("Issue0001/get.cfm")#" addtoken="true";
		assertEquals(false,isDate(result.filecontent.trim()));
		*/
		deleteCache();
		/*
		assertEquals("",result.filecontent);
		
		try{
			// error
			fail("");
		}
		catch(local.exp){}*/
	}
	
	private string function createURL(string calledName){
		var baseURL="http://#cgi.HTTP_HOST##getDirectoryFromPath(contractPath(getCurrenttemplatepath()))#";
		return baseURL&""&calledName;
	}

	private function createCache() {
		admin 
			action="updateCacheConnection"
			type="web"
			password="#request.webadminpassword#"
			
			default="object"
			name="mmongoDBTest" 
			class="#request.cache.mongodb.class#" 
			storage="true"
			custom="#request.cache.mongodb.custom#";
	}
				
	private function deleteCache(){
		admin 
			action="removeCacheConnection"
			type="web"
			password="#request.webadminpassword#"
			name="mmongoDBTest";
						
	}
	
} 
</cfscript>