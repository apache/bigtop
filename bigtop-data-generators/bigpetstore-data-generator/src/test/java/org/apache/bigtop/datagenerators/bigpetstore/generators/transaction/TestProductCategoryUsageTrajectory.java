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

import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.ProductCategoryUsageTrajectory;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TestProductCategoryUsageTrajectory
{

	@Test
	public void testTrajectory()
	{
		double initialAmount = 30.0;
		double initialTime = 0.0;

		ProductCategoryUsageTrajectory trajectory = new ProductCategoryUsageTrajectory(initialTime, initialAmount);

		assertEquals(trajectory.size(), 1);

		Pair<Double, Double> entry = trajectory.getStep(0);
		assertEquals(initialTime, entry.getLeft(), 0.0001);
		assertEquals(initialAmount, entry.getRight(), 0.0001);

		trajectory.append(1.0, 25.0);

		assertEquals(2, trajectory.size());

		entry = trajectory.getStep(1);
		assertEquals(1.0, entry.getLeft(), 0.0001);
		assertEquals(25.0, entry.getRight(), 0.0001);

		assertEquals(1.0, trajectory.getLastTime(), 0.0001);
		assertEquals(25.0, trajectory.getLastAmount(), 0.0001);
	}

	@Test
	public void testAmountAtTime()
	{
		ProductCategoryUsageTrajectory trajectory = new ProductCategoryUsageTrajectory(0.0, 30.0);
		trajectory.append(1.0, 25.0);
		trajectory.append(2.0, 20.0);
		trajectory.append(3.0, 50.0);
		trajectory.append(4.0, 40.0);
		trajectory.append(4.0, 50.0);
		trajectory.append(5.0, 30.0);

		assertEquals(30.0, trajectory.amountAtTime(0.5), 0.0001);
		assertEquals(50.0, trajectory.amountAtTime(4.0), 0.0001);
		assertEquals(30.0, trajectory.amountAtTime(10.0), 0.0001);
	}


}
