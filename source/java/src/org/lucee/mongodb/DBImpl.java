package org.lucee.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lucee.mongodb.support.DBImplSupport;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Objects;
import lucee.runtime.type.Struct;

public class DBImpl extends DBImplSupport implements Collection,Objects {

	private static final long serialVersionUID = -378132108333079775L;
	private final DB db;
	private static Map<String,MongoClient> clients=new ConcurrentHashMap<String, MongoClient>();
	
	public DBImpl(DB db){
		this.db=db;
	}
	
	public static DBImpl getInstance(String dbName,String host, int port, boolean createNewClient) throws UnknownHostException{
		String key=host+":"+port;
		
		MongoClient client = createNewClient?null:clients.get(key);
		if(client==null) {
			 client = createClient(host,port); 
			 clients.put(key, client);
		}
		return new DBImpl(client.getDB(dbName));
	}
	

	private static MongoClient createClient(String host, int port) throws UnknownHostException { 
		boolean hasHost=!Util.isEmpty(host);
		boolean hasPort=port>0;
		
		if(!hasHost && !hasPort)return new MongoClient();
		else if(!hasPort)return new MongoClient(host);
		else if(!hasHost)return new MongoClient("localhost",port);
		return new MongoClient(host,port);
	}

	public void test(){
		keys();
		toDumpData(CFMLEngineFactory.getInstance().getThreadPageContext(), -1, null);
	}
	

	@Override
	public Iterator<String> keysAsStringIterator() {
		return db.getCollectionNames().iterator();
	}

	@Override
	public Iterator<Key> keyIterator() {
		return new KeyIterator(caster,db.getCollectionNames().iterator());
	}

	/*public Iterator<Object> valueIterator() {
		return new ValueIterator(db, db.getCollectionNames().iterator());
	}

	public Iterator<Entry<Key, Object>> entryIterator() {
		return new EntryIterator(caster,db, this, db.getCollectionNames().iterator());
	}*/

	@Override
	public int size() {
		return db.getCollectionNames().size();
	}

	@Override
	public Key[] keys() {
		Iterator<String> it = db.getCollectionNames().iterator();
		List<Key> list=new ArrayList<Key>();
		while(it.hasNext()){
			list.add(caster.toKey(it.next(),null));
		}
		return list.toArray(new Key[list.size()]);
	}

	@Override
	public Object remove(Key key) throws PageException {
		if(!containsKey(key.getString())) 
			throw exp.createExpressionException("can't remove DBCollection with key ["+key+"], key doesn't exist");
		
		DBCollection coll = db.getCollection(key.getString());
		coll.drop();
		return toCFML(coll);
	}

	@Override
	public Object removeEL(Key key) {
		try {
			return remove(key);
		} catch (PageException e) {
			return null;
		}
	}

	// TODO @Override 
	public Object remove(Key key, Object defaultValue) {
		try {
			return remove(key);
		} catch (PageException e) {
			return defaultValue;
		}
	}

	@Override
	public void clear() {
		Iterator<Key> it = keyIterator();
		Key k;
		DBCollection coll;
		while(it.hasNext()){
			k=it.next();
			coll = db.getCollection(k.getString());
			coll.drop();
		}
	}

	@Override
	public Object get(String key) throws PageException {
		DBCollection coll = db.getCollection(key);
		if(coll.count()!=0 || containsKey(key)) return toCFML(coll);
		throw exp.createExpressionException("key ["+key+"] doesn't exist ");
	}

	@Override
	public Object get(String key, Object defaultValue) {
		DBCollection coll = db.getCollection(key);
		if(coll.count()!=0 || containsKey(key)) return toCFML(coll);
		return defaultValue;
	}

	@Override
	public Object set(String key, Object value) throws PageException {
		if(containsKey(key)) {
			throw exp.createExpressionException("there is already a collection with name ["+key+"], you have to remove this collection first by calling the method \"drop()\" for example");
			//DBCollection coll = db.getCollection(key);
			//coll.drop();
		}
		db.createCollection(key, _toDBObject(value));
		return value;
	}

	@Override
	public Object setEL(String key, Object value) {
		try {
			return set(key, value);
		} 
		catch (Throwable t) {}
		return value;
	}

	@Override
	public Collection duplicate(boolean deepCopy) {
		throw new UnsupportedOperationException("this operation is not suppored");
	}

