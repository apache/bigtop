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
package org.apache.bigtop.datagenerators.bigpetstore.generators.customer;

import java.util.List;
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Customer;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Store;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.InputData;
import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.bigtop.datagenerators.namegenerator.NameGenerator;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.pdfs.ProbabilityDensityFunction;
import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.SequenceSampler;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;

public class CustomerSamplerBuilder
{
	private final List<Store> stores;
	private final InputData inputData;
	private final SeedFactory seedFactory;

	public CustomerSamplerBuilder(List<Store> stores, InputData inputData, SeedFactory seedFactory)
	{
		this.stores = stores;
		this.seedFactory = seedFactory;
		this.inputData = inputData;
	}

	protected ConditionalSampler<Location, Store> buildLocationSampler()
	{
		final Map<Store, Sampler<Location>> locationSamplers = Maps.newHashMap();
		for(Store store : stores)
		{
			ProbabilityDensityFunction<Location> locationPDF = new CustomerLocationPDF(inputData.getZipcodeTable(),
					store, Constants.AVERAGE_CUSTOMER_STORE_DISTANCE);
			Sampler<Location> locationSampler = RouletteWheelSampler.create(inputData.getZipcodeTable(), locationPDF, seedFactory);
			locationSamplers.put(store, locationSampler);
		}

		return new ConditionalSampler<Location, Store>()
				{
					public Location sample(Store store) throws Exception
					{
						return locationSamplers.get(store).sample();
					}
				};
	}

	public Sampler<Customer> build() throws Exception
	{
		ProbabilityDensityFunction<Store> storePDF = new CustomerStorePDF(stores);

		Sampler<Integer> idSampler = new SequenceSampler();
		Sampler<Pair<String, String>> nameSampler = new NameGenerator(seedFactory);
		Sampler<Store> storeSampler = RouletteWheelSampler.create(stores, storePDF, seedFactory);

		return new CustomerSampler(idSampler, nameSampler, storeSampler, buildLocationSampler());
	}

}
