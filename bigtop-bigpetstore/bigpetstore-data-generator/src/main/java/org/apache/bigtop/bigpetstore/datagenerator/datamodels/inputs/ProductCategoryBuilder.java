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
package org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs;

import java.util.List;
import java.util.Set;

import org.apache.bigtop.bigpetstore.datagenerator.datamodels.PetSpecies;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Product;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ProductCategoryBuilder
{
	String categoryLabel;
	Set<PetSpecies> applicableSpecies;
	Set<String> fieldNames;
	Boolean triggerTransaction;
	Double dailyUsageRate;
	Double amountUsedPerPetAverage;
	Double amountUsedPerPetVariance;
	Double triggerTransactionRate;
	Double triggerPurchaseRate;
	List<Product> products;
	
	public ProductCategoryBuilder()
	{
		applicableSpecies = Sets.newHashSet();
		fieldNames = Sets.newHashSet();
		products = Lists.newArrayList();
		
		dailyUsageRate = 0.0;
		amountUsedPerPetAverage = 0.0;
		amountUsedPerPetVariance = 0.0;
		triggerTransactionRate = 0.0;
		triggerPurchaseRate = 0.0;
		triggerTransaction = false;
		categoryLabel = null;
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
	
	public void addFieldName(String fieldName)
	{
		this.fieldNames.add(fieldName);
	}
	
	public void addProduct(Product product)
	{
		this.products.add(product);
	}
	
	protected boolean validateProducts()
	{
		for(Product product : products)
		{
			for(String fieldName : product.getFieldNames())
			{
				if(!fieldNames.contains(fieldName))
					return false;
			}
			
			for(String fieldName : fieldNames)
			{
				if(!product.getFieldNames().contains(fieldName))
					return false;
			}
		}
		
		return true;
	}
	
	public ProductCategory build()
	{
		validateProducts();
		
		return new ProductCategory(categoryLabel, applicableSpecies, fieldNames, triggerTransaction,
				dailyUsageRate, amountUsedPerPetAverage, amountUsedPerPetVariance, triggerTransactionRate,
					triggerPurchaseRate, products);
	}
}
