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
package org.apache.bigtop.datagenerators.bigpetstore.generators.products.cartesian;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class CartesianProductField<T> implements CartesianProduct<T>
{
	String fieldName;
	Iterator<T> fieldValuesIterator;
	Collection<T> fieldValues;
	CartesianProduct<T> previous;
	Map<String, T> baseValue;

	public CartesianProductField(String fieldName, Collection<T> fieldValues, CartesianProduct<T> previous)
	{
		this.fieldValues = fieldValues;
		this.fieldName = fieldName;
		this.previous = previous;
	}

	@Override
	public boolean hasNext()
	{
		return previous.hasNext() || (fieldValuesIterator != null && fieldValuesIterator.hasNext());
	}

	@Override
	public Map<String, T> next()
	{
		if(fieldValuesIterator != null)
		{
			Map<String, T> map = new HashMap<String, T>(baseValue);
			map.put(fieldName, fieldValuesIterator.next());

			if(!fieldValuesIterator.hasNext())
			{
				fieldValuesIterator = null;
			}

			return map;
		} else if(previous.hasNext())
		{
			baseValue = previous.next();
			fieldValuesIterator = fieldValues.iterator();

			Map<String, T> map = new HashMap<String, T>(baseValue);
			map.put(fieldName, fieldValuesIterator.next());

			return map;
		}

		throw new NoSuchElementException();
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("CartesianProductBase does not support remove()");
	}

}
