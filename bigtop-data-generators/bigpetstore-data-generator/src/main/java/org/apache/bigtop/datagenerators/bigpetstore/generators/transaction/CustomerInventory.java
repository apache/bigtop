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

import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class CustomerInventory
{
	final private ImmutableMap<String, ProductCategoryInventory> productCategoryInventories;

	public CustomerInventory(Map<String, ProductCategoryInventory> productCategoryInventories)
	{
		this.productCategoryInventories = ImmutableMap.copyOf(productCategoryInventories);
	}

	public void simulatePurchase(double time, Product product) throws Exception
	{
		String category = product.getFieldValueAsString(Constants.PRODUCT_CATEGORY);
		ProductCategoryInventory inventory = productCategoryInventories.get(category);
		inventory.simulatePurchase(time, product);
	}

	public ImmutableMap<String, Double> getInventoryAmounts(double time)
	{
		Map<String, Double> amounts = Maps.newHashMap();
		for(String category : productCategoryInventories.keySet())
		{
			double amount = productCategoryInventories.get(category).findRemainingAmount(time);
			amounts.put(category, amount);
		}

		return ImmutableMap.copyOf(amounts);
	}

	public ImmutableMap<String, Double> getExhaustionTimes()
	{
		Map<String, Double> times = Maps.newHashMap();
		for(String category : productCategoryInventories.keySet())
		{
			double time = productCategoryInventories.get(category).findExhaustionTime();
			times.put(category, time);
		}

		return ImmutableMap.copyOf(times);
	}
}
