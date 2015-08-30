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
package org.apache.bigtop.datagenerators.bigpetstore.generators.customer;

import java.util.List;
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Store;
import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.bigtop.datagenerators.samplers.pdfs.ProbabilityDensityFunction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class CustomerLocationPDF implements ProbabilityDensityFunction<Location>
{
	private final Map<Location, Double> pdf;

	public CustomerLocationPDF(List<Location> zipcodes, Store store, double averageDistance)
	{
		this.pdf = build(zipcodes, store, averageDistance);
	}

	protected ImmutableMap<Location, Double> build(List<Location> zipcodeTable,
			Store store, double averageDistance)
	{
		double lambda = 1.0 / averageDistance;

		Map<Location, Double> zipcodeWeights = Maps.newHashMap();
		double totalWeight = 0.0;
		for(Location record : zipcodeTable)
		{
			double dist = record.distance(store.getLocation());

			double weight = lambda * Math.exp(-1.0 * lambda * dist);
			totalWeight += weight;
			zipcodeWeights.put(record, weight);
		}

		Map<Location, Double> pdf = Maps.newHashMap();
		for(Location record : zipcodeTable)
		{
			pdf.put(record, zipcodeWeights.get(record) / totalWeight);
		}

		return ImmutableMap.copyOf(pdf);
	}

	public double probability(Location record)
	{
		if(!this.pdf.containsKey(record))
			return 0.0;

		return this.pdf.get(record);
	}
}