	@Override
	public boolean containsKey(String key) {
		Iterator<String> it = db.getCollectionNames().iterator();
		while(it.hasNext()) {
			if(key.equals(it.next())) return true;
		}
		return false;
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {
		
		// db.addOptions(options);
		if(methodName.equals("addOption")) {
			checkArgLength("addOption",args,1,1);
			db.addOption(caster.toIntValue(args[0]));
			return null;
		}
		// db.addUser(username, password[, readOnly])
		else if(methodName.equals("addUser")) {
			checkArgLength("addUser",args,2,3);
			return toCFML(db.addUser(
				caster.toString(args[0]), 
				caster.toString(args[1]).toCharArray(), // TODO is this .toCharArray() valid? 
				args.length==2?false:caster.toBooleanValue(args[2])
			));
		}
		// db.auth(username, password)
		else if(methodName.equals("auth") || methodName.equals("authenticate")) {
			checkArgLength(methodName.getString(),args,2,2);
			return toCFML(db.authenticate(
				caster.toString(args[0]), 
				caster.toString(args[1]).toCharArray() // TODO is this .toCharArray() valid? 
			));
		}
		// db.authenticateCommand(username, password)
		else if(methodName.equals("authenticateCommand") || methodName.equals("authenticateCommand")) {
			checkArgLength(methodName.getString(),args,2,2);
			return toCFML(db.authenticateCommand(
				caster.toString(args[0]), 
				caster.toString(args[1]).toCharArray() // TODO is this .toCharArray() valid? 
			));
		}
		// cleanCursors(boolean)
		else if(methodName.equals("cleanCursors")) {
			checkArgLength("cleanCursors",args,1,1);
			db.cleanCursors(caster.toBooleanValue(args[0]));
			return null;
		}
		// collectionExists(boolean)
		else if(methodName.equals("collectionExists")) {
			checkArgLength("collectionExists",args,1,1);
			return toCFML(db.collectionExists(caster.toString(args[0])));
		}
		// command
		else if(methodName.equals("command")) {
			checkArgLength("command",args,1,1);
			DBObject dbo = toDBObject(args[0],null);
			if(dbo!=null)return toCFML(db.command(dbo));
			return toCFML(db.command(caster.toString(args[0])));
		}
		
		// db.createCollection(name[, {capped: <boolean>, size: <value>, max <bytes>}])
		else if(methodName.equals("createCollection")) {
			checkArgLength("createCollection",args,1,2);
			if(args[1]==null)args[1]=new BasicDBObject(); 
			return toCFML(db.createCollection(
					caster.toString(args[0]), 
					_toDBObject(args[1])
				));
		}
		// db.dropDatabase()
		else if(methodName.equals("dropDatabase")) {
			checkArgLength("dropDatabase",args,0,0);
			db.dropDatabase();
			return null;
		}
		// db.eval(function, arguments)
		else if(methodName.equals("eval")) {
			checkArgLength("eval",args,1,2);
			return toCFML(db.eval(
					caster.toString(args[0]), 
					toNativeMongoArray(args[1])
				));
		}
		// db.forceError()
		else if(methodName.equals("forceError")) {
			checkArgLength("forceError",args,0,0);
			db.forceError();
			return null;
		}
		// db.getCollection(name)
		else if(methodName.equals("getCollection")) {
			checkArgLength("getCollection",args,1,1);
			return toCFML(db.getCollection(caster.toString(args[0])));
		}
		// getCollectionFromString(name)
		else if(methodName.equals("getCollectionFromString")) {
			checkArgLength("getCollectionFromString",args,1,1);
			return toCFML(db.getCollectionFromString(caster.toString(args[0])));
		}
		// db.getCollectionNames()
		else if(methodName.equals("getCollectionNames")) {
			checkArgLength("getCollectionNames",args,0,0);
			return toCFML(db.getCollectionNames());
		}
		// db.getLastError()
		else if(methodName.equals("getLastError")) {
			checkArgLength("getLastError",args,0,1);
			WriteConcern wc = toWriteConcern(args[0],null);
			if(wc!=null) return toCFML(db.getLastError(wc));
			return toCFML(db.getLastError());
		}
		//  d.getMongo()
		else if(methodName.equals("getMongo")) {
			checkArgLength("getMongo",args,0,0);
			return toCFML(db.getMongo());
		}
		// db.getName()
		else if(methodName.equals("getName")) {
			checkArgLength("getName",args,0,0);
			return toCFML(db.getName());
		}
		// db.getOptions()
		else if(methodName.equals("getOptions")) {
			checkArgLength("getOptions",args,0,0);
			return toCFML(db.getOptions());
		}
		// db.getPreviousError()
		else if(methodName.equals("getPreviousError")) {
			checkArgLength("getPreviousError",args,0,0);
			return toCFML(db.getPreviousError());
		}
		// db.getReadPreference()
		else if(methodName.equals("getReadPreference")) {
			checkArgLength("getReadPreference",args,0,0);
			return toCFML(db.getReadPreference());
		}
		// db.getSisterDB(name)
		else if(methodName.equals("getSisterDB")) {
			checkArgLength("getSisterDB",args,1,1);
			return toCFML(db.getSisterDB(caster.toString(args[0])));
		}
		// db.getStats()
		else if(methodName.equals("getStats")) {
			checkArgLength("getStats",args,0,0);
			return toCFML(db.getStats());
		}
		// db.getWriteConcern()
		else if(methodName.equals("getWriteConcern")) {
			checkArgLength("getWriteConcern",args,0,0);
			return toCFML(db.getWriteConcern());
		}
		// db.isAuthenticated()
		else if(methodName.equals("isAuthenticated")) {
			checkArgLength("isAuthenticated",args,0,0);
			return toCFML(db.isAuthenticated());
		}
		// db.removeUser(username)
		else if(methodName.equals("removeUser")) {
			checkArgLength("removeUser",args,1,1);
			return toCFML(db.removeUser(caster.toString(args[0])));
		}
		// db.requestDone()
		else if(methodName.equals("requestDone")) {
			checkArgLength("requestDone",args,0,0);
			db.requestDone();
			return null;
		}
		// db.requestEnsureConnection()
		else if(methodName.equals("requestEnsureConnection")) {
			checkArgLength("requestEnsureConnection",args,0,0);
			db.requestEnsureConnection();
			return null;
		}
		// db.requestStart()
		else if(methodName.equals("requestStart")) {
			checkArgLength("requestStart",args,0,0);
			db.requestStart();
			return null;
		}
		// db.resetError()
		else if(methodName.equals("resetError")) {
			checkArgLength("resetError",args,0,0);
			db.resetError();
			return null;
		}
		// db.resetOptions()
		else if(methodName.equals("resetOptions")) {
			checkArgLength("resetOptions",args,0,0);
			db.resetOptions();
			return null;
		}
		// db.setOptions(options)
		else if(methodName.equals("setOptions")) {
			checkArgLength("setOptions",args,1,1);
			db.setOptions(caster.toIntValue(args[0]));
			return null;
		}
		// db.setReadOnly(readOnly)
		else if(methodName.equals("setReadOnly")) {
			checkArgLength("setReadOnly",args,1,1);
			db.setReadOnly(caster.toBoolean(args[0]));
			return null;
		}
		
		String supportedFunctions=
		"addOption,addUser,auth,authenticateCommand,cleanCursors,collectionExists,command,createCollection,dropDatabase,eval,forceError," +
		"getCollection,getCollectionFromString,getCollectionNames,getLastError,getMongo,getName,getOptions,getPreviousError,getReadPreference," +
		"getSisterDB,getStats,getWriteConcern,isAuthenticated,removeUser,requestDone,requestEnsureConnection,requestStart,resetError," +
		"resetOptions,setOptions,setReadOnly";
		
		throw exp.createExpressionException("function ["+methodName+"] is not supported, supported functions are ["+supportedFunctions+"]");
		
		
		// TODO cloneDatabase,copyDatabase,currentOp, commandHelp,fsyncLock,getLastErrorObj,getProfilingLevel,getProfilingStatus
		// getReplicationInfo,getSiblingDB,killOp(),db.listCommands(),db.loadServerScripts(),db.logout(),db.printCollectionStats()
		// printReplicationInfo,printShardingStatus,printSlaveReplicationInfo,repairDatabase,runCommand,serverBuildInfo
		
		// TODO setReadPreference,setWriteConcern
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		if(args.isEmpty()) return call(pc, methodName, new Object[0]);
		
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}
	
	
	
	

	private DBObject _toDBObject(Object obj) throws PageException {
		if(obj instanceof DBObject) return (DBObject) obj;
		Struct sct = caster.toStruct(obj,null);
		if(sct==null)
			throw exp.createExpressionException("only a DBObject or Struct/Map can be set to define a DBCollection, " +
					"a Structs/Map can have the following possible parameters:\n" +
					"- capped:boolean: if the collection is capped\n" +
					"- size:int: collection size\n" +
					"- max:int: max number of documents"
					);
		
		BasicDBObject bo=new BasicDBObject();
		// capped
		Boolean capped = caster.toBoolean(sct.get("capped",null),null);
		if(capped!=null)bo.put("capped", capped);
		// size
		Integer size = caster.toInteger(sct.get("size",null),null);
		if(size!=null)bo.put("size", size);
		// max
		Integer max = caster.toInteger(sct.get("max",null),null);
		if(max!=null)bo.put("max", max);
		
		return bo;
	}

	public DB getDB() {
		return db;
	}
}
