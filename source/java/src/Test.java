import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Set;

import org.opencfmlfoundation.mongodb.DBImpl;
import org.opencfmlfoundation.mongodb.util.aprint;
import org.opencfmlfoundation.mongodb.util.print;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;


public class Test {
	public static void main(String[] args) throws UnknownHostException {
		
		
		MongoClient client = new MongoClient("localhost", 27017);
		
		// databases
		System.out.println("--- databases ---");
		for (String s : client.getDatabaseNames()) {
			System.out.println(s);
		}
		
		DB db = client.getDB("test");

		System.out.println(db.collectionExists("test"));
		System.out.println(db.collectionExists("Test"));
		
		// collection names
		System.out.println("--- collection names ---");
		
		Set<String> colls = db.getCollectionNames();
		for (String s : colls) {
			System.out.println(s);
		}

		aprint.e(db.collectionExists("test"));
		aprint.e(db.collectionExists("TEST"));
		aprint.e(db.collectionExists("Test"));
		aprint.e(db.collectionExists("ttest"));
		aprint.e(db.getCollection("test").count());
		aprint.e(db.getCollection("Test").count());
		aprint.e(db.getCollectionFromString("test").count());
		aprint.e(db.getCollectionFromString("Test").count());
		
		if(true) return;
		
		DBCollection coll = db.getCollection("test");
		DBObject myDoc = coll.findOne();
		System.out.println(myDoc);
		
		BasicDBObject doc = new BasicDBObject("name", "MongoDB").
        append("type", "database").
        append("count", 1)
       .append("info", new BasicDBObject("x", 203).append("y", 102));
		coll.insert(doc);
		
		DBCursor cur = coll.find();
		while(cur.hasNext()){
			System.out.println(cur.next());
		}
		
		coll = db.getCollection("TEST");
		myDoc = coll.findOne();
		System.out.println(myDoc);
		
		doc = new BasicDBObject("name", "MongoDB").
        append("type", "database").
        append("count", 1)
       .append("info", new BasicDBObject("x", 203).append("y", 102));
		coll.insert(doc);
		
		
	}
}
