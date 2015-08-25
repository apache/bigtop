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

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class CustomerTransactionParametersBuilder
{
	private Multiset<PetSpecies> petCounts;
	private double averageTransactionTriggerTime;
	private double averagePurchaseTriggerTime;

	public CustomerTransactionParametersBuilder()
	{
		this.petCounts = HashMultiset.create();
		this.averagePurchaseTriggerTime = 0.0;
		this.averageTransactionTriggerTime = 0.0;
	}

	public void addPet(PetSpecies species)
	{
		this.petCounts.add(species);
	}

	public void setAverageTransactionTriggerTime(
			double averageTransactionTriggerTime)
	{
		this.averageTransactionTriggerTime = averageTransactionTriggerTime;
	}

	public void setAveragePurchaseTriggerTime(double averagePurchaseTriggerTime)
	{
		this.averagePurchaseTriggerTime = averagePurchaseTriggerTime;
	}

	public CustomerTransactionParameters build()
	{
		return new CustomerTransactionParameters(this.petCounts,
				this.averageTransactionTriggerTime,
				this.averagePurchaseTriggerTime);
	}
}
