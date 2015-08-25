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
package org.apache.bigtop.datagenerators.namegenerator;

import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.commons.lang3.tuple.Pair;

public class NameGenerator implements Sampler<Pair<String, String>>
{
	private final Sampler<String> firstNameSampler;
	private final Sampler<String> lastNameSampler;
	
	public NameGenerator(SeedFactory seedFactory) throws Exception
	{
		Names names = new NameReader().readData();
		
		firstNameSampler = RouletteWheelSampler.create(names.getFirstNames(), seedFactory);
		lastNameSampler = RouletteWheelSampler.create(names.getLastNames(), seedFactory);
	}
	
	public Pair<String, String> sample() throws Exception
	{
		return Pair.of(firstNameSampler.sample(), lastNameSampler.sample());
	}
}
