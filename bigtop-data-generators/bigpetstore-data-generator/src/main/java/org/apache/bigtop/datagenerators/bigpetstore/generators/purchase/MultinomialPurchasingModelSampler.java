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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.pdfs.MultinomialPDF;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.UniformIntSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.UniformSampler;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class MultinomialPurchasingModelSampler implements Sampler<MultinomialPurchasingModel>
{
	private final SeedFactory seedFactory;
	private final Collection<ProductCategory> productCategories;

	public MultinomialPurchasingModelSampler(Collection<ProductCategory> productCategories, SeedFactory seedFactory)
	{
		this.seedFactory = seedFactory;
		this.productCategories = productCategories;
	}

	protected <T> List<T> shuffle(Collection<T> input) throws Exception
	{
		Vector<T> shuffled = new Vector<>(input);
		for(int i = 0; i < input.size() - 1; i++)
		{
			int swapIdx = new UniformIntSampler(i, input.size() - 1, seedFactory).sample();
			T tmp = shuffled.get(i);
			shuffled.set(i, shuffled.get(swapIdx));
			shuffled.set(swapIdx, tmp);
		}

		return shuffled;
	}

	protected Map<Pair<String, Object>, Double> generateFieldValueWeights(ProductCategory productCategory) throws Exception
	{
		// Get all values for each field by iterating over all products
		Multimap<String, Object> allFieldValues = HashMultimap.create();
		for(String fieldName : productCategory.getFieldNames())
		{
			if(!Constants.PRODUCT_MODEL_EXCLUDED_FIELDS.contains(fieldName))
			{
				for(Product p : productCategory.getProducts())
				{
					Object fieldValue = p.getFieldValue(fieldName);
					allFieldValues.put(fieldName, fieldValue);
				}
			}
		}

		Sampler<Double> sampler = new UniformSampler(seedFactory);

		// shuffle field values
		Map<Pair<String, Object>, Double> fieldValueWeights = Maps.newHashMap();
		for(Map.Entry<String, Collection<Object>> entry : allFieldValues.asMap().entrySet())
		{
			String fieldName = entry.getKey();
			List<Object> shuffled = shuffle(entry.getValue());

			for(int i = 0; i < shuffled.size(); i++)
			{
				double weight = Constants.PRODUCT_MULTINOMIAL_POSITIVE_WEIGHT;
				if ((i + 1) > Constants.PRODUCT_MULTINOMIAL_POSITIVE_COUNT_MIN)
				{
					double r = sampler.sample();
					if (r >= Constants.PRODUCT_MULTINOMIAL_POSITIVE_FREQUENCY)
					{
						weight = Constants.PRODUCT_MULTINOMIAL_NEGATIVE_WEIGHT;
					}
				}

				Object fieldValue = shuffled.get(i);
				fieldValueWeights.put(Pair.of(fieldName, fieldValue), weight);
			}
		}


		return ImmutableMap.copyOf(fieldValueWeights);
	}

	protected Map<Product, Double> generateProductWeights(Map<Pair<String, Object>, Double> fieldValueWeights,
			ProductCategory productCategory) throws Exception
	{
		Map<Product, Double> productWeights = Maps.newHashMap();
		for(Product p : productCategory.getProducts())
		{
			double weight = 1.0;
			for(String fieldName : productCategory.getFieldNames())
			{
				if(!Constants.PRODUCT_MODEL_EXCLUDED_FIELDS.contains(fieldName))
				{
					Object fieldValue = p.getFieldValue(fieldName);
					Pair<String, Object> key = Pair.of(fieldName, fieldValue);
					weight *= fieldValueWeights.get(key);
				}
			}
			productWeights.put(p, weight);
		}

		return productWeights;
	}

	public MultinomialPurchasingModel sample() throws Exception
	{
		Map<String, MultinomialPDF<Product>> pdfs = Maps.newHashMap();
		for(ProductCategory productCategory : productCategories)
		{
			Map<Pair<String, Object>, Double> fieldWeights = this.generateFieldValueWeights(productCategory);
			Map<Product, Double> productWeights = this.generateProductWeights(fieldWeights, productCategory);
			pdfs.put(productCategory.getCategoryLabel(), new MultinomialPDF<Product>(productWeights));
		}

		return new MultinomialPurchasingModel(pdfs);
	}
}
