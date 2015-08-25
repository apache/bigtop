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
import org.junit.Assert;
import org.junit.Test;

public class TestCartesianProductBase
{
	@Test
	public void testNext()
	{
		Iterator<Map<String, Double>> iter = new CartesianProductBase<Double>("count",
				Arrays.asList(1.0, 2.0, 3.0));

		Assert.assertTrue(iter.hasNext());

		Map<String, Double> map = iter.next();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsKey("count"));
		Assert.assertEquals( (double) map.get("count"), (double) 1.0, 0.0001);

		Assert.assertTrue(iter.hasNext());

		map = iter.next();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsKey("count"));
		Assert.assertEquals( (double) map.get("count"), (double) 2.0, 0.0001);

		Assert.assertTrue(iter.hasNext());

		map = iter.next();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsKey("count"));
		Assert.assertEquals( (double) map.get("count"), (double) 3.0, 0.0001);

		Assert.assertFalse(iter.hasNext());
	}
}
