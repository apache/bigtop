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
package org.apache.bigtop.datagenerators.bigpetstore;

import java.util.List;

import org.apache.bigtop.datagenerators.bigpetstore.Constants.ProductsCollectionSize;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.collections.MediumProductCollection;
import org.apache.bigtop.datagenerators.bigpetstore.generators.products.collections.SmallProductCollection;

public class ProductGenerator
{
	ProductsCollectionSize collection;

	public ProductGenerator(ProductsCollectionSize collection)
	{
		this.collection = collection;
	}

	public List<ProductCategory> generate()
	{
		List<ProductCategory> categories;

		if(collection.equals(ProductsCollectionSize.SMALL))
		{
			SmallProductCollection collection = new SmallProductCollection();
			categories = collection.generateProductCategory();
		} else
		{
			MediumProductCollection collection = new MediumProductCollection();
			categories = collection.generateProductCategory();
		}

		return categories;
	}
}
