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

import org.apache.bigtop.datagenerators.locations.Location;
import org.apache.bigtop.datagenerators.samplers.pdfs.ProbabilityDensityFunction;

public class StoreLocationIncomePDF implements ProbabilityDensityFunction<Location>
{
	double incomeNormalizationFactor;
	double minIncome;
	double k;

	public StoreLocationIncomePDF(List<Location> zipcodeTable, double incomeScalingFactor)
	{

		double maxIncome = 0.0;
		minIncome = Double.MAX_VALUE;

		for(Location record : zipcodeTable)
		{
			maxIncome = Math.max(maxIncome, record.getMedianHouseholdIncome());
			minIncome = Math.min(minIncome, record.getMedianHouseholdIncome());
		}

		k = Math.log(incomeScalingFactor) / (maxIncome - minIncome);

		incomeNormalizationFactor = 0.0d;
		for(Location record : zipcodeTable)
		{
			double weight = incomeWeight(record);
			incomeNormalizationFactor += weight;
		}
	}

	private double incomeWeight(Location record)
	{
		return Math.exp(k * (record.getMedianHouseholdIncome() - minIncome));
	}


	@Override
	public double probability(Location datum)
	{
		double weight = incomeWeight(datum);

		return weight / this.incomeNormalizationFactor;
	}

}
