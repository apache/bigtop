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

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.BoundedMultiModalGaussianSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestBoundedMultiModalGaussianSampler
{

	@Test
	public void testSample() throws Exception
	{
		double upperbound = 10.0;
		double lowerbound = 1.0;

		List<Pair<Double, Double>> distributions = Lists.newArrayList(Pair.of(2.0, 2.0), Pair.of(7.5, 2.0));

		SeedFactory seedFactory = new SeedFactory(1234);

		Sampler<Double> sampler = new BoundedMultiModalGaussianSampler(distributions, lowerbound, upperbound, seedFactory);

		Double result = sampler.sample();

		assertTrue(result >= lowerbound);
		assertTrue(result <= upperbound);
	}
}
