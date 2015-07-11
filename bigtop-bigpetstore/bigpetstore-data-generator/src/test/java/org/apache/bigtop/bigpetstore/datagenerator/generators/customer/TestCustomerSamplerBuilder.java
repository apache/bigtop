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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Customer;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Pair;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Store;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.InputData;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.Names;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ZipcodeRecord;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TestCustomerSamplerBuilder
{

	@Test
	public void testSample() throws Exception
	{	
		Map<String, Double> nameList = ImmutableMap.of("Fred", 1.0, "George", 1.0, "Gary", 1.0, "Fiona", 1.0);
		List<ZipcodeRecord> zipcodes = Arrays.asList(new ZipcodeRecord[] {				
				new ZipcodeRecord("11111", Pair.create(1.0, 1.0), "AZ", "Tempte", 30000.0, 100),
				new ZipcodeRecord("22222", Pair.create(2.0, 2.0), "AZ", "Phoenix", 45000.0, 200),
				new ZipcodeRecord("33333", Pair.create(3.0, 3.0), "AZ", "Flagstaff", 60000.0, 300)
				});
		
		Names names = new Names(nameList, nameList);
		
		// don't need product categories for building customers
		InputData inputData = new InputData(zipcodes, names);
		
		List<Store> stores = Arrays.asList(new Store(0, "Store_0", zipcodes.get(0)),
				new Store(1, "Store_1", zipcodes.get(1)),
				new Store(2, "Store_2", zipcodes.get(2))
				);
		
		SeedFactory factory = new SeedFactory(1234);
		
		CustomerSamplerBuilder builder = new CustomerSamplerBuilder(stores, inputData, factory);
		Sampler<Customer> sampler = builder.build();
		
		Customer customer = sampler.sample();
		
		assertNotNull(customer);
		assertTrue(customer.getId() >= 0);
		assertNotNull(customer.getName());
		assertNotNull(customer.getName().getFirst());
		assertNotNull(customer.getName().getSecond());
		assertNotNull(customer.getLocation());
		
	}

}
