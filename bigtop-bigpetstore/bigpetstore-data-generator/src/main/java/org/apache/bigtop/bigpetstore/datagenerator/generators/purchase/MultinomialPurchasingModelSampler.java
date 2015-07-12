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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.bigtop.bigpetstore.datagenerator.Constants;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Pair;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Product;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ProductCategory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.DiscretePDF;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.UniformIntSampler;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class MultinomialPurchasingModelSampler implements Sampler<MultinomialPurchasingModel>
{
	private final SeedFactory seedFactory;
	private final Collection<ProductCategory> productCategories;

	public MultinomialPurchasingModelSampler(Collection<ProductCategory> productCategories, SeedFactory seedFactory)
	{
		this.seedFactory = seedFactory;
		this.productCategories = productCategories;
	}

	protected void shuffle(Vector<Pair<String, Object>> input) throws Exception
	{
		for(int i = 0; i < input.size() - 1; i++)
		{
			int swapIdx = new UniformIntSampler(i, input.size() - 1, seedFactory).sample();
			Pair<String, Object> tmp = input.get(i);
			input.set(i, input.get(swapIdx));
			input.set(swapIdx, tmp);
		}
	}

	protected Map<Pair<String, Object>, Double> generateFieldValueWeights(ProductCategory productCategory) throws Exception
	{
		Set<Pair<String, Object>> fieldValuesSet = Sets.newHashSet();
		for(String fieldName : productCategory.getFieldNames())
		{
			for(Product p : productCategory.getProducts())
			{
				Object fieldValue = p.getFieldValue(fieldName);
				fieldValuesSet.add(Pair.create(fieldName, fieldValue));
			}
		}

		Vector<Pair<String, Object>> fieldValues = new Vector<Pair<String, Object>>(fieldValuesSet);
		shuffle(fieldValues);

		int lowerbound = Math.max(Constants.PRODUCT_MULTINOMIAL_MIN_COUNT,
				(int) Math.ceil(fieldValues.size() * Constants.PRODUCT_MULTINOMIAL_MIN_PERCENT));
		int upperbound = Math.max(Constants.PRODUCT_MULTINOMIAL_MIN_COUNT + 1,
				(int) Math.floor(fieldValues.size() * Constants.PRODUCT_MULTINOMIAL_MAX_PERCENT));

		Sampler<Integer> countSampler = new UniformIntSampler(lowerbound, upperbound, seedFactory);
		int highWeightCount = countSampler.sample();
		int lowWeightCount = countSampler.sample();

		Map<Pair<String, Object>, Double> fieldValueWeights = Maps.newHashMap();
		for(int i = 0; i < fieldValues.size(); i++)
		{
			if(i < highWeightCount)
			{
				fieldValueWeights.put(fieldValues.get(i), Constants.PRODUCT_MULTINOMIAL_HIGH_WEIGHT);
			}
			else if(i >= highWeightCount && i < (lowWeightCount + highWeightCount))
			{
				fieldValueWeights.put(fieldValues.get(i), Constants.PRODUCT_MULTINOMIAL_LOW_WEIGHT);
			}
			else
			{
				fieldValueWeights.put(fieldValues.get(i), Constants.PRODUCT_MULTINOMIAL_NEUTRAL_WEIGHT);
			}
		}

		return fieldValueWeights;
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
				Object fieldValue = p.getFieldValue(fieldName);
				Pair<String, Object> key = Pair.create(fieldName, fieldValue);
				weight *= fieldValueWeights.get(key);
			}
			productWeights.put(p, weight);
		}

		return productWeights;
	}

	public MultinomialPurchasingModel sample() throws Exception
	{
		Map<String, DiscretePDF<Product>> pdfs = Maps.newHashMap();
		for(ProductCategory productCategory : productCategories)
		{
			Map<Pair<String, Object>, Double> fieldWeights = this.generateFieldValueWeights(productCategory);
			Map<Product, Double> productWeights = this.generateProductWeights(fieldWeights, productCategory);
			pdfs.put(productCategory.getCategoryLabel(), new DiscretePDF<Product>(productWeights));
		}

		return new MultinomialPurchasingModel(pdfs);
	}
}
