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
package org.apache.bigtop.datagenerators.bigpetstore.generators.store;

import java.util.List;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Store;
import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.pdfs.JointPDF;
import org.apache.bigtop.datagenerators.samplers.pdfs.ProbabilityDensityFunction;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.apache.bigtop.datagenerators.samplers.samplers.SequenceSampler;

public class StoreSamplerBuilder
{
	private final List<Location> zipcodeTable;
	private final SeedFactory seedFactory;

	public StoreSamplerBuilder(List<Location> zipcodeTable, SeedFactory seedFactory)
	{
		this.zipcodeTable = zipcodeTable;
		this.seedFactory = seedFactory;
	}

	public Sampler<Store> build()
	{
		Sampler<Integer> idSampler = new SequenceSampler();

		ProbabilityDensityFunction<Location> locationPopulationPDF =
				new StoreLocationPopulationPDF(zipcodeTable);
		ProbabilityDensityFunction<Location> locationIncomePDF =
				new StoreLocationIncomePDF(zipcodeTable, Constants.INCOME_SCALING_FACTOR);
		ProbabilityDensityFunction<Location> locationJointPDF =
				new JointPDF<Location>(zipcodeTable, locationPopulationPDF, locationIncomePDF);

		Sampler<Location> locationSampler = RouletteWheelSampler.create(zipcodeTable, locationJointPDF, seedFactory);

		return new StoreSampler(idSampler, locationSampler);
	}

}
