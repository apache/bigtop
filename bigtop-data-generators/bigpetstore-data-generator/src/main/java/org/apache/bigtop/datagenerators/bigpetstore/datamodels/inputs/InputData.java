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
package org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.bigtop.datagenerators.bigpetstore.Constants;
import org.apache.bigtop.datagenerators.bigpetstore.ProductGenerator;
import org.apache.bigtop.datagenerators.locations.Location;

public class InputData implements Serializable
{
	private static final long serialVersionUID = 9078989799806707788L;

	List<Location> zipcodeTable;
	Collection<ProductCategory> productCategories;

	public InputData(List<Location> zipcodeTable)
	{
		this(zipcodeTable, new ProductGenerator(Constants.PRODUCTS_COLLECTION).generate());
	}

	public InputData(List<Location> zipcodeTable, Collection<ProductCategory> productCategories)
	{
		this.zipcodeTable = Collections.unmodifiableList(zipcodeTable);
		this.productCategories = Collections.unmodifiableCollection(productCategories);
	}

	public List<Location> getZipcodeTable()
	{
		return zipcodeTable;
	}

	public Collection<ProductCategory> getProductCategories()
	{
		return productCategories;
	}
}
