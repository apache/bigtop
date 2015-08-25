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

import java.util.Arrays;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.PetSpecies;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.BoundedMultiModalGaussianSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.UniformIntSampler;

public class CustomerTransactionParametersSamplerBuilder
{
	final private SeedFactory seedFactory;

	public CustomerTransactionParametersSamplerBuilder(SeedFactory seedFactory)
	{
		this.seedFactory = seedFactory;
	}

	public Sampler<CustomerTransactionParameters> build()
	{
		Sampler<Integer> nPetsSampler = new UniformIntSampler(Constants.MIN_PETS, Constants.MAX_PETS, seedFactory);

		Sampler<PetSpecies> petSpeciesSampler = RouletteWheelSampler.createUniform(Arrays.asList(PetSpecies.values()), seedFactory);

		Sampler<Double> transactionTriggerTimeSampler = new BoundedMultiModalGaussianSampler(Constants.TRANSACTION_TRIGGER_TIME_GAUSSIANS,
					Constants.TRANSACTION_TRIGGER_TIME_MIN, Constants.TRANSACTION_TRIGGER_TIME_MAX,
					seedFactory);

		Sampler<Double> purchaseTriggerTimeSampler = new BoundedMultiModalGaussianSampler(Constants.PURCHASE_TRIGGER_TIME_GAUSSIANS,
				Constants.PURCHASE_TRIGGER_TIME_MIN, Constants.PURCHASE_TRIGGER_TIME_MAX,
				seedFactory);

		return new CustomerTransactionParametersSampler(nPetsSampler, petSpeciesSampler,
				transactionTriggerTimeSampler, purchaseTriggerTimeSampler);
	}

}
