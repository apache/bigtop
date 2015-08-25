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

import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.rules.Rule;

public class ProductFilterIterator implements Iterator<Product>
{
	Rule rule;
	Iterator<Product> products;
	Product buffer;

	public ProductFilterIterator(Rule rule, Iterator<Product> products)
	{
		this.rule = rule;
		this.products = products;
		this.buffer = getNext();
	}

	private Product getNext()
	{
		while(products.hasNext())
		{
			Product product = products.next();

			if(!rule.ruleMatches(product))
			{
				return product;
			}
		}

		return null;
	}

	@Override
	public boolean hasNext()
	{
		return buffer != null;
	}

	@Override
	public Product next()
	{
		Product previous = buffer;
		buffer = getNext();

		return previous;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("ProductFilter does not support remove()");
	}

}
