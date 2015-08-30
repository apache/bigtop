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
package org.apache.bigtop.datagenerators.samplers.wfs;

import java.util.Map;
import java.util.Set;

import org.apache.bigtop.datagenerators.samplers.pdfs.MultinomialPDF;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class MultinomialWF<T> implements DiscreteWeightFunction<T>
{
	private final ImmutableMap<T, Double> weights;

	public MultinomialWF(Map<T, Double> probabilities)
	{
		this.weights = ImmutableMap.copyOf(probabilities);
	}

	public Set<T> getData()
	{
		return weights.keySet();
	}

	public double weight(T value)
	{
		if(weights.containsKey(value))
		{
			return weights.get(value);
		}

		return 0.0;
	}
	
	public MultinomialPDF<T> normalize()
	{
		double sum = 0.0;
		for(double w : weights.values())
		{
			sum += w;
		}
		
		Map<T, Double> probabilities = Maps.newHashMap();
		for(Map.Entry<T, Double> entry : weights.entrySet())
		{
			probabilities.put(entry.getKey(), entry.getValue() / sum);
		}
		
		return new MultinomialPDF<T>(probabilities);
	}

}
