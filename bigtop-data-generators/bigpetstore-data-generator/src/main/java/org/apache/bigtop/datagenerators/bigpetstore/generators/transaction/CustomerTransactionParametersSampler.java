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
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;

public class CustomerTransactionParametersSampler implements Sampler<CustomerTransactionParameters>
{
	final private Sampler<Integer> nPetsSampler;
	final private Sampler<PetSpecies> petSpeciesSampler;
	final private Sampler<Double> purchaseTriggerTimeSampler;
	final private Sampler<Double> transactionTriggerTimeSampler;

	public CustomerTransactionParametersSampler(Sampler<Integer> nPetsSampler,
			Sampler<PetSpecies> petSpeciesSampler,
			Sampler<Double> purchaseTriggerTimeSampler,
			Sampler<Double> transactionTriggerTimeSampler)
	{

		this.nPetsSampler = nPetsSampler;
		this.petSpeciesSampler = petSpeciesSampler;
		this.purchaseTriggerTimeSampler = purchaseTriggerTimeSampler;
		this.transactionTriggerTimeSampler = transactionTriggerTimeSampler;
	}

	protected void generatePets(CustomerTransactionParametersBuilder builder) throws Exception
	{
		int nPets = this.nPetsSampler.sample();

		for(int i = 0; i < nPets; i++)
		{
			PetSpecies species = this.petSpeciesSampler.sample();
			builder.addPet(species);
		}
	}

	public CustomerTransactionParameters sample() throws Exception
	{
		CustomerTransactionParametersBuilder builder = new CustomerTransactionParametersBuilder();

		this.generatePets(builder);
		builder.setAveragePurchaseTriggerTime(this.purchaseTriggerTimeSampler.sample());
		builder.setAverageTransactionTriggerTime(this.transactionTriggerTimeSampler.sample());

		return builder.build();
	}
}
