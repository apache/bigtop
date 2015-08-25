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
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.wfs.ConditionalWeightFunction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TransactionPurchasesHiddenMarkovModel implements ConditionalSampler<List<Product>, Double>
{

	protected final static String STOP_STATE = "STOP";

	final ConditionalSampler<Product, String> purchasingProcesses;
	final ConditionalWeightFunction<Double, Double> categoryWF;
	final CustomerInventory inventory;

	final SeedFactory seedFactory;

	public TransactionPurchasesHiddenMarkovModel(ConditionalSampler<Product, String> purchasingProcesses,
			ConditionalWeightFunction<Double, Double> categoryWF, CustomerInventory inventory,
				SeedFactory seedFactory)
	{
		this.purchasingProcesses = purchasingProcesses;
		this.inventory = inventory;
		this.categoryWF = categoryWF;

		this.seedFactory = seedFactory;
	}

	protected String chooseCategory(double transactionTime, int numPurchases) throws Exception
	{
		ImmutableMap<String, Double> exhaustionTimes = this.inventory.getExhaustionTimes();
		Map<String, Double> weights = Maps.newHashMap();

		for(Map.Entry<String, Double> entry : exhaustionTimes.entrySet())
		{
			String category = entry.getKey();
			double weight = this.categoryWF.weight(entry.getValue(), transactionTime);
			weights.put(category, weight);
		}

		if(numPurchases > 0)
		{
			weights.put(STOP_STATE, Constants.STOP_CATEGORY_WEIGHT);
		}

		Sampler<String> sampler = RouletteWheelSampler.create(weights, seedFactory);

		return sampler.sample();
	}

	protected Product chooseProduct(String category) throws Exception
	{
		return this.purchasingProcesses.sample(category);
	}

	public List<Product> sample(Double transactionTime) throws Exception
	{
		int numPurchases = 0;

		List<Product> purchasedProducts = Lists.newArrayList();

		String category;
		while(true)
		{
			category = this.chooseCategory(transactionTime, numPurchases);

			if(category.equals(STOP_STATE))
			{
				break;
			}

			Product product = this.chooseProduct(category);

			purchasedProducts.add(product);

			this.inventory.simulatePurchase(transactionTime, product);
			numPurchases += 1;
		}

		return purchasedProducts;
	}
}
