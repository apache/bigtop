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
package org.apache.bigtop.datagenerators.bigpetstore.generators.products;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.AlwaysTrueRule;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.NotRule;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.OrRule;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ProductCategoryBuilder
{
	String categoryLabel;
	Set<PetSpecies> applicableSpecies;
	Boolean triggerTransaction;
	Double dailyUsageRate;
	Double amountUsedPerPetAverage;
	Double amountUsedPerPetVariance;
	Double triggerTransactionRate;
	Double triggerPurchaseRate;
	List<Rule> exclusionRules;
	Map<String, Collection<ProductFieldValue>> productFieldValues;
	Double basePrice;
	List<Product> products;

	public ProductCategoryBuilder()
	{
		applicableSpecies = Sets.newHashSet();

		dailyUsageRate = 0.0;
		amountUsedPerPetAverage = 0.0;
		amountUsedPerPetVariance = 0.0;
		triggerTransactionRate = 0.0;
		triggerPurchaseRate = 0.0;
		triggerTransaction = false;
		categoryLabel = null;
		exclusionRules = new Vector<Rule>();
		productFieldValues = Maps.newHashMap();
		basePrice = 1.0;
		products = Lists.newArrayList();
	}

	public void setCategory(String category)
	{
		this.categoryLabel = category;
	}

	public void setTriggerTransaction(Boolean triggerTransaction)
	{
		this.triggerTransaction = triggerTransaction;
	}

	public void setDailyUsageRate(Double dailyUsageRate)
	{
		this.dailyUsageRate = dailyUsageRate;
	}

	public void setAmountUsedPetPetAverage(Double baseAmountUsedAverage)
	{
		this.amountUsedPerPetAverage = baseAmountUsedAverage;
	}

	public void setAmountUsedPetPetVariance(Double baseAmountUsedVariance)
	{
		this.amountUsedPerPetVariance = baseAmountUsedVariance;
	}

	public void setTriggerTransactionRate(Double triggerTransactionRate)
	{
		this.triggerTransactionRate = triggerTransactionRate;
	}

	public void setTriggerPurchaseRate(Double triggerPurchaseRate)
	{
		this.triggerPurchaseRate = triggerPurchaseRate;
	}

	public void addApplicableSpecies(PetSpecies species)
	{
		this.applicableSpecies.add(species);
	}

	public void addProduct(Product product)
	{
		products.add(product);
	}

	public void addExclusionRule(Rule rule)
	{
		this.exclusionRules.add(rule);
	}

	public void addPropertyValues(String fieldName, ProductFieldValue ... values)
	{
		this.productFieldValues.put(fieldName,  Arrays.asList(values));
	}

	public void setBasePrice(double basePrice)
	{
		this.basePrice = basePrice;
	}


	protected List<Product> generateProducts()
	{
		Rule combinedRules = new NotRule(new AlwaysTrueRule());
		if(exclusionRules.size() == 1)
		{
			combinedRules = exclusionRules.get(0);
		}
		else if(exclusionRules.size() > 1)
		{
			 combinedRules = new OrRule(exclusionRules.toArray(new Rule[] {}));
		}

		Iterator<Product> productIterator = new ProductIterator(productFieldValues, combinedRules,
				basePrice, categoryLabel);

		while(productIterator.hasNext())
		{
			products.add(productIterator.next());
		}

		return products;
	}

	public void validateProducts()
	{
		for(Product product : products)
		{
			if(!product.getFieldNames().contains(Constants.PRODUCT_CATEGORY))
			{
				throw new IllegalStateException("Product must have field " + Constants.PRODUCT_CATEGORY);
			}

			if(!product.getFieldNames().contains(Constants.PRODUCT_QUANTITY))
			{
				throw new IllegalStateException("Product must have field " + Constants.PRODUCT_QUANTITY);
			}

			if(!product.getFieldNames().contains(Constants.PRODUCT_PRICE))
			{
				throw new IllegalStateException("Product must have field " + Constants.PRODUCT_PRICE);
			}

			if(!product.getFieldNames().contains(Constants.PRODUCT_UNIT_PRICE))
			{
				throw new IllegalStateException("Product must have field " + Constants.PRODUCT_UNIT_PRICE);
			}
		}
	}

	public ProductCategory build()
	{
		List<Product> products = generateProducts();

		Set<String> fieldNames = Sets.newHashSet();
		for(Product product : products)
		{
			fieldNames.addAll(product.getFieldNames());
		}

		return new ProductCategory(categoryLabel, applicableSpecies, fieldNames, triggerTransaction,
				dailyUsageRate, amountUsedPerPetAverage, amountUsedPerPetVariance, triggerTransactionRate,
					triggerPurchaseRate, products);
	}
}
