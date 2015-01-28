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

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.dump.DumpData;

public class SimpleDumpData implements DumpData {

	private String data;

	public SimpleDumpData(String data) {
		this.data=data;
	}
	public SimpleDumpData(double data) {
		this.data=CFMLEngineFactory.getInstance().getCastUtil().toString(data);
	}

	public SimpleDumpData(boolean data) {
		this.data=CFMLEngineFactory.getInstance().getCastUtil().toString(data);
	}
	
	@Override
	public String toString() {
		return data;
	}
}
