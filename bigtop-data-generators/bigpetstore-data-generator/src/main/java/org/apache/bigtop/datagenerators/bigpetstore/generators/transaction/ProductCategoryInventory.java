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

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;

public class ProductCategoryInventory
{
	private ProductCategoryUsageTrajectory trajectory;
	private ProductCategoryUsageSimulator simulator;

	public ProductCategoryInventory(ProductCategory productCategory, CustomerTransactionParameters parameters,
			SeedFactory seedFactory)
	{

		double amountUsedAverage = productCategory.getBaseAmountUsedAverage() * parameters.countPetsBySpecies(productCategory.getApplicableSpecies());
		double amountUsedVariance = productCategory.getBaseAmountUsedVariance() * parameters.countPetsBySpecies(productCategory.getApplicableSpecies());

		trajectory = new ProductCategoryUsageTrajectory(0.0, 0.0);
		simulator = new ProductCategoryUsageSimulator(productCategory.getDailyUsageRate(),
				amountUsedAverage, amountUsedVariance, seedFactory);
	}

	public void simulatePurchase(double time, Product product) throws Exception
	{
		double amountPurchased = product.getFieldValueAsDouble(Constants.PRODUCT_QUANTITY);

		double amountRemainingBeforePurchase = trajectory.amountAtTime(time);

		trajectory = simulator.simulate(time, amountRemainingBeforePurchase + amountPurchased);
	}

	public double findExhaustionTime()
	{
		return trajectory.getLastTime();
	}

	public double findRemainingAmount(double time)
	{
		return trajectory.amountAtTime(time);
	}
}
