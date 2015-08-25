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
package org.apache.bigtop.datagenerators.bigpetstore.generators.purchase;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.generators.purchase.PurchasingProcesses;
import org.apache.bigtop.datagenerators.samplers.SeedFactory;
import org.apache.bigtop.datagenerators.samplers.samplers.RouletteWheelSampler;
import org.apache.bigtop.datagenerators.samplers.samplers.Sampler;
import org.junit.Test;

import com.google.common.collect.Maps;

public class TestPurchasingProcesses
{

	@Test
	public void testSimulatePurchase() throws Exception
	{
		Map<Product, Double> productPDF = Maps.newHashMap();

		for(int i = 0; i < 10; i++)
		{
			Map<String, Object> fields = Maps.newHashMap();
			fields.put(Constants.PRODUCT_CATEGORY, "dog food");
			fields.put(Constants.PRODUCT_QUANTITY, (double) (i + 1));
			Product product = new Product(fields);
			productPDF.put(product, 0.1);
		}

		SeedFactory seedFactory = new SeedFactory(1234);
		Sampler<Product> sampler = RouletteWheelSampler.create(productPDF, seedFactory);


		Map<String, Sampler<Product>> processesMap = Maps.newHashMap();
		processesMap.put("dog food", sampler);
		PurchasingProcesses processes = new PurchasingProcesses(processesMap);

		Product product = processes.sample("dog food");

		assertNotNull(product);
		assertNotNull(product.getFieldValue(Constants.PRODUCT_CATEGORY));
		assertNotNull(product.getFieldValue(Constants.PRODUCT_QUANTITY));

		product = processes.sample("dog food");

		assertNotNull(product);
		assertNotNull(product.getFieldValue(Constants.PRODUCT_CATEGORY));
		assertNotNull(product.getFieldValue(Constants.PRODUCT_QUANTITY));
	}

}
