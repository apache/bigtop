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

import java.util.Collection;
import java.util.List;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Customer;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Transaction;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.purchase.PurchasingModel;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.SequenceSampler;

public class TransactionSamplerBuilder
{
	private final Collection<ProductCategory> productCategories;
	private final Customer customer;
	private final PurchasingModel purchasingProfile;
	private final SeedFactory seedFactory;

	CustomerTransactionParameters parameters;
	CustomerInventory inventory;

	public TransactionSamplerBuilder(Collection<ProductCategory> productCategories,
			Customer customer,
			PurchasingModel purchasingProfile,
			SeedFactory seedFactory) throws Exception
	{
		this.customer = customer;
		this.seedFactory = seedFactory;
		this.purchasingProfile = purchasingProfile;
		this.productCategories = productCategories;
	}

	protected void buildParameters() throws Exception
	{
		CustomerTransactionParametersSamplerBuilder builder = new CustomerTransactionParametersSamplerBuilder(seedFactory);
		parameters = builder.build().sample();
	}

	protected ConditionalSampler<List<Product>, Double> buildPurchasesSampler() throws Exception
	{
		TransactionPurchasesSamplerBuilder builder = new TransactionPurchasesSamplerBuilder(productCategories,
				purchasingProfile, seedFactory);

		builder.setTransactionParameters(parameters);
		builder.setInventory(inventory);

		return builder.build();
	}

	protected Sampler<Double> buildTimeSampler()
	{
		TransactionTimeSamplerBuilder builder = new TransactionTimeSamplerBuilder(seedFactory);
		builder.setCustomerTransactionParameters(parameters);
		builder.setCustomerInventory(inventory);

		return builder.build();
	}

	protected void buildCustomerInventory()
	{
		CustomerInventoryBuilder inventoryBuilder = new CustomerInventoryBuilder(parameters,
				seedFactory);
		inventoryBuilder.addAllProductCategories(productCategories);
		inventory = inventoryBuilder.build();
	}

	public Sampler<Transaction> build() throws Exception
	{
		buildParameters();
		buildCustomerInventory();

		Sampler<Double> timeSampler = buildTimeSampler();

		return new TransactionSampler(customer, timeSampler, buildPurchasesSampler(), new SequenceSampler());
	}
}
