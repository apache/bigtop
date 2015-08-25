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

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.UniformIntSampler;
import org.junit.Test;

public class TestUniformIntSampler
{

	@Test
	public void testSample() throws Exception
	{
		int upperbound = 10;
		int lowerbound = 1;

		SeedFactory seedFactory = new SeedFactory(1234);

		Sampler<Integer> sampler = new UniformIntSampler(lowerbound, upperbound, seedFactory);

		Integer result = sampler.sample();

		assertTrue(result >= lowerbound);
		assertTrue(result <= upperbound);
	}

	@Test
	public void testSampleInclusive() throws Exception
	{
		int upperbound = 2;
		int lowerbound = 1;

		SeedFactory seedFactory = new SeedFactory(1234);

		Sampler<Integer> sampler = new UniformIntSampler(lowerbound, upperbound, seedFactory);

		Integer result = sampler.sample();

		assertTrue(result >= lowerbound);
		assertTrue(result <= upperbound);
	}

}
