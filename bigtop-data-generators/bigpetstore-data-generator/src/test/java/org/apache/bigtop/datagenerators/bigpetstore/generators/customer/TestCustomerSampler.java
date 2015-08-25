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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Customer;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Store;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ZipcodeRecord;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.pdfs.ProbabilityDensityFunction;
import org.apache.bigtop.datagenerators.samplers.samplers.ConditionalSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.SequenceSampler;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TestCustomerSampler
{
	protected ConditionalSampler<ZipcodeRecord, Store> buildLocationSampler(List<Store> stores, List<ZipcodeRecord> records,
			SeedFactory factory)
	{
		final Map<Store, Sampler<ZipcodeRecord>> locationSamplers = Maps.newHashMap();
		for(Store store : stores)
		{
			ProbabilityDensityFunction<ZipcodeRecord> locationPDF = new CustomerLocationPDF(records,
					store, Constants.AVERAGE_CUSTOMER_STORE_DISTANCE);
			Sampler<ZipcodeRecord> locationSampler = RouletteWheelSampler.create(records, locationPDF, factory);
			locationSamplers.put(store, locationSampler);
		}
			
		return new ConditionalSampler<ZipcodeRecord, Store>()
				{
					public ZipcodeRecord sample(Store store) throws Exception
					{
						return locationSamplers.get(store).sample();
					}
				};
	}

	@Test
	public void testBuild() throws Exception
	{	
		SeedFactory factory = new SeedFactory(1234);
		
		List<Pair<String, String>> nameList = Lists.newArrayList();
		nameList.add(Pair.of("Fred", "Fred"));
		nameList.add(Pair.of("Gary", "Gary"));
		nameList.add(Pair.of("George", "George"));
		nameList.add(Pair.of("Fiona", "Fiona"));
		
		List<ZipcodeRecord> zipcodes = Arrays.asList(new ZipcodeRecord[] {				
				new ZipcodeRecord("11111", Pair.of(1.0, 1.0), "AZ", "Tempte", 30000.0, 100),
				new ZipcodeRecord("22222", Pair.of(2.0, 2.0), "AZ", "Phoenix", 45000.0, 200),
				new ZipcodeRecord("33333", Pair.of(3.0, 3.0), "AZ", "Flagstaff", 60000.0, 300)
				});
		
		List<Store> stores = new ArrayList<Store>();
		for(int i = 0; i < zipcodes.size(); i++)
		{
			Store store = new Store(i, "Store_" + i, zipcodes.get(i));
			stores.add(store);
		}
		
		
		Sampler<Integer> idSampler = new SequenceSampler();
		Sampler<Pair<String, String>> nameSampler = RouletteWheelSampler.createUniform(nameList, factory);
		Sampler<Store> storeSampler = RouletteWheelSampler.createUniform(stores, factory);
		ConditionalSampler<ZipcodeRecord, Store> zipcodeSampler = buildLocationSampler(stores, zipcodes, factory);
		
		Sampler<Customer> sampler = new CustomerSampler(idSampler, nameSampler, storeSampler, zipcodeSampler);
		
		Customer customer = sampler.sample();
		
		assertNotNull(customer);
		assertTrue(customer.getId() >= 0);
		assertNotNull(customer.getName());
		assertTrue(nameList.contains(customer.getName()));
		assertNotNull(customer.getLocation());
		assertTrue(zipcodes.contains(customer.getLocation()));
		
	}

}
