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
package org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class ProductCategory implements Serializable
{
	private static final long serialVersionUID = -7638076590334497836L;

	String categoryLabel;
	ImmutableSet<PetSpecies> applicableSpecies;
	ImmutableSet<String> fieldNames;
	boolean triggerTransaction;
	double dailyUsageRate;
	double amountUsedPerPetAverage;
	double amountUsedPerPetVariance;
	double triggerTransactionRate;
	double triggerPurchaseRate;
	ImmutableList<Product> products;

	public ProductCategory(String categoryLabel, Set<PetSpecies> species, Set<String> fieldNames,
			boolean triggerTransaction, double dailyUsageRate, double amountUsedPerPetAverage,
				double amountUsedPerPetVariance, double triggerTransactionRate,
				double triggerPurchaseRate, List<Product> products)
	{
		this.categoryLabel = categoryLabel;
		this.applicableSpecies = ImmutableSet.copyOf(species);
		this.fieldNames = ImmutableSet.copyOf(fieldNames);
		this.triggerTransaction = triggerTransaction;
		this.dailyUsageRate = dailyUsageRate;
		this.amountUsedPerPetAverage = amountUsedPerPetAverage;
		this.amountUsedPerPetVariance = amountUsedPerPetVariance;
		this.triggerTransactionRate = triggerTransactionRate;
		this.triggerPurchaseRate = triggerPurchaseRate;
		this.products = ImmutableList.copyOf(products);
	}

	public String getCategoryLabel()
	{
		return categoryLabel;
	}

	public ImmutableSet<PetSpecies> getApplicableSpecies()
	{
		return applicableSpecies;
	}

	public ImmutableSet<String> getFieldNames()
	{
		return fieldNames;
	}
	public Boolean getTriggerTransaction()
	{
		return triggerTransaction;
	}

	public Double getDailyUsageRate()
	{
		return dailyUsageRate;
	}

	public Double getBaseAmountUsedAverage()
	{
		return amountUsedPerPetAverage;
	}

	public Double getBaseAmountUsedVariance()
	{
		return amountUsedPerPetVariance;
	}

	public Double getTransactionTriggerRate()
	{
		return triggerTransactionRate;
	}

	public Double getPurchaseTriggerRate()
	{
		return triggerPurchaseRate;
	}

	public ImmutableList<Product> getProducts()
	{
		return products;
	}
}
