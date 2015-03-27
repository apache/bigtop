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
import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.DiscretePDF;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.RouletteWheelSampler;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class StaticPurchasingModel implements PurchasingModel
{

	private static final long serialVersionUID = 5863830733003282570L;
	
	private final ImmutableMap<String, DiscretePDF<Product>> productPDFs;
	
	public StaticPurchasingModel(Map<String, DiscretePDF<Product>> productPDFs)
	{
		this.productPDFs = ImmutableMap.copyOf(productPDFs);
	}

	@Override
	public ImmutableSet<String> getProductCategories()
	{
		return productPDFs.keySet();
	}

	@Override
	public PurchasingProcesses buildProcesses(SeedFactory seedFactory)
	{
		Map<String, Sampler<Product>> processes = Maps.newHashMap();
		for(String category : getProductCategories())
		{
			DiscretePDF<Product> pdf = productPDFs.get(category);
			processes.put(category, RouletteWheelSampler.create(pdf, seedFactory));
		}
		
		return new PurchasingProcesses(processes);
	}

}
