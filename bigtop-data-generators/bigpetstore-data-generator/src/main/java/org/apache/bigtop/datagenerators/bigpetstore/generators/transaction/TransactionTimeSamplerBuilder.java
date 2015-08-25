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
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.StatefulMonteCarloSampler;

public class TransactionTimeSamplerBuilder
{
	private final SeedFactory seedFactory;
	private CustomerInventory customerInventory;
	private CustomerTransactionParameters transactionParameters;

	public TransactionTimeSamplerBuilder(SeedFactory seedFactory)
	{
		this.seedFactory = seedFactory;
	}

	public void setCustomerInventory(CustomerInventory inventory)
	{
		this.customerInventory = inventory;
	}

	public void setCustomerTransactionParameters(CustomerTransactionParameters parameters)
	{
		this.transactionParameters = parameters;
	}

	public Sampler<Double> build()
	{
		double lambda = 1.0 / transactionParameters.getAverageTransactionTriggerTime();
		Sampler<Double> arrivalTimeSampler = new ExponentialSampler(lambda, seedFactory);
		Sampler<Double> proposedTimeSampler = new ProposedPurchaseTimeSampler(customerInventory,
				arrivalTimeSampler);

		return new StatefulMonteCarloSampler<Double>(proposedTimeSampler,
				new TransactionTimePDF(),
				0.0,
				seedFactory);
	}
}
