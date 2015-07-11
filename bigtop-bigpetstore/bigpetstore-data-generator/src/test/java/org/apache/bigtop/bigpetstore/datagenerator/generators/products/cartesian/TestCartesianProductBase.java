package org.apache.bigtop.bigpetstore.datagenerator.generators.products.cartesian;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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
