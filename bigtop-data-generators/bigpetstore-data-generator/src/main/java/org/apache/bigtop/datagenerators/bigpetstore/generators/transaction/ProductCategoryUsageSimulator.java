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

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.ExponentialSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.GaussianSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

public class ProductCategoryUsageSimulator
{
	final private double amountUsedAverage;
	final private double amountUsedVariance;

	final private Sampler<Double> timestepSampler;
	final private Sampler<Double> R;

	public ProductCategoryUsageSimulator(double dailyUsageRate, double amountUsedAverage,
			double amountUsedVariance, SeedFactory seedFactory)
	{
		this.amountUsedAverage = amountUsedAverage;
		this.amountUsedVariance = amountUsedVariance;

		timestepSampler = new ExponentialSampler(dailyUsageRate, seedFactory);
		R = new GaussianSampler(0.0, 1.0, seedFactory);
	}

	private void step(ProductCategoryUsageTrajectory trajectory) throws Exception
	{
		// given in days since last usage
		double timestep = timestepSampler.sample();

		double r = R.sample();

		// given in units per day
		double usageAmount = this.amountUsedAverage * timestep +
				Math.sqrt(this.amountUsedVariance * timestep) * r;

		// can't use a negative amount
		usageAmount = Math.max(usageAmount, 0.0);

		double remainingAmount = Math.max(0.0, trajectory.getLastAmount() - usageAmount);
		double time = trajectory.getLastTime() + timestep;

		trajectory.append(time, remainingAmount);
	}

	public ProductCategoryUsageTrajectory simulate(double initialTime, double initialAmount) throws Exception
	{
		ProductCategoryUsageTrajectory trajectory = new ProductCategoryUsageTrajectory(initialTime, initialAmount);

		while(trajectory.getLastAmount() > 0.0)
		{
			step(trajectory);
		}

		return trajectory;
	}
}
