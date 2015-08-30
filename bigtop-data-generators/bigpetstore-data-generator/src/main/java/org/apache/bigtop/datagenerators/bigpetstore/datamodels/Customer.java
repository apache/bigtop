/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.datagenerators.bigpetstore.datamodels;

import java.io.Serializable;

import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.commons.lang3.tuple.Pair;

public class Customer implements Serializable
{
	private static final long serialVersionUID = 5739806281335931258L;

	int id;
	Pair<String, String> name;
	Location location;
	Store store;

	public Customer(int id, Pair<String, String> name, Store store, Location location)
	{
		this.id = id;
		this.name = name;
		this.location = location;
		this.store = store;
	}

	public int getId()
	{
		return id;
	}

	public Pair<String, String> getName()
	{
		return name;
	}

	public Location getLocation()
	{
		return location;
	}

	public Store getStore()
	{
		return store;
	}
}
