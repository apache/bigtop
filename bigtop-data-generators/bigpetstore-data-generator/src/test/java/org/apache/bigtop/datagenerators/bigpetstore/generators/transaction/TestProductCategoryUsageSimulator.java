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
package org.apache.bigtop.datagenerators.bigpetstore.generators.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.ProductCategoryUsageSimulator;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.ProductCategoryUsageTrajectory;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TestProductCategoryUsageSimulator
{

	@Test
	public void testSimulate() throws Exception
	{
		SeedFactory seedFactory = new SeedFactory(1234);

		ProductCategoryUsageSimulator simulator = new ProductCategoryUsageSimulator(2.0, 1.0, 1.0, seedFactory);

		ProductCategoryUsageTrajectory trajectory = simulator.simulate(0.0, 30.0);

		assertEquals(0.0, trajectory.getLastAmount(), 0.0001);

		Pair<Double, Double> previousEntry = trajectory.getStep(0);
		for(int i = 1; i < trajectory.size(); i++)
		{
			Pair<Double, Double> entry = trajectory.getStep(i);
			// time should move forward
			assertTrue(previousEntry.getLeft() <= entry.getLeft());
			// remaining amounts should go down
			assertTrue(previousEntry.getRight() >= entry.getRight());
			previousEntry = entry;
		}
	}

}
