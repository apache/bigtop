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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModel;
import org.apache.bigtop.datagenerators.samplers.samplers.BoundedMultiModalGaussianSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class PurchasingModelSamplerBuilder
{
	final ImmutableList<ProductCategory> productCategories;
	final SeedFactory seedFactory;

	public PurchasingModelSamplerBuilder(Collection<ProductCategory> productCategories, SeedFactory seedFactory)
	{
		this.productCategories = ImmutableList.copyOf(productCategories);
		this.seedFactory = seedFactory;
	}

	protected Map<String, Double> generateFieldWeights(Sampler<Double> fieldWeightSampler) throws Exception
	{
		Set<String> fieldNames = new HashSet<String>();
		for(ProductCategory pc : productCategories)
		{
			for(String fieldName : pc.getFieldNames())
			{
				fieldNames.add(fieldName);
			}
		}

		Map<String, Double> fieldWeights = Maps.newHashMap();
		for(String fieldName : fieldNames)
		{
			double weight = fieldWeightSampler.sample();
			fieldWeights.put(fieldName, weight);
		}

		return fieldWeights;
	}

	public Sampler<MarkovPurchasingModel> buildMarkovPurchasingModel() throws Exception
	{

		Sampler<Double> fieldWeightSampler = new BoundedMultiModalGaussianSampler(Constants.PRODUCT_MSM_FIELD_WEIGHT_GAUSSIANS,
				Constants.PRODUCT_MSM_FIELD_WEIGHT_LOWERBOUND,
				Constants.PRODUCT_MSM_FIELD_WEIGHT_UPPERBOUND,
				seedFactory);

		Sampler<Double> fieldSimilarityWeightSampler = new BoundedMultiModalGaussianSampler(Constants.PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_GAUSSIANS,
				Constants.PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_LOWERBOUND,
				Constants.PRODUCT_MSM_FIELD_SIMILARITY_WEIGHT_UPPERBOUND,
				seedFactory);

		Sampler<Double> loopbackWeightSampler = new BoundedMultiModalGaussianSampler(Constants.PRODUCT_MSM_LOOPBACK_WEIGHT_GAUSSIANS,
				Constants.PRODUCT_MSM_LOOPBACK_WEIGHT_LOWERBOUND,
				Constants.PRODUCT_MSM_LOOPBACK_WEIGHT_UPPERBOUND,
				seedFactory);

		Map<String, Double> fieldWeights = generateFieldWeights(fieldWeightSampler);

		Map<ProductCategory, Sampler<MarkovModel<Product>>> categorySamplers = Maps.newHashMap();
		for(ProductCategory productCategory : productCategories)
		{
			MarkovModelProductCategorySampler sampler = new MarkovModelProductCategorySampler(productCategory,
					fieldWeights, fieldSimilarityWeightSampler, loopbackWeightSampler);
			categorySamplers.put(productCategory, sampler);
		}

		return new MarkovPurchasingModelSampler(categorySamplers);
	}

	public Sampler<? extends PurchasingModel> build() throws Exception
	{
		if(Constants.PURCHASING_MODEL_TYPE.equals(Constants.PurchasingModelType.MARKOV))
		{
			return buildMarkovPurchasingModel();
		}
		else
		{
			return new MultinomialPurchasingModelSampler(productCategories, seedFactory);
		}
	}
}
