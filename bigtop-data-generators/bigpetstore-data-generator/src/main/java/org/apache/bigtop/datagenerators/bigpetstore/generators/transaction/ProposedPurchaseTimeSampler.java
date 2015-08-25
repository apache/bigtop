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

import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

public class ProposedPurchaseTimeSampler implements Sampler<Double>
{
	final CustomerInventory customerInventory;
	final Sampler<Double> arrivalTimeSampler;

	public ProposedPurchaseTimeSampler(CustomerInventory customerInventory,
			Sampler<Double> arrivalTimeSampler)
	{
		this.customerInventory = customerInventory;
		this.arrivalTimeSampler = arrivalTimeSampler;
	}

	protected double categoryProposedTime(double exhaustionTime) throws Exception
	{
		return Math.max(exhaustionTime - arrivalTimeSampler.sample(), 0.0);
	}

	public Double sample() throws Exception
	{
		double minProposedTime = Double.MAX_VALUE;
		for(Double exhaustionTime : this.customerInventory.getExhaustionTimes().values())
		{
			double proposedTime = this.categoryProposedTime(exhaustionTime);
			minProposedTime = Math.min(proposedTime, minProposedTime);
		}

		return minProposedTime;
	}

}
