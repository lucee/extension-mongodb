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
component extends="org.lucee.cfml.test.LuceeTestCase" labels="mongodb"	{

	include "../properties.cfm";

	variables.id=hash(server.lucee.version);


	public function setUp(){
		createCache();
	}

	public function tearDown(){
		try {
			deleteCache();
		} finally {
			MongoDBConnect(request.cache.mongodb.custom.database, request.cache.mongodb.custom.uri)
				.getCollection(variables.collectionName).drop();
		}
	}

	public void function testDate(){
		var key = "testDate"&variables.id;
		// store a value to the cache that expires after a second
		cachePut(id=key, value=now(), timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id=key, cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isDate(value));
	}

	public void function testNumeric(){
		var key = "testNumeric"&variables.id;
		// store a value to the cache that expires after a second
		cachePut(id=key, value=100, timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id=key	, cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isNumeric(value)); 
	}
	
	public void function testStruct(){

		var key = "testStruct"&variables.id;
		// store a value to the cache that expires after a second
		cachePut(id=key, value={"foo":"bar"}, timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id=key, cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isStruct(value) && structkeyexists(value,"foo"));
	}

	public void function testArray(){

		var key = "testArray"&variables.id;
		// store a value to the cache that expires after a second
		cachePut(id=key, value=[1,2,3], timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id=key, cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value) && isArray(value) && value.len()==3);
	}

	public void function testString(){

		var key = "testString"&variables.id;
		// store a value to the cache that expires after a second
		cachePut(id=key, value="FooBar", timeSpan=createTimespan(0,0,0,1), cacheName:"MongoDBTestCache");
		
		// after a couple of nanos schould still exists
		var value = cacheGet(id=key, cacheName:"MongoDBTestCache");
		assertTrue(!isNull(value));
		$assert.isEqual("FooBar",value);
	}
	
	public void function testCacheEnumerationAndCount(){
		cachePut(id="coverage-one", value="one", cacheName="MongoDBTestCache");
		cachePut(id="coverage-two", value={"answer":42}, cacheName="MongoDBTestCache");
		cachePut(id="other", value="untouched", cacheName="MongoDBTestCache");
		assertEquals(3, cacheCount(cacheName="MongoDBTestCache"));
		var values = cacheGetAll(filter="coverage-*", cacheName="MongoDBTestCache");
		assertEquals(2, structCount(values));
		assertEquals("one", values["coverage-one"]);
		assertEquals(42, values["coverage-two"].answer);
		assertEquals(2, arrayLen(cacheGetAllIds(filter="coverage-*", cacheName="MongoDBTestCache")));
	}

	public void function testCacheOverwriteAndDelete(){
		cachePut(id="MixedCase", value="first", cacheName="MongoDBTestCache");
		cachePut(id="MIXEDCASE", value="second", cacheName="MongoDBTestCache");
		assertEquals(1, cacheCount(cacheName="MongoDBTestCache"));
		assertEquals("second", cacheGet(id="mixedcase", cacheName="MongoDBTestCache"));
		assertTrue(cacheIdExists(id="MixedCase", cacheName="MongoDBTestCache"));
		cacheDelete(id="MIXEDCASE", cacheName="MongoDBTestCache");
		assertFalse(cacheIdExists(id="mixedcase", cacheName="MongoDBTestCache"));
		assertTrue(isNull(cacheGet(id="mixedcase", cacheName="MongoDBTestCache")));
		assertEquals(0, cacheCount(cacheName="MongoDBTestCache"));
	}

	public void function testCacheClearWithFilter(){
		cachePut(id="coverage-one", value=1, cacheName="MongoDBTestCache");
		cachePut(id="coverage-two", value=2, cacheName="MongoDBTestCache");
		cachePut(id="other", value=3, cacheName="MongoDBTestCache");
		assertEquals(2, cacheClear(filter="coverage-*", cacheName="MongoDBTestCache"));
		assertEquals(1, cacheCount(cacheName="MongoDBTestCache"));
		assertEquals(3, cacheGet(id="other", cacheName="MongoDBTestCache"));
		assertEquals(1, cacheClear(cacheName="MongoDBTestCache"));
		assertEquals(0, cacheCount(cacheName="MongoDBTestCache"));
	}

	private string function createURL(string calledName){
		var baseURL="http://#cgi.HTTP_HOST##getDirectoryFromPath(contractPath(getCurrenttemplatepath()))#";
		return baseURL&""&calledName;
	}

	private function createCache() {
		var custom = duplicate(request.cache.mongodb.custom);
		variables.collectionName = "testcache_" & lcase(replace(createUUID(), "-", "", "all"));
		custom.collection = variables.collectionName;
	

		admin 
			action="updateCacheConnection"
			type="web"
			password="#request.webadminpassword#"			
			default="object"
			name="MongoDBTestCache" 
			class="#request.cache.mongodb.class#" 
			storage="true"
			custom="#custom#";
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