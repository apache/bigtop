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
package org.apache.bigtop.bigpetstore.datagenerator.generators.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.bigtop.bigpetstore.datagenerator.Constants;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.PetSpecies;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Product;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;
import org.apache.bigtop.bigpetstore.datagenerator.generators.products.ProductCategoryBuilder;
import org.apache.bigtop.bigpetstore.datagenerator.generators.transaction.CustomerInventory;
import org.apache.bigtop.bigpetstore.datagenerator.generators.transaction.CustomerTransactionParameters;
import org.apache.bigtop.bigpetstore.datagenerator.generators.transaction.CustomerTransactionParametersSamplerBuilder;
import org.apache.bigtop.bigpetstore.datagenerator.generators.transaction.ProductCategoryInventory;
import org.junit.Test;

import com.google.common.collect.Maps;

public class TestCustomerInventory
{
	
	@Test
	public void testPurchase() throws Exception
	{
		SeedFactory seedFactory = new SeedFactory(1234);
		
		CustomerTransactionParametersSamplerBuilder transParamsBuilder = new CustomerTransactionParametersSamplerBuilder(seedFactory);
		Sampler<CustomerTransactionParameters> sampler = transParamsBuilder.build();
		
		CustomerTransactionParameters parameters = sampler.sample();
		
		ProductCategoryBuilder builder = new ProductCategoryBuilder();
		builder.addApplicableSpecies(PetSpecies.DOG);
		builder.setAmountUsedPetPetAverage(1.0);
		builder.setAmountUsedPetPetVariance(1.0);
		builder.setDailyUsageRate(2.0);
		builder.setCategory("dog food");
		
		
		ProductCategory category = builder.build();
		
		ProductCategoryInventory productInventory = new ProductCategoryInventory(category, parameters, seedFactory);
		
		Map<String, ProductCategoryInventory> inventories = Maps.newHashMap();
		inventories.put("dog food", productInventory);
		
		CustomerInventory inventory = new CustomerInventory(inventories);
		
		for(Map.Entry<String, Double> entry : inventory.getExhaustionTimes().entrySet())
		{
			assertEquals(entry.getValue(), 0.0, 0.0001);
		}
		
		for(Map.Entry<String, Double> entry : inventory.getInventoryAmounts(0.0).entrySet())
		{
			assertEquals(entry.getValue(), 0.0, 0.0001);
		}
		
		Map<String, Object> fields = Maps.newHashMap();
		fields.put(Constants.PRODUCT_CATEGORY, "dog food");
		fields.put(Constants.PRODUCT_QUANTITY, 30.0);
		Product product = new Product(fields);
		
		inventory.simulatePurchase(1.0, product);
		
		Map<String, Double> exhaustionTimes = inventory.getExhaustionTimes();
		assertTrue(exhaustionTimes.containsKey("dog food"));
		assertTrue(exhaustionTimes.get("dog food") > 0.0);
		
		Map<String, Double> amounts = inventory.getInventoryAmounts(2.0);
		assertTrue(amounts.containsKey("dog food"));
		assertTrue(amounts.get("dog food") > 0.0);
	}

}
