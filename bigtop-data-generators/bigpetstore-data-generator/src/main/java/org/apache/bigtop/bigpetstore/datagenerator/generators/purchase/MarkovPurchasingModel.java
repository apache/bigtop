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

import java.util.Map;

import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Product;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.markovmodels.MarkovModel;
import org.apache.bigtop.bigpetstore.datagenerator.framework.markovmodels.MarkovProcess;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class MarkovPurchasingModel implements PurchasingModel<MarkovModel<Product>>
{

	private static final long serialVersionUID = 3098355461347511619L;
	ImmutableMap<String, MarkovModel<Product>> productCategoryProfiles;
	
	public MarkovPurchasingModel(Map<String, MarkovModel<Product>> productCategoryProfiles)
	{
		this.productCategoryProfiles = ImmutableMap.copyOf(productCategoryProfiles);
	}
	
	@Override
	public ImmutableSet<String> getProductCategories()
	{
		return productCategoryProfiles.keySet();
	}

	@Override
	public MarkovModel<Product> getProfile(String productCategory)
	{
		return productCategoryProfiles.get(productCategory);
	}

	@Override
	public PurchasingProcesses buildProcesses(SeedFactory seedFactory)
	{
		Map<String, Sampler<Product>> processes = Maps.newHashMap();
		for(String category : getProductCategories())
		{
			MarkovModel<Product> model = getProfile(category);
			processes.put(category, new MarkovProcess<Product>(model, seedFactory));
		}
		
		return new PurchasingProcesses(processes);
	}
}
