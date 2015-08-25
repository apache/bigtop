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

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

public class ProductCategoryUsageTrajectory
{
	final private List<Pair<Double, Double>> trajectory;

	public ProductCategoryUsageTrajectory(double initialTime, double initialAmount)
	{
		trajectory = Lists.newArrayList();
		this.append(initialTime, initialAmount);
	}

	public void append(double time, double amount)
	{
		trajectory.add(Pair.of(time, amount));
	}

	public double getLastAmount()
	{
		return trajectory.get(trajectory.size() - 1).getValue();
	}

	public double getLastTime()
	{
		return trajectory.get(trajectory.size() - 1).getKey();
	}

	public double amountAtTime(double time)
	{
		Pair<Double, Double> previous = null;
		for(Pair<Double, Double> entry : trajectory)
		{
			if(entry.getKey() > time)
				break;
			previous = entry;
		}

		if(previous == null)
			return 0.0;

		return previous.getValue();
	}

	public Pair<Double, Double> getStep(int idx)
	{
		return trajectory.get(idx);
	}

	public int size()
	{
		return trajectory.size();
	}
}
