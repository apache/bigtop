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
package org.apache.bigtop.datagenerators.bigpetstore.generators.purchase;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.ProductCategoryBuilder;
import org.apache.bigtop.datagenerators.bigpetstore.generators.purchase.MarkovModelProductCategorySampler;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModel;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.UniformSampler;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TestProductCategoryMarkovModelSampler
{

	private List<ProductCategory> createProducts()
	{
		List<ProductCategory> productCategories = Lists.newArrayList();

		ProductCategoryBuilder foodBuilder = new ProductCategoryBuilder();
		foodBuilder.addApplicableSpecies(PetSpecies.DOG);
		foodBuilder.setAmountUsedPetPetAverage(1.0);
		foodBuilder.setAmountUsedPetPetVariance(1.0);
		foodBuilder.setDailyUsageRate(2.0);
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
		bagBuilder.setCategory("Poop Bags");
		bagBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "Poop Bags",
				Constants.PRODUCT_QUANTITY, (Object) 60.0, "Color", "Blue")));
		bagBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "Poop Bags",
				Constants.PRODUCT_QUANTITY, (Object) 30.0, "Color", "Red")));
		bagBuilder.addProduct(new Product(ImmutableMap.of(Constants.PRODUCT_CATEGORY, (Object) "Poop Bags",
				Constants.PRODUCT_QUANTITY, (Object) 120.0, "Flavor", "Multicolor")));
		productCategories.add(bagBuilder.build());

		return productCategories;
	}

	@Test
	public void testSample() throws Exception
	{
		SeedFactory seedFactory = new SeedFactory(1245);

		List<ProductCategory> productCategories = createProducts();

		ProductCategory productCategory = productCategories.get(0);

		Sampler<Double> fieldWeightSampler = new UniformSampler(seedFactory);

		Map<String, Double> fieldWeights = Maps.newHashMap();
		for(String fieldName : productCategory.getFieldNames())
		{
			fieldWeights.put(fieldName, fieldWeightSampler.sample());
		}

		MarkovModelProductCategorySampler generator = new MarkovModelProductCategorySampler(productCategory,
				fieldWeights, new UniformSampler(seedFactory), new UniformSampler(seedFactory)
				);

		MarkovModel<Product> model = generator.sample();

		assertNotNull(model);
		assertNotNull(model.getStartWeights());
		assertNotNull(model.getTransitionWeights());
		assertTrue(model.getStartWeights().size() > 0);
		assertTrue(model.getTransitionWeights().size() > 0);
	}

}
