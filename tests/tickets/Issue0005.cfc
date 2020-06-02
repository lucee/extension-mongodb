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

	public function setUp(){
		createCache();
	}

	public function tearDown(){
		deleteCache();
	}

	public void function testDate(){

		// store a value to the cache that expires after a second
		cachePut(id="cacheTestTemp", value=now(), timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id="cacheTestTemp", cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isDate(value));
		sleep(2000);
		var value = cacheGet(id="cacheTestTemp", cacheName:"MongoDBTestCache");
		assertTrue(isNull(value));

	}

	public void function testNumeric(){

		// store a value to the cache that expires after a second
		cachePut(id="cacheTestTemp", value=100, timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id="cacheTestTemp", cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isNumeric(value));
	}
	
	public void function testStruct(){

		// store a value to the cache that expires after a second
		cachePut(id="cacheTestTemp", value={"foo":"bar"}, timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id="cacheTestTemp", cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isStruct(value) && structkeyexists(value,"foo"));
	}

	public void function testArray(){

		// store a value to the cache that expires after a second
		cachePut(id="cacheTestTemp", value=[1,2,3], timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id="cacheTestTemp", cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isArray(value) && value.len()==3);
	}

	public void function testString(){

		// store a value to the cache that expires after a second
		cachePut(id="cacheTestTemp", value="FooBar", timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id="cacheTestTemp", cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value));
		$assert.isEqual("FooBar",value);
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
			name="MongoDBTestCache" 
			class="#request.cache.mongodb.class#" 
			storage="true"
			custom="#request.cache.mongodb.custom#";
	}
				
	private function deleteCache(){
		admin 
			action="removeCacheConnection"
			type="web"
			password="#request.webadminpassword#"
			name="MongoDBTestCache";			
	}
	
} 
</cfscript>