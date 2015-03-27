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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.bigtop.bigpetstore.datagenerator.Constants;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Pair;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Store;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.ZipcodeRecord;
import org.apache.bigtop.bigpetstore.datagenerator.generators.customer.CustomerLocationPDF;
import org.junit.Test;

public class TestCustomerLocationPDF
{

	@Test
	public void testProbability() throws Exception
	{
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
		
		CustomerLocationPDF customerLocationPDF = new CustomerLocationPDF(zipcodes, stores.get(0), 
					Constants.AVERAGE_CUSTOMER_STORE_DISTANCE);
		
		double prob = customerLocationPDF.probability(zipcodes.get(0));
		
		assertTrue(prob > 0.0);
	}

}
