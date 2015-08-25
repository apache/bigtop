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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.junit.Test;

import com.google.common.collect.Maps;

public class TestProduct
{

	@Test
	public void testString()
	{
		Map<String, Object> fields = Maps.newHashMap();
		fields.put(Constants.PRODUCT_CATEGORY, "poop bags");
		fields.put(Constants.PRODUCT_QUANTITY, 120);
		fields.put("price", 12.80);

		Product product = new Product(fields);

		assertEquals(product.getFieldValueAsString(Constants.PRODUCT_CATEGORY), "poop bags");
		assertEquals(product.getFieldValueAsString("price"), "12.8");
		assertEquals(product.getFieldValueAsString(Constants.PRODUCT_QUANTITY), "120");
	}

	@Test
	public void testDouble()
	{
		Map<String, Object> fields = Maps.newHashMap();
		fields.put(Constants.PRODUCT_CATEGORY, "poop bags");
		fields.put(Constants.PRODUCT_QUANTITY, 120);
		fields.put("price", 12.80);

		Product product = new Product(fields);

		assertNull(product.getFieldValueAsDouble(Constants.PRODUCT_CATEGORY));
		assertEquals(product.getFieldValueAsDouble("price"), 12.80, 1e-5);
		assertNull(product.getFieldValueAsDouble(Constants.PRODUCT_QUANTITY));
	}

	@Test
	public void testLong()
	{
		Map<String, Object> fields = Maps.newHashMap();
		fields.put(Constants.PRODUCT_CATEGORY, "poop bags");
		fields.put(Constants.PRODUCT_QUANTITY, 120);
		fields.put("price", 12.80);

		Product product = new Product(fields);

		assertNull(product.getFieldValueAsLong(Constants.PRODUCT_CATEGORY));
		assertNull(product.getFieldValueAsLong("price"));
		assertEquals((long) product.getFieldValueAsLong(Constants.PRODUCT_QUANTITY), 120L);
	}

}
