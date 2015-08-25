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
package org.apache.bigtop.datagenerators.bigpetstore.generators.products;

import java.util.Iterator;
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;

import com.google.common.collect.Maps;

public class ProductBuilderIterator implements Iterator<Product>
{
	Iterator<Map<String, ProductFieldValue>> productIterator;
	double basePrice;
	String productCategory;

	public ProductBuilderIterator(double basePrice, String productCategory,
			Iterator<Map<String, ProductFieldValue>> productIterator)
	{
		this.productIterator = productIterator;
		this.basePrice = basePrice;
		this.productCategory = productCategory;
	}

	@Override
	public boolean hasNext()
	{
		return productIterator != null && productIterator.hasNext();
	}

	@Override
	public Product next()
	{
		Map<String, ProductFieldValue> productComponents = productIterator.next();

		double sum = 0.0;
		double product = 1.0;

		Map<String, Object> productFields = Maps.newHashMap();

		for(Map.Entry<String, ProductFieldValue> entry : productComponents.entrySet())
		{
			productFields.put(entry.getKey(), entry.getValue().getValue());
			sum += entry.getValue().getAdd();
			product *= entry.getValue().getMultiply();
		}

		double quantity = (Double) productFields.get(Constants.PRODUCT_QUANTITY);
		double price = product * (sum + basePrice);
		double unitPrice = price / quantity;

		productFields.put(Constants.PRODUCT_UNIT_PRICE, unitPrice);
		productFields.put(Constants.PRODUCT_PRICE, price);
		productFields.put(Constants.PRODUCT_CATEGORY, productCategory);

		return new Product(productFields);
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("ProductBuilder does not support remove()");
	}

}
