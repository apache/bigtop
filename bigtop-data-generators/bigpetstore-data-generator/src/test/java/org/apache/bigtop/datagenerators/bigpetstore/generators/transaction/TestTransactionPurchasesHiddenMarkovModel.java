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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.ProductCategoryBuilder;
import org.apache.bigtop.datagenerators.bigpetstore.generators.purchase.MarkovPurchasingModel;
import org.apache.bigtop.datagenerators.bigpetstore.generators.purchase.PurchasingProcesses;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CategoryWeightFunction;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerInventory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerInventoryBuilder;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerTransactionParameters;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerTransactionParametersSamplerBuilder;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.TransactionPurchasesHiddenMarkovModel;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModel;
import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModelBuilder;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.wfs.ConditionalWeightFunction;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TestTransactionPurchasesHiddenMarkovModel
{

	private List<Product> createProducts(String category)
	{
		List<Product> products = Lists.newArrayList();

		for(int i = 0; i < 10; i++)
		{
			Map<String, Object> fields = Maps.newHashMap();
			fields.put(Constants.PRODUCT_CATEGORY, category);
			fields.put(Constants.PRODUCT_QUANTITY, (double) (i + 1));
			Product product = new Product(fields);
			products.add(product);
		}

		return products;
	}

	private MarkovModel<Product> createMarkovModel(ProductCategory category)
	{
		MarkovModelBuilder<Product> markovBuilder = new MarkovModelBuilder<Product>();

		for(Product product1 : category.getProducts())
		{
			markovBuilder.addStartState(product1, 1.0);
			for(Product product2 : category.getProducts())
			{
				markovBuilder.addTransition(product1, product2, 1.0);
			}
		}

		return markovBuilder.build();
	}

	protected PurchasingProcesses createProcesses(ProductCategory dogFoodCategory,
			ProductCategory catFoodCategory, SeedFactory seedFactory)
	{
		MarkovModel<Product> dogFoodModel = createMarkovModel(dogFoodCategory);
		MarkovModel<Product> catFoodModel = createMarkovModel(catFoodCategory);

		Map<String, MarkovModel<Product>> models = Maps.newHashMap();
		models.put("dog food", dogFoodModel);
		models.put("cat food", catFoodModel);

		MarkovPurchasingModel profile = new MarkovPurchasingModel(models);

		return profile.buildProcesses(seedFactory);
	}

	protected ProductCategory createCategory(String category)
	{
		List<Product> products = createProducts(category);

		ProductCategoryBuilder builder = new ProductCategoryBuilder();

		if(category.equals("dog food"))
		{
			builder.addApplicableSpecies(PetSpecies.DOG);
		}
		else
		{
			builder.addApplicableSpecies(PetSpecies.CAT);
		}

		builder.setAmountUsedPetPetAverage(1.0);
		builder.setAmountUsedPetPetVariance(1.0);
		builder.setDailyUsageRate(2.0);
		builder.setCategory(category);
		builder.setTriggerPurchaseRate(1.0 / 10.0);
		builder.setTriggerPurchaseRate(1.0 / 10.0);

		for(Product product : products)
		{
			builder.addProduct(product);
		}

		return builder.build();
	}

	protected TransactionPurchasesHiddenMarkovModel createHMM() throws Exception
	{
		SeedFactory seedFactory = new SeedFactory(1234);

		ProductCategory dogFoodCategory = createCategory("dog food");
		ProductCategory catFoodCategory = createCategory("cat food");

		PurchasingProcesses processes = createProcesses(dogFoodCategory, catFoodCategory, seedFactory);

		CustomerTransactionParametersSamplerBuilder transParamsBuilder = new CustomerTransactionParametersSamplerBuilder(seedFactory);
		Sampler<CustomerTransactionParameters> sampler = transParamsBuilder.build();

		CustomerTransactionParameters parameters = sampler.sample();

		CustomerInventoryBuilder inventoryBuilder = new CustomerInventoryBuilder(parameters, seedFactory);
		inventoryBuilder.addProductCategory(dogFoodCategory);
		inventoryBuilder.addProductCategory(catFoodCategory);
		CustomerInventory inventory = inventoryBuilder.build();

		ConditionalWeightFunction<Double, Double> categoryWF =
				new CategoryWeightFunction(parameters.getAveragePurchaseTriggerTime());

		TransactionPurchasesHiddenMarkovModel hmm = new TransactionPurchasesHiddenMarkovModel(processes,
				categoryWF, inventory, seedFactory);

		return hmm;
	}

	@Test
	public void testChooseCategory() throws Exception
	{
		TransactionPurchasesHiddenMarkovModel hmm = createHMM();

		String category = hmm.chooseCategory(1.0, 0);

		assertNotNull(category);
		assertTrue(category.equals(TransactionPurchasesHiddenMarkovModel.STOP_STATE) ||
				category.equals("dog food") ||
				category.equals("cat food"));
	}

	@Test
	public void testChooseProduct() throws Exception
	{
		TransactionPurchasesHiddenMarkovModel hmm = createHMM();

		Product product = hmm.chooseProduct("dog food");

		assertNotNull(product);
		assertTrue(product.getFieldValue(Constants.PRODUCT_CATEGORY).equals("dog food"));

		product = hmm.chooseProduct("cat food");

		assertNotNull(product);
		assertTrue(product.getFieldValue(Constants.PRODUCT_CATEGORY).equals("cat food"));
	}

	@Test
	public void testSample() throws Exception
	{
		TransactionPurchasesHiddenMarkovModel hmm = createHMM();

		List<Product> purchase = hmm.sample(1.0);

		assertTrue(purchase.size() > 0);

		for(int i = 0; i < purchase.size(); i++)
		{
			Product product = purchase.get(i);

			// first product should never be null
			assertNotNull(product);
			assertTrue(product.getFieldValue(Constants.PRODUCT_CATEGORY).equals("dog food") ||
					product.getFieldValue(Constants.PRODUCT_CATEGORY).equals("cat food"));
		}

	}

}
