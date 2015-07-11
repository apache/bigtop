package org.apache.bigtop.bigpetstore.datagenerator.generators.products.cartesian;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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
