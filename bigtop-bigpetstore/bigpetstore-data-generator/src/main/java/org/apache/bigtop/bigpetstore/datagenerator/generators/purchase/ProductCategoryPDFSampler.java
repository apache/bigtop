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
import java.util.Set;

import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Product;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.DiscretePDF;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ProductCategoryPDFSampler implements Sampler<DiscretePDF<Product>>
{
	private final ProductCategory productCategory;
	private final Sampler<Double> fieldValueWeightSampler;
	private final Map<String, Double> fieldWeights;
	
	public ProductCategoryPDFSampler(ProductCategory productCategory,
			Map<String, Double> fieldWeights,
			Sampler<Double> fieldValueWeightSampler)
	{
		this.productCategory = productCategory;
		this.fieldWeights = fieldWeights;
		this.fieldValueWeightSampler = fieldValueWeightSampler;
	}
	
	protected <T> Map<T, Double> normalize(Map<T, Double> weights)
	{
		double weightSum = 0.0;
		for(double w : weights.values())
		{
			weightSum += w;
		}
		
		Map<T, Double> normalized = Maps.newHashMap();
		for(Map.Entry<T, Double> entry : weights.entrySet())
		{
			normalized.put(entry.getKey(), entry.getValue() / weightSum);
		}
		
		return normalized;
	}
	
	protected Map<String, Map<Object, Double>> generateFieldValueWeights() throws Exception
	{
		Map<String, Set<Object>> allFieldValues = Maps.newHashMap();
		for(String fieldName : productCategory.getFieldNames())
		{
			Set<Object> fieldValues = Sets.newHashSet();
			for(Product p : productCategory.getProducts())
			{
				Object fieldValue = p.getFieldValue(fieldName); 
				fieldValues.add(fieldValue);
			}
			allFieldValues.put(fieldName, fieldValues);
		}
		
		Map<String, Map<Object, Double>> allFieldValueWeights = Maps.newHashMap();
		for(String fieldName : productCategory.getFieldNames())
		{
			Map<Object, Double> fieldValueWeights = Maps.newHashMap();
			for(Object fieldValue : allFieldValues.get(fieldName))
			{
				double fieldValueWeight = fieldValueWeightSampler.sample();
				fieldValueWeights.put(fieldValue, fieldValueWeight);
			}

			allFieldValueWeights.put(fieldName, fieldValueWeights);
		}
		
		return allFieldValueWeights;
	}
	
	protected Map<Product, Double> generateProductWeights() throws Exception
	{
		Map<String, Map<Object, Double>> allFieldValueWeights = generateFieldValueWeights();
		
		Map<Product, Double> productWeights = Maps.newHashMap();
		for(Product p : productCategory.getProducts())
		{
			double weight = 0.0;
			for(String fieldName : productCategory.getFieldNames())
			{
				Object fieldValue = p.getFieldValue(fieldName);
				weight += fieldWeights.get(fieldName) * allFieldValueWeights.get(fieldName).get(fieldValue);
			}
			productWeights.put(p, weight);
		}
		productWeights = normalize(productWeights);
		
		return productWeights;
	}
	
	public DiscretePDF<Product> sample() throws Exception
	{
		Map<Product, Double> probs = generateProductWeights();
		return new DiscretePDF<Product>(probs);
	}
	
}
