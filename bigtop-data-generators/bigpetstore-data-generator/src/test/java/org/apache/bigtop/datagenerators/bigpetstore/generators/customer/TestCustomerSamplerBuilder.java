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

import java.util.Arrays;
import java.util.List;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Customer;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Store;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.InputData;
import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TestCustomerSamplerBuilder
{

	@Test
	public void testSample() throws Exception
	{
		List<Location> zipcodes = Arrays.asList(new Location[] {
				new Location("11111", Pair.of(1.0, 1.0), "AZ", "Tempte", 30000.0, 100),
				new Location("22222", Pair.of(2.0, 2.0), "AZ", "Phoenix", 45000.0, 200),
				new Location("33333", Pair.of(3.0, 3.0), "AZ", "Flagstaff", 60000.0, 300)
				});

		// don't need product categories for building customers
		InputData inputData = new InputData(zipcodes);

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
		assertNotNull(customer.getLocation());

	}

}
