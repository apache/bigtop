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

import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModel;
import org.apache.bigtop.datagenerators.samplers.markovmodels.MarkovModelBuilder;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

import com.google.common.collect.Maps;

public class MarkovModelProductCategorySampler implements Sampler<MarkovModel<Product>>
{
	final ProductCategory productCategory;
	final Sampler<Double> fieldSimilarityWeightSampler;
	final Sampler<Double> loopbackWeightSampler;

	final Map<String, Double> fieldWeights;
	Map<String, Double> fieldSimilarityWeights;
	double loopbackWeight;

	public MarkovModelProductCategorySampler(ProductCategory productCategory,
			Map<String, Double> fieldWeights, Sampler<Double> fieldSimilarityWeightSampler,
			Sampler<Double> loopbackWeightSampler)
	{
		this.productCategory = productCategory;

		this.fieldSimilarityWeightSampler = fieldSimilarityWeightSampler;
		this.fieldWeights = fieldWeights;
		this.loopbackWeightSampler = loopbackWeightSampler;
	}

	protected void generateWeights() throws Exception
	{
		fieldSimilarityWeights = Maps.newHashMap();

		for(String fieldName : productCategory.getFieldNames())
		{
			fieldSimilarityWeights.put(fieldName,fieldSimilarityWeightSampler.sample());
		}

		loopbackWeight = loopbackWeightSampler.sample();
	}

	protected double productPairWeight(Product product1, Product product2)
	{
		double weightSum = 0.0;
		for(String fieldName : productCategory.getFieldNames())
		{
			double fieldWeight = this.fieldWeights.get(fieldName);

			if(product1.getFieldValue(fieldName).equals(product2.getFieldValue(fieldName)))
			{
				fieldWeight *= this.fieldSimilarityWeights.get(fieldName);
			}
			else
			{
				fieldWeight *= (1.0 - this.fieldSimilarityWeights.get(fieldName));
			}

			weightSum += fieldWeight;
		}
		return weightSum;
	}

	public MarkovModel<Product> sample() throws Exception
	{
		generateWeights();

		MarkovModelBuilder<Product> builder = new MarkovModelBuilder<Product>();

		for(Product product1 : productCategory.getProducts())
		{
			builder.addStartState(product1, 1.0);

			double weightSum = 0.0;
			for(Product product2 : productCategory.getProducts())
			{
				if(!product1.equals(product2))
				{
					weightSum += productPairWeight(product1, product2);
				}
			}

			for(Product product2 : productCategory.getProducts())
			{
				double weight = 0.0;
				if(!product1.equals(product2))
				{
					weight = (1.0 - loopbackWeight) * productPairWeight(product1, product2) / weightSum;
				}
				else
				{	weight = loopbackWeight;

				}

				builder.addTransition(product1, product2, weight);
			}
		}

		return builder.build();
	}
}
