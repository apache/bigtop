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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.generators.products.cartesian.CartesianProductBase;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.cartesian.CartesianProductField;
import org.junit.Assert;
import org.junit.Test;

public class TestCartesianProductField
{

	@Test
	public void testTwoLevels()
	{
		Iterator<Map<String, String>> iter = new CartesianProductField<String>(
				"children", Arrays.asList("1", "2"),
					new CartesianProductBase<String>(
							"animal", Arrays.asList("cat", "dog")));

		Assert.assertEquals(iter.hasNext(), true);
		Map<String, String> map = iter.next();
		Assert.assertEquals(map.size(), 2);
		Assert.assertTrue(map.containsKey("animal"));
		Assert.assertTrue(map.containsKey("children"));
		Assert.assertEquals(map.get("animal"), "cat");
		Assert.assertEquals(map.get("children"), "1");

		Assert.assertEquals(iter.hasNext(), true);
		map = iter.next();
		Assert.assertEquals(map.size(), 2);
		Assert.assertTrue(map.containsKey("animal"));
		Assert.assertTrue(map.containsKey("children"));
		Assert.assertEquals(map.get("animal"), "cat");
		Assert.assertEquals(map.get("children"), "2");

		Assert.assertEquals(iter.hasNext(), true);
		map = iter.next();
		Assert.assertEquals(map.size(), 2);
		Assert.assertTrue(map.containsKey("animal"));
		Assert.assertTrue(map.containsKey("children"));
		Assert.assertEquals(map.get("animal"), "dog");
		Assert.assertEquals(map.get("children"), "1");

		Assert.assertEquals(iter.hasNext(), true);
		map = iter.next();
		Assert.assertEquals(map.size(), 2);
		Assert.assertTrue(map.containsKey("animal"));
		Assert.assertTrue(map.containsKey("children"));
		Assert.assertEquals(map.get("animal"), "dog");
		Assert.assertEquals(map.get("children"), "2");

		Assert.assertFalse(iter.hasNext());
		Assert.assertFalse(iter.hasNext());
		Assert.assertFalse(iter.hasNext());
	}
}
