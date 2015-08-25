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

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerTransactionParameters;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerTransactionParametersBuilder;
import org.junit.Test;

public class TestCustomerTransactionParametersBuilder
{

	@Test
	public void testBuild() throws Exception
	{
		CustomerTransactionParametersBuilder builder = new CustomerTransactionParametersBuilder();

		builder.addPet(PetSpecies.DOG);
		builder.addPet(PetSpecies.DOG);
		builder.addPet(PetSpecies.DOG);

		builder.setAveragePurchaseTriggerTime(1.0);
		builder.setAverageTransactionTriggerTime(2.0);

		CustomerTransactionParameters transParams = builder.build();

		assertTrue(transParams.countPetsBySpecies(PetSpecies.DOG) == 3);
		assertTrue(transParams.countPetsBySpecies(PetSpecies.CAT) == 0);
		assertTrue(transParams.getAveragePurchaseTriggerTime() == 1.0);
		assertTrue(transParams.getAverageTransactionTriggerTime() == 2.0);
	}

}
