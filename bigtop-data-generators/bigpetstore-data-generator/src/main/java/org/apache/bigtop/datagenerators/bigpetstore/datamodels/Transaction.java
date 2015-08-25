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
package org.apache.bigtop.datagenerators.bigpetstore.datamodels;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class Transaction implements Serializable
{
	private static final long serialVersionUID = 103133601154354349L;

	final int id;
	final Customer customer;
	final Store store;
	final Double dateTime;
	final ImmutableList<Product> products;

	public Transaction(int id, Customer customer, Store store, Double dateTime, List<Product> products)
	{
		this.id = id;
		this.customer = customer;
		this.store = store;
		this.dateTime = dateTime;
		this.products = ImmutableList.copyOf(products);
	}

	public int getId()
	{
		return id;
	}

	public Customer getCustomer()
	{
		return customer;
	}

	public Store getStore()
	{
		return store;
	}

	public Double getDateTime()
	{
		return dateTime;
	}

	public ImmutableList<Product> getProducts()
	{
		return products;
	}


}
