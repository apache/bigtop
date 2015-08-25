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

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.util.Map;

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TestRouletteWheelSampler
{

	@Test
	public void testSample() throws Exception
	{
		Map<String, Double> dataPoints = ImmutableMap.of(
				"a", 0.25,
				"b", 0.25,
				"c", 0.25,
				"d", 0.25
				);

		SeedFactory seedFactory = new SeedFactory(1234);

		Sampler<String> sampler = new RouletteWheelSampler<String>(dataPoints, seedFactory);

		String result = sampler.sample();

		assertThat(dataPoints.keySet(), hasItem(result));
	}

	@Test
	public void testSampleUnnormalized() throws Exception
	{
		Map<String, Double> dataPoints = ImmutableMap.of(
				"a", 1.0,
				"b", 1.0,
				"c", 1.0,
				"d", 1.0
				);

		SeedFactory seedFactory = new SeedFactory(1234);

		Sampler<String> sampler = new RouletteWheelSampler<String>(dataPoints, seedFactory);

		String result = sampler.sample();

		assertThat(dataPoints.keySet(), hasItem(result));
	}

}
