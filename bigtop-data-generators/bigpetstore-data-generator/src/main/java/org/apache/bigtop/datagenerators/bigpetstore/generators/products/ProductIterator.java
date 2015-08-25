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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.cartesian.CartesianProduct;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.cartesian.CartesianProductBase;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.cartesian.CartesianProductField;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.Rule;

public class ProductIterator implements Iterator<Product>
{

	Iterator<Product> productIterator;

	public ProductIterator(Map<String, Collection<ProductFieldValue>> fieldValues, Rule matcher,
			double basePrice, String productCategory)
	{
		productIterator = new ProductFilterIterator(matcher,
				new ProductBuilderIterator(basePrice, productCategory,
						buildCartesianProductIterator(fieldValues)));
	}

	public Iterator<Map<String, ProductFieldValue>>
		buildCartesianProductIterator(Map<String, Collection<ProductFieldValue>> fieldValues)
	{
		CartesianProduct<ProductFieldValue> product = null;
		for(Map.Entry<String, Collection<ProductFieldValue>> pair : fieldValues.entrySet())
		{
			if(product == null)
			{
				product = new CartesianProductBase<ProductFieldValue>(pair.getKey(), pair.getValue());
			} else
			{
				product = new CartesianProductField<ProductFieldValue>(pair.getKey(), pair.getValue(), product);
			}

		}

		return product;
	}

	@Override
	public boolean hasNext()
	{
		return productIterator.hasNext();
	}

	@Override
	public Product next()
	{
		return productIterator.next();
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("ProductIterator does not support remove()");
	}

}
