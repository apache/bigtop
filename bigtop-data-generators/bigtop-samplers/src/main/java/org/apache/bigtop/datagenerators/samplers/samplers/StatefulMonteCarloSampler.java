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

import java.util.Random;

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.pdfs.ConditionalProbabilityDensityFunction;


public class StatefulMonteCarloSampler<T> implements Sampler<T>
{
	private final Sampler<T> stateSampler;
	private final Random rng;
	private final ConditionalProbabilityDensityFunction<T, T> acceptancePDF;
	private T currentState;

	public StatefulMonteCarloSampler(Sampler<T> stateGenerator,
			ConditionalProbabilityDensityFunction<T, T> acceptancePDF,
			T initialState,
			SeedFactory seedFactory)
	{
		this.acceptancePDF = acceptancePDF;
		this.stateSampler = stateGenerator;

		rng = new Random(seedFactory.getNextSeed());

		this.currentState = initialState;
	}

	public T sample() throws Exception
	{
		while(true)
		{
			T proposedState = this.stateSampler.sample();
			double probability = acceptancePDF.probability(proposedState, currentState);
			double r = rng.nextDouble();

			if(r < probability)
			{
				this.currentState = proposedState;
				return proposedState;
			}
		}
	}

}
