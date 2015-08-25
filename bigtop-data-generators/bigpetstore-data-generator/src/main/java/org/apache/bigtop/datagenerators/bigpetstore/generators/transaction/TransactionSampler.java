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

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Customer;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Transaction;
import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

public class TransactionSampler implements Sampler<Transaction>
{
	private final Sampler<Double> timeSampler;
	private final ConditionalSampler<List<Product>, Double> purchasesSampler;
	private final Sampler<Integer> idSampler;
	private final Customer customer;

	public TransactionSampler(Customer customer, Sampler<Double> timeSampler,
			ConditionalSampler<List<Product>, Double> purchasesSampler,
			Sampler<Integer> idSampler)
	{
		this.timeSampler = timeSampler;
		this.customer = customer;
		this.purchasesSampler = purchasesSampler;
		this.idSampler = idSampler;
	}


	public Transaction sample() throws Exception
	{
		Double transactionTime = timeSampler.sample();
		List<Product> purchase = purchasesSampler.sample(transactionTime);
		Integer id = idSampler.sample();

		Transaction transaction = new Transaction(id, customer, customer.getStore(),
				transactionTime, purchase);

		return transaction;
	}

}
