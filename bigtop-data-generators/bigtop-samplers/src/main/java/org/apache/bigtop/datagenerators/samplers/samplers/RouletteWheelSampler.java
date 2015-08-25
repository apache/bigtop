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
package org.apache.bigtop.datagenerators.samplers.samplers;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.pdfs.MultinomialPDF;
import org.apache.bigtop.datagenerators.samplers.pdfs.ProbabilityDensityFunction;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class RouletteWheelSampler<T> implements Sampler<T>
{
	Random rng;
	final ImmutableList<Pair<T, Double>> wheel;

	public static <T> RouletteWheelSampler<T> create(Map<T, Double> domainWeights, SeedFactory factory)
	{
		return new RouletteWheelSampler<T>(domainWeights, factory);
	}

	public static <T> RouletteWheelSampler<T> create(MultinomialPDF<T> pdf, SeedFactory factory)
	{
		return new RouletteWheelSampler<T>(pdf.getData(), pdf, factory);
	}

	public static <T> RouletteWheelSampler<T> create(Collection<T> data, ProbabilityDensityFunction<T> pdf, SeedFactory factory)
	{
		return new RouletteWheelSampler<T>(data, pdf, factory);
	}

	public static <T> RouletteWheelSampler<T> createUniform(Collection<T> data, SeedFactory factory)
	{
		Map<T, Double> pdf = Maps.newHashMap();
		for(T datum : data)
		{
			pdf.put(datum, 1.0);
		}

		return create(pdf, factory);
	}

	public RouletteWheelSampler(Map<T, Double> domainWeights, SeedFactory factory)
	{
		this.rng = new Random(factory.getNextSeed());
		this.wheel = this.normalize(domainWeights);
	}

	public RouletteWheelSampler(Collection<T> data, ProbabilityDensityFunction<T> pdf, SeedFactory factory)
	{
		this.rng = new Random(factory.getNextSeed());

		Map<T, Double> domainWeights = Maps.newHashMap();
		for(T datum : data)
		{
			double prob = pdf.probability(datum);
			domainWeights.put(datum, prob);
		}

		this.wheel = this.normalize(domainWeights);
	}

	private ImmutableList<Pair<T, Double>> normalize(Map<T, Double> domainWeights)
	{
		double weightSum = 0.0;
		for(Map.Entry<T, Double> entry : domainWeights.entrySet())
		{
			weightSum += entry.getValue();
		}

		double cumProb = 0.0;
		ImmutableList.Builder<Pair<T, Double>> builder = ImmutableList.builder();
		for(Map.Entry<T, Double> entry : domainWeights.entrySet())
		{
			double prob = entry.getValue() / weightSum;
			cumProb += prob;

			builder.add(Pair.of(entry.getKey(), cumProb));
		}

		return builder.build();
	}

	public T sample()
	{
		double r = rng.nextDouble();
		for(Pair<T, Double> cumProbPair : wheel)
			if(r < cumProbPair.getValue())
				return cumProbPair.getKey();

		throw new IllegalStateException("Invalid state -- RouletteWheelSampler should never fail to sample!");
	}
}
