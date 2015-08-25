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

import java.util.Collection;
import java.util.Set;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class CustomerTransactionParameters
{
	final ImmutableMultiset<PetSpecies> petCounts;
	final double averageTransactionTriggerTime;
	final double averagePurchaseTriggerTime;

	public CustomerTransactionParameters(Multiset<PetSpecies> petCounts,
			double averageTransactionTriggerTime, double averagePurchaseTriggerTime)
	{
		this.petCounts = ImmutableMultiset.copyOf(petCounts);
		this.averageTransactionTriggerTime = averageTransactionTriggerTime;
		this.averagePurchaseTriggerTime = averagePurchaseTriggerTime;
	}

	public double getAverageTransactionTriggerTime()
	{
		return averageTransactionTriggerTime;
	}

	public double getAveragePurchaseTriggerTime()
	{
		return averagePurchaseTriggerTime;
	}

	public int countPetsBySpecies(PetSpecies species)
	{
		return petCounts.count(species);
	}

	public int countPetsBySpecies(Collection<PetSpecies> allSpecies)
	{
		int count = 0;
		Set<PetSpecies> speciesSet = Sets.newHashSet(allSpecies);
		for(PetSpecies species : speciesSet)
		{
			count += countPetsBySpecies(species);
		}

		return count;
	}

	public int countPets()
	{
		return petCounts.size();
	}

}
