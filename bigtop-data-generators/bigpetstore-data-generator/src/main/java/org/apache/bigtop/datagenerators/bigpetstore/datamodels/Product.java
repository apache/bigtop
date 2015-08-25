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
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Product implements Serializable
{
	private static final long serialVersionUID = 4519472063058037956L;

	ImmutableMap<String, Object> fields;

	public Product(Map<String, Object> fields)
	{
		this.fields = ImmutableMap.copyOf(fields);
	}

	public ImmutableSet<String> getFieldNames()
	{
		return fields.keySet();
	}

	public Object getFieldValue(String fieldName)
	{
		return fields.get(fieldName);
	}

	public String getFieldValueAsString(String fieldName)
	{
		return fields.get(fieldName).toString();
	}

	public Double getFieldValueAsDouble(String fieldName)
	{
		Object value = getFieldValue(fieldName);
		try
		{
			Double doubleValue = (Double) value;
			return doubleValue;
		}
		catch(ClassCastException e)
		{
			return null;
		}
	}

	public Long getFieldValueAsLong(String fieldName)
	{
		Object value = getFieldValue(fieldName);
		try
		{
			Long longValue = (Long) value;
			return longValue;
		}
		catch(ClassCastException e)
		{
			try
			{
				Integer intValue = (Integer) value;
				return new Long(intValue);
			}
			catch(ClassCastException f)
			{
				return null;
			}
		}
	}

	public String toString()
	{
		String str = "";
		for(Map.Entry<String, Object> entry : fields.entrySet())
		{
			str += entry.getKey() + "=" + entry.getValue() + ";";
		}

		return str;
	}
}
