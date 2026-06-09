package org.lucee.mongodb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Objects;
import lucee.runtime.type.Struct;

import org.bson.Document;
import org.lucee.mongodb.support.DBImplSupport;

import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DBImpl extends DBImplSupport implements Collection, Objects {

	private static final long serialVersionUID = -378132108333079775L;
	private MongoDatabase db;
	private final MongoClient client;
	private static Map<String, MongoClient> clients = new ConcurrentHashMap<String, MongoClient>();

	public DBImpl(MongoDatabase db, MongoClient client) {
		this.db = db;
		this.client = client;
	}

	public static DBImpl getInstance(String dbName, String host, int port, boolean createNewClient) throws MongoException {
		String key = host + ":" + port;
		MongoClient client = createNewClient ? createClient(host, port) : clients.computeIfAbsent(key, k -> createClient(host, port));
		return new DBImpl(client.getDatabase(dbName), client);
	}

	public static DBImpl getInstanceByURI(String dbName, String uri) throws MongoException {
		MongoClient client = clients.computeIfAbsent(uri, MongoClients::create);
		return new DBImpl(client.getDatabase(dbName), client);
	}

	private static MongoClient createClient(String host, int port) throws MongoException {
		boolean hasHost = !Util.isEmpty(host);
		boolean hasPort = port > 0;
		if (!hasHost && !hasPort) return MongoClients.create();
		String uri = "mongodb://" + (hasHost ? host : "localhost") + (hasPort ? ":" + port : "");
		return MongoClients.create(uri);
	}

	private boolean collectionExists(String name) {
		for (String n : db.listCollectionNames()) {
			if (n.equals(name)) return true;
		}
		return false;
	}

	@Override
	public Iterator<String> keysAsStringIterator() {
		List<String> names = new ArrayList<String>();
		db.listCollectionNames().into(names);
		return names.iterator();
	}

	@Override
	public Iterator<Key> keyIterator() {
		return new KeyIterator(caster, keysAsStringIterator());
	}

	@Override
	public int size() {
		List<String> names = new ArrayList<String>();
		db.listCollectionNames().into(names);
		return names.size();
	}

	@Override
	public Key[] keys() {
		List<Key> list = new ArrayList<Key>();
		for (String name : db.listCollectionNames()) {
			list.add(caster.toKey(name, null));
		}
		return list.toArray(new Key[list.size()]);
	}

	@Override
	public Object remove(Key key) throws PageException {
		if (!collectionExists(key.getString()))
			throw exp.createExpressionException("can't remove collection [" + key + "], key doesn't exist");
		DBCollectionImpl c = new DBCollectionImpl(db.getCollection(key.getString()), db);
		c.call(null, creator.createKey("drop"), new Object[0]);
		return toCFML(c);
	}

	@Override
	public Object removeEL(Key key) {
		try { return remove(key); } catch (PageException e) { return null; }
	}

	public Object remove(Key key, Object defaultValue) {
		try { return remove(key); } catch (PageException e) { return defaultValue; }
	}

	@Override
	public void clear() {
		for (String name : db.listCollectionNames()) {
			db.getCollection(name).drop();
		}
	}

	@Override
	public Object get(String key) throws PageException {
		if (collectionExists(key)) return new DBCollectionImpl(db.getCollection(key), db);
		throw exp.createExpressionException("key [" + key + "] doesn't exist");
	}

	@Override
	public Object get(String key, Object defaultValue) {
		if (collectionExists(key)) return new DBCollectionImpl(db.getCollection(key), db);
		return defaultValue;
	}

	@Override
	public Object set(String key, Object value) throws PageException {
		if (collectionExists(key))
			throw exp.createExpressionException("there is already a collection [" + key + "]; drop it first");
		db.createCollection(key, buildCreateCollectionOptions(value));
		return value;
	}

	@Override
	public Object setEL(String key, Object value) {
		try { return set(key, value); } catch (Throwable t) {
			if (t instanceof ThreadDeath) throw (ThreadDeath) t;
		}
		return value;
	}

	@Override
	public Collection duplicate(boolean deepCopy) {
		throw new UnsupportedOperationException("this operation is not suppored");
	}

	@Override
	public boolean containsKey(String key) {
		return collectionExists(key);
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] args) throws PageException {

		if (methodName.equals("collectionExists")) {
			checkArgLength("collectionExists", args, 1, 1);
			return toCFML(collectionExists(caster.toString(args[0])));
		}
		if (methodName.equals("command") || methodName.equals("runCommand")) {
			checkArgLength("command", args, 1, 1);
			Document dbo = toDocument(args[0], null);
			Document result;
			if (dbo != null) result = db.runCommand(dbo);
			else result = db.runCommand(new Document(caster.toString(args[0]), 1));
			return new CommandResultImpl(result);
		}
		if (methodName.equals("createCollection")) {
			checkArgLength("createCollection", args, 1, 2);
			String name = caster.toString(args[0]);
			db.createCollection(name, buildCreateCollectionOptions(args.length > 1 ? args[1] : null));
			return toCFML(new DBCollectionImpl(db.getCollection(name), db));
		}
		if (methodName.equals("dropDatabase")) {
			checkArgLength("dropDatabase", args, 0, 0);
			db.drop();
			return null;
		}
		if (methodName.equals("getCollection")) {
			checkArgLength("getCollection", args, 1, 1);
			return toCFML(new DBCollectionImpl(db.getCollection(caster.toString(args[0])), db));
		}
		if (methodName.equals("getCollectionFromString") || methodName.equals("getCollectionNames")) {
			if (methodName.equals("getCollectionNames")) {
				checkArgLength("getCollectionNames", args, 0, 0);
				List<String> names = new ArrayList<String>();
				db.listCollectionNames().into(names);
				return toCFML(names);
			}
			checkArgLength("getCollectionFromString", args, 1, 1);
			return toCFML(new DBCollectionImpl(db.getCollection(caster.toString(args[0])), db));
		}
		if (methodName.equals("getMongo")) {
			checkArgLength("getMongo", args, 0, 0);
			return toCFML(client != null ? client.toString() : null);
		}
		if (methodName.equals("getName")) {
			checkArgLength("getName", args, 0, 0);
			return toCFML(db.getName());
		}
		if (methodName.equals("getReadPreference")) {
			checkArgLength("getReadPreference", args, 0, 0);
			return toCFML(db.getReadPreference().toString());
		}
		if (methodName.equals("getSisterDB") || methodName.equals("getSiblingDB")) {
			checkArgLength(methodName.getString(), args, 1, 1);
			if (client == null)
				throw exp.createApplicationException(methodName + "() requires a MongoClient reference; use MongoDbConnect() with a URI");
			return toCFML(new DBImpl(client.getDatabase(caster.toString(args[0])), client));
		}
		if (methodName.equals("getStats")) {
			checkArgLength("getStats", args, 0, 0);
			return new CommandResultImpl(db.runCommand(new Document("dbStats", 1)));
		}
		if (methodName.equals("getWriteConcern")) {
			checkArgLength("getWriteConcern", args, 0, 0);
			return new WriteConcernImpl(db.getWriteConcern());
		}
		if (methodName.equals("setWriteConcern")) {
			checkArgLength("setWriteConcern", args, 1, 1);
			WriteConcern wc = toWriteConcern(args[0], null);
			if (wc != null) db = db.withWriteConcern(wc);
			return null;
		}
		// Removed methods — throw informative errors
		if (methodName.equals("addUser") || methodName.equals("removeUser")) {
			throw exp.createApplicationException(methodName + "() was removed from the MongoDB Java driver 5.x. Manage users via the MongoDB shell or admin commands.");
		}
		if (methodName.equals("eval")) {
			throw exp.createApplicationException("eval() was removed from MongoDB 4.2+. Use aggregation pipelines instead.");
		}
		if (methodName.equals("addOption") || methodName.equals("getOptions") ||
			methodName.equals("setOptions") || methodName.equals("resetOptions")) {
			throw exp.createApplicationException(methodName + "() was removed from the MongoDB Java driver 5.x.");
		}

		String supportedFunctions = "collectionExists,command,createCollection,dropDatabase,getCollection," +
			"getCollectionFromString,getCollectionNames,getMongo,getName,getReadPreference,getSisterDB,getStats,getWriteConcern,setWriteConcern";
		throw exp.createExpressionException("function [" + methodName + "] is not supported, supported functions are [" + supportedFunctions + "]");
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		if (args.isEmpty()) return call(pc, methodName, new Object[0]);
		throw new UnsupportedOperationException("named arguments are not supported yet!");
	}

	/**
	 * Convert a CFML options struct to a fully-populated {@link CreateCollectionOptions}.
	 *
	 * Supported options:
	 *   capped (boolean), size (long), max (long)            — capped collection
	 *   validator (struct/document)                          — JSON Schema validator
	 *   validationLevel  "off" | "moderate" | "strict"       — default: strict
	 *   validationAction "warn" | "error"                    — default: error
	 *   timeseries.timeField (string, required for TS)       — time series
	 *   timeseries.metaField (string)
	 *   timeseries.granularity "seconds" | "minutes" | "hours"
	 *   expireAfterSeconds (long)                            — TTL
	 */
	private com.mongodb.client.model.CreateCollectionOptions buildCreateCollectionOptions(Object obj) throws PageException {
		com.mongodb.client.model.CreateCollectionOptions cco = new com.mongodb.client.model.CreateCollectionOptions();
		if (obj == null) return cco;
		Struct sct = caster.toStruct(obj, null);
		if (sct == null) return cco;

		// --- capped collection ---
		Boolean capped = caster.toBoolean(sct.get("capped", null), null);
		if (capped != null) cco.capped(capped);
		Integer size = caster.toInteger(sct.get("size", null), null);
		if (size != null) cco.sizeInBytes(size.longValue());
		Integer max = caster.toInteger(sct.get("max", null), null);
		if (max != null) cco.maxDocuments(max.longValue());

		// --- JSON Schema validator ---
		Object validatorObj = sct.get("validator", null);
		if (validatorObj != null) {
			Document validatorDoc = toDocument(validatorObj, null);
			if (validatorDoc != null) {
				com.mongodb.client.model.ValidationOptions vo =
					new com.mongodb.client.model.ValidationOptions().validator(validatorDoc);
				String levelStr = caster.toString(sct.get("validationLevel", null), null);
				if (levelStr != null) {
					switch (levelStr.toLowerCase()) {
						case "off":      vo.validationLevel(com.mongodb.client.model.ValidationLevel.OFF);      break;
						case "moderate": vo.validationLevel(com.mongodb.client.model.ValidationLevel.MODERATE); break;
						default:         vo.validationLevel(com.mongodb.client.model.ValidationLevel.STRICT);   break;
					}
				}
				String actionStr = caster.toString(sct.get("validationAction", null), null);
				if (actionStr != null) {
					if ("warn".equalsIgnoreCase(actionStr))
						vo.validationAction(com.mongodb.client.model.ValidationAction.WARN);
					else
						vo.validationAction(com.mongodb.client.model.ValidationAction.ERROR);
				}
				cco.validationOptions(vo);
			}
		}

		// --- time series ---
		Object tsObj = sct.get("timeseries", null);
		if (tsObj != null) {
			Struct tsSct = caster.toStruct(tsObj, null);
			if (tsSct != null) {
				String timeField = caster.toString(tsSct.get("timeField", null), null);
				if (timeField != null) {
					com.mongodb.client.model.TimeSeriesOptions tso =
						new com.mongodb.client.model.TimeSeriesOptions(timeField);
					String metaField = caster.toString(tsSct.get("metaField", null), null);
					if (metaField != null) tso.metaField(metaField);
					String gran = caster.toString(tsSct.get("granularity", null), null);
					if (gran != null) {
						switch (gran.toLowerCase()) {
							case "minutes": tso.granularity(com.mongodb.client.model.TimeSeriesGranularity.MINUTES); break;
							case "hours":   tso.granularity(com.mongodb.client.model.TimeSeriesGranularity.HOURS);   break;
							default:        tso.granularity(com.mongodb.client.model.TimeSeriesGranularity.SECONDS); break;
						}
					}
					cco.timeSeriesOptions(tso);
				}
			}
		}

		// --- TTL (works for both time series and standard collections) ---
		Object expireObj = sct.get("expireAfterSeconds", null);
		if (expireObj != null)
			cco.expireAfter(caster.toLongValue(expireObj), TimeUnit.SECONDS);

		return cco;
	}

	public MongoDatabase getDatabase() {
		return db;
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties dp) {
		DumpTable table = new DumpTable("struct", "#339933", "#8e714e", "#000000");
		table.setTitle("DB");
		table.setComment("http://docs.mongodb.org/manual/reference/command/");
		table.appendRow(1,
			__toDumpData("name", pageContext, maxlevel, dp),
			__toDumpData(db.getName(), pageContext, maxlevel, dp));
		List<String> names = new ArrayList<String>();
		db.listCollectionNames().into(names);
		names.sort(null);
		table.appendRow(1,
			__toDumpData("collections", pageContext, maxlevel, dp),
			__toDumpData(String.join(", ", names), pageContext, maxlevel, dp));
		return table;
	}
}
