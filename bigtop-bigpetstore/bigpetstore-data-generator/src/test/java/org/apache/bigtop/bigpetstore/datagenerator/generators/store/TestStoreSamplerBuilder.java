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
package org.apache.bigtop.bigpetstore.datagenerator.generators.store;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Pair;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Store;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ZipcodeRecord;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;
import org.apache.bigtop.bigpetstore.datagenerator.generators.store.StoreSamplerBuilder;
import org.junit.Test;

public class TestStoreSamplerBuilder
{

	@Test
	public void testBuild() throws Exception
	{
		List<ZipcodeRecord> zipcodes = Arrays.asList(new ZipcodeRecord[] {				
				new ZipcodeRecord("11111", Pair.create(1.0, 1.0), "AZ", "Tempte", 30000.0, 100),
				new ZipcodeRecord("22222", Pair.create(2.0, 2.0), "AZ", "Phoenix", 45000.0, 200),
				new ZipcodeRecord("33333", Pair.create(3.0, 3.0), "AZ", "Flagstaff", 60000.0, 300)
				});
		
		assertTrue(zipcodes.size() > 0);
		
		SeedFactory factory = new SeedFactory(1234);
		
		StoreSamplerBuilder builder = new StoreSamplerBuilder(zipcodes, factory);
		Sampler<Store> sampler = builder.build();
		
		Store store = sampler.sample();
		assertNotNull(store);
		assertTrue(store.getId() >= 0);
		assertNotNull(store.getName());
		assertNotNull(store.getLocation());
		
	}

}
