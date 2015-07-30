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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import lucee.loader.util.Util;

public class SerializerUtil {
	public static String serialize(Object o) throws IOException {
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
		return Base64Encoder.encode(baos.toByteArray());
	}
	
	public static Object evaluate(String ser) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64Encoder.decode(ser));
		ObjectInputStream ois=null;
        Object o=null;
        try {
	        ois = new ObjectInputStream(bais);
	        o=ois.readObject();
        }
        finally {
        	Util.closeEL(ois);
        }
        return o;
    }

}
