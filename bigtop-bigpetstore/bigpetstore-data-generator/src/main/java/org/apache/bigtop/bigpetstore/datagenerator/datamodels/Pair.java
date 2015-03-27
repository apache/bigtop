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
package org.apache.bigtop.bigpetstore.datagenerator.datamodels;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

public class Pair<A, B> implements Serializable
{
	private static final long serialVersionUID = -105021821052665898L;
	
	A first;
	B second;
	
	public Pair(A first, B second)
	{
		this.first = first;
		this.second = second;
	}

	public A getFirst()
	{
		return first;
	}

	public B getSecond()
	{
		return second;
	}
	
	public static <A, B> Pair<A, B> create(A first, B second)
	{
		return new Pair<A, B>(first, second);
	}
	
	public static <A, B> List<Pair<A, B>> create(Map<A, B> map)
	{
		List<Pair<A, B>> list = Lists.newArrayListWithExpectedSize(map.size());
		for(Map.Entry<A, B> entry : map.entrySet())
			list.add(Pair.create(entry.getKey(), entry.getValue()));
		return list;
	}
	
	public String toString()
	{
		return "Pair(" + first + ", " + second + ")";
	}
}
