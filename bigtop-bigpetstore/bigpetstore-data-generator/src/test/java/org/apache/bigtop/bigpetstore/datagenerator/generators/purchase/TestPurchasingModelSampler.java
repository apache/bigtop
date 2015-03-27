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
package org.apache.bigtop.bigpetstore.datagenerator.generators.purchase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.bigtop.bigpetstore.datagenerator.Constants;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.PetSpecies;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Product;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategory;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategoryBuilder;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;
import org.apache.bigtop.bigpetstore.datagenerator.generators.purchase.MarkovPurchasingModel;
import org.apache.bigtop.bigpetstore.datagenerator.generators.purchase.PurchasingModelSamplerBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class TestPurchasingModelSampler
{
	
	private List<ProductCategory> createProducts()
	{
		List<ProductCategory> productCategories = Lists.newArrayList();
		
		ProductCategoryBuilder foodBuilder = new ProductCategoryBuilder();
		foodBuilder.addApplicableSpecies(PetSpecies.DOG);
		foodBuilder.setAmountUsedPetPetAverage(1.0);
		foodBuilder.setAmountUsedPetPetVariance(1.0);
		foodBuilder.setDailyUsageRate(2.0);
		foodBuilder.addFieldName(Constants.PRODUCT_CATEGORY);
		foodBuilder.addFieldName(Constants.PRODUCT_QUANTITY);
		foodBuilder.addFieldName("Flavor");
		foodBuilder.setCategory("dogFood");
		foodBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "dogFood",
				Constants.PRODUCT_QUANTITY, (Object) 60.0, "Flavor", "Fish & Potato")));
		foodBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "dogFood",
				Constants.PRODUCT_QUANTITY, (Object) 30.0, "Flavor", "Chicken & Rice")));
		foodBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "dogFood",
				Constants.PRODUCT_QUANTITY, (Object) 15.0, "Flavor", "Lamb & Barley")));
		productCategories.add(foodBuilder.build());
		
		ProductCategoryBuilder bagBuilder = new ProductCategoryBuilder();
		bagBuilder.addApplicableSpecies(PetSpecies.DOG);
		bagBuilder.setAmountUsedPetPetAverage(1.0);
		bagBuilder.setAmountUsedPetPetVariance(1.0);
		bagBuilder.setDailyUsageRate(2.0);
		bagBuilder.addFieldName(Constants.PRODUCT_CATEGORY);
		bagBuilder.addFieldName(Constants.PRODUCT_QUANTITY);
		bagBuilder.addFieldName("Color");
		bagBuilder.setCategory("Poop Bags");
		bagBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "Poop Bags",
				Constants.PRODUCT_QUANTITY, (Object) 60.0, "Color", "Blue")));
		bagBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "Poop Bags",
				Constants.PRODUCT_QUANTITY, (Object) 30.0, "Color", "Red")));
		bagBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "Poop Bags",
				Constants.PRODUCT_QUANTITY, (Object) 120.0, "Color", "Multicolor")));
		productCategories.add(bagBuilder.build());
		
		return productCategories;
	}

	@Test
	public void testSample() throws Exception
	{
		SeedFactory seedFactory = new SeedFactory(1245);
		
		List<ProductCategory> productCategories = createProducts();
		
		PurchasingModelSamplerBuilder builder = new PurchasingModelSamplerBuilder(productCategories, seedFactory);
		Sampler<MarkovPurchasingModel> sampler = builder.buildMarkovPurchasingModel();
		MarkovPurchasingModel profile = sampler.sample();
		
		assertNotNull(profile);
		assertNotNull(profile.getProductCategories());
		assertTrue(profile.getProductCategories().size() > 0);
		
		for(String label : profile.getProductCategories())
		{
			assertNotNull(profile.getProfile(label));
			assertNotNull(profile.getProfile(label).getStartWeights());
			assertTrue(profile.getProfile(label).getStartWeights().size() > 0);
			assertNotNull(profile.getProfile(label).getTransitionWeights());
			assertTrue(profile.getProfile(label).getTransitionWeights().size() > 0);
		}
	}

}
