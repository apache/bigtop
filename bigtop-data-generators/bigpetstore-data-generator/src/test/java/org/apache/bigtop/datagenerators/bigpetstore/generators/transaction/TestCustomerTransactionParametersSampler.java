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
package org.apache.bigtop.datagenerators.bigpetstore.generators.transaction;

import static org.junit.Assert.assertTrue;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerTransactionParameters;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerTransactionParametersSamplerBuilder;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.junit.Test;

public class TestCustomerTransactionParametersSampler
{

	@Test
	public void testSample() throws Exception
	{
		SeedFactory seedFactory = new SeedFactory(1234);
		CustomerTransactionParametersSamplerBuilder builder = new CustomerTransactionParametersSamplerBuilder(seedFactory);
		Sampler<CustomerTransactionParameters> sampler = builder.build();

		CustomerTransactionParameters transParams = sampler.sample();

		assertTrue(transParams.countPets() >= Constants.MIN_PETS);
		assertTrue(transParams.countPets() <= Constants.MAX_PETS);
		assertTrue(transParams.getAveragePurchaseTriggerTime() >= Constants.PURCHASE_TRIGGER_TIME_MIN);
		assertTrue(transParams.getAveragePurchaseTriggerTime() <= Constants.PURCHASE_TRIGGER_TIME_MAX);
		assertTrue(transParams.getAverageTransactionTriggerTime() >= Constants.TRANSACTION_TRIGGER_TIME_MIN);
		assertTrue(transParams.getAverageTransactionTriggerTime() <= Constants.TRANSACTION_TRIGGER_TIME_MAX);
	}

}
