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
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.DiscretePDF;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class StaticPurchasingModelSampler implements Sampler<StaticPurchasingModel>
{
	final ImmutableMap<ProductCategory, Sampler<DiscretePDF<Product>>> categorySamplers;
	
	public StaticPurchasingModelSampler(Map<ProductCategory, Sampler<DiscretePDF<Product>>> categorySamplers)
	{
		this.categorySamplers = ImmutableMap.copyOf(categorySamplers);
	}
	
	public StaticPurchasingModel sample() throws Exception
	{
		Map<String, DiscretePDF<Product>> pdfs = Maps.newHashMap();
		for(ProductCategory productCategory : categorySamplers.keySet())
		{
			Sampler<DiscretePDF<Product>> sampler = categorySamplers.get(productCategory);
			pdfs.put(productCategory.getCategoryLabel(), sampler.sample());
		}
		
		return new StaticPurchasingModel(pdfs);
	}
}

