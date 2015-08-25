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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.bigpetstore.generators.transaction.CustomerTransactionParameters;
import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class TestCustomerTransactionParameters
{

	@Test
	public void testCountPetsBySpecies() throws Exception
	{
		Multiset<PetSpecies> petCounts = HashMultiset.create();

		petCounts.add(PetSpecies.CAT);
		petCounts.add(PetSpecies.CAT);
		petCounts.add(PetSpecies.CAT);


		CustomerTransactionParameters transParams = new CustomerTransactionParameters(
				petCounts, 0.0, 0.0);


		assertEquals(transParams.countPetsBySpecies(PetSpecies.CAT), 3);
		assertEquals(transParams.countPetsBySpecies(PetSpecies.DOG), 0);
		assertEquals(transParams.countPets(), 3);
	}

	@Test
	public void testCountPetsByMultipleSpecies() throws Exception
	{
		Multiset<PetSpecies> petCounts = HashMultiset.create();

		petCounts.add(PetSpecies.CAT);
		petCounts.add(PetSpecies.CAT);
		petCounts.add(PetSpecies.DOG);


		CustomerTransactionParameters transParams = new CustomerTransactionParameters(
				petCounts, 0.0, 0.0);


		assertEquals(transParams.countPetsBySpecies(Arrays.asList(PetSpecies.values())), 3);
		assertEquals(transParams.countPets(), 3);
	}

}
