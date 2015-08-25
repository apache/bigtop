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
package org.apache.bigtop.datagenerators.samplers.markovmodels;

import java.util.Map;

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

import com.google.common.collect.ImmutableMap;

public class MarkovProcess<T> implements Sampler<T>
{
	final ImmutableMap<T, Sampler<T>> transitionSamplers;
	final Sampler<T> startStateSampler;

	T currentState;


	public MarkovProcess(MarkovModel<T> model, SeedFactory factory)
	{
		Map<T, Map<T, Double>> transitionTable = model.getTransitionWeights();

		startStateSampler = RouletteWheelSampler.create(model.getStartWeights(), factory);

		ImmutableMap.Builder<T, Sampler<T>> builder = ImmutableMap.builder();
		for(Map.Entry<T, Map<T, Double>> entry : transitionTable.entrySet())
		{
			builder.put(entry.getKey(), RouletteWheelSampler.create(entry.getValue(), factory));
		}


		this.transitionSamplers = builder.build();

		currentState = null;
	}

	public static <T> MarkovProcess<T> create(MarkovModel<T> model, SeedFactory factory)
	{
		return new MarkovProcess<T>(model, factory);
	}

	public T sample() throws Exception
	{
		if(currentState == null)
		{
			currentState = startStateSampler.sample();
			return currentState;
		}

		currentState = transitionSamplers.get(currentState).sample();
		return currentState;
	}
}
