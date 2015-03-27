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
package org.apache.bigtop.bigpetstore.datagenerator.generators.transaction;

import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.ExponentialPDF;
import org.apache.bigtop.bigpetstore.datagenerator.framework.pdfs.ProbabilityDensityFunction;
import org.apache.bigtop.bigpetstore.datagenerator.framework.wfs.ConditionalWeightFunction;
import org.apache.bigtop.bigpetstore.datagenerator.framework.wfs.WeightFunction;

public class CategoryWeightFunction implements ConditionalWeightFunction<Double, Double>
{
	private final ProbabilityDensityFunction<Double> pdf;
	
	public CategoryWeightFunction(double averagePurchaseTriggerTime)
	{
		double lambda = 1.0 / averagePurchaseTriggerTime;
		pdf = new ExponentialPDF(lambda);
	}
	
	@Override
	public double weight(Double exhaustionTime, Double transactionTime)
	{
		return fixConditional(transactionTime).weight(exhaustionTime);
	}
	
	@Override
	public WeightFunction<Double> fixConditional(final Double transactionTime)
	{
		return new WeightFunction<Double>()
			{
				public double weight(Double exhaustionTime)
				{
					double remainingTime = Math.max(0.0, exhaustionTime - transactionTime);
					return pdf.probability(remainingTime);
				}
			};
	}
}
