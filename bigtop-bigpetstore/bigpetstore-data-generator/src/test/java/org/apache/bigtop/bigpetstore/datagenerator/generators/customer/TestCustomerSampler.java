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
package org.apache.bigtop.bigpetstore.datagenerator.generators.customer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.bigtop.bigpetstore.datagenerator.Constants;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Customer;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Pair;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Store;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ZipcodeRecord;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.ProbabilityDensityFunction;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.ConditionalSampler;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.RouletteWheelSampler;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.SequenceSampler;
import org.apache.bigtop.bigpetstore.datagenerator.generators.customer.CustomerLocationPDF;
import org.apache.bigtop.bigpetstore.datagenerator.generators.customer.CustomerSampler;
import org.junit.Test;

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
		
		Collection<String> nameList = Arrays.asList(new String[] {"Fred", "Gary", "George", "Fiona"});
		List<ZipcodeRecord> zipcodes = Arrays.asList(new ZipcodeRecord[] {				
				new ZipcodeRecord("11111", Pair.create(1.0, 1.0), "AZ", "Tempte", 30000.0, 100),
				new ZipcodeRecord("22222", Pair.create(2.0, 2.0), "AZ", "Phoenix", 45000.0, 200),
				new ZipcodeRecord("33333", Pair.create(3.0, 3.0), "AZ", "Flagstaff", 60000.0, 300)
				});
		
		List<Store> stores = new ArrayList<Store>();
		for(int i = 0; i < zipcodes.size(); i++)
		{
			Store store = new Store(i, "Store_" + i, zipcodes.get(i));
			stores.add(store);
		}
		
		
		Sampler<Integer> idSampler = new SequenceSampler();
		Sampler<String> nameSampler = RouletteWheelSampler.createUniform(nameList, factory);
		Sampler<Store> storeSampler = RouletteWheelSampler.createUniform(stores, factory);
		ConditionalSampler<ZipcodeRecord, Store> zipcodeSampler = buildLocationSampler(stores, zipcodes, factory);
		
		Sampler<Customer> sampler = new CustomerSampler(idSampler, nameSampler, nameSampler, storeSampler, zipcodeSampler);
		
		Customer customer = sampler.sample();
		
		assertNotNull(customer);
		assertTrue(customer.getId() >= 0);
		assertNotNull(customer.getName());
		assertNotNull(customer.getName().getFirst());
		assertTrue(nameList.contains(customer.getName().getFirst()));
		assertNotNull(customer.getName().getSecond());
		assertTrue(nameList.contains(customer.getName().getSecond()));
		assertNotNull(customer.getLocation());
		assertTrue(zipcodes.contains(customer.getLocation()));
		
	}

}
