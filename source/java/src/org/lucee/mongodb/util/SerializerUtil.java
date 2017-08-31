/**
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
 **/
package org.lucee.mongodb.util;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;

public class SerializerUtil {
	
	private static Class<?> clazz;
	private static Method deser;
	private static Method ser;

	public static Class<?> loadClass() throws IOException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		return engine.getClassUtil().loadClass("lucee.runtime.converter.JavaConverter");
	}
	
	public static String serialize(Object o) throws Exception {
		if(clazz==null) clazz=loadClass();
		
		if(ser==null) ser=clazz.getMethod("serialize", new Class[]{Serializable.class});
		try {
			return (String)ser.invoke(null, new Object[]{o});
		}
		catch(InvocationTargetException ite) {
			Throwable t = ite.getTargetException();
			if(t instanceof Exception) throw (Exception)t;
			throw new RuntimeException(t);
		}
		/* //ObjectSave
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ObjectOutputStream oos=null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
		}
		finally {
			Util.closeEL(oos);
			Util.closeEL(baos);
		}
		return Base64Encoder.encode(baos.toByteArray());*/
	}
	
	public static Object evaluate(String str) throws Exception {
		if(clazz==null) clazz=loadClass();
		if(deser==null) deser=clazz.getMethod("deserialize", new Class[]{String.class});
		
		try {
			return deser.invoke(null, new Object[]{str});
		}
		catch(InvocationTargetException ite) {
			Throwable t = ite.getTargetException();
			if(t instanceof Exception) throw (Exception)t;
			throw new RuntimeException(t);
		}
        /*ByteArrayInputStream bais = new ByteArrayInputStream(Base64Encoder.decode(ser));
		ObjectInputStream ois=null;
        Object o=null;
        try {
	        ois = new ObjectInputStream(bais);
	        o=ois.readObject();
        }
        finally {
        	Util.closeEL(ois);
        }
        return o;*/
    }

}
