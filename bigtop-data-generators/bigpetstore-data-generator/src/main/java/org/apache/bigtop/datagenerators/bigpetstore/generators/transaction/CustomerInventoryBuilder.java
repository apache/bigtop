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
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CustomerInventoryBuilder
{
	final private List<ProductCategory> productCategories;
	final private SeedFactory seedFactory;
	final private CustomerTransactionParameters parameters;

	public CustomerInventoryBuilder(CustomerTransactionParameters parameters,
			SeedFactory seedFactory)
	{
		productCategories = Lists.newArrayList();
		this.seedFactory = seedFactory;
		this.parameters = parameters;
	}

	public void addProductCategory(ProductCategory productCategory)
	{
		this.productCategories.add(productCategory);
	}

	public void addAllProductCategories(Collection<ProductCategory> productCategories)
	{
		this.productCategories.addAll(productCategories);
	}

	public CustomerInventory build()
	{
		Map<String, ProductCategoryInventory> inventories = Maps.newHashMap();
		for(ProductCategory productCategory : productCategories)
		{
			if(parameters.countPetsBySpecies(productCategory.getApplicableSpecies()) > 0)
			{
				ProductCategoryInventory inventory = new ProductCategoryInventory(productCategory,
					parameters, seedFactory);
				inventories.put(productCategory.getCategoryLabel(), inventory);
			}
		}

		return new CustomerInventory(inventories);
	}
}
