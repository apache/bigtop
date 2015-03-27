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
package org.apache.bigtop.bigpetstore.datagenerator.cli;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.bigtop.bigpetstore.datagenerator.CustomerGenerator;
import org.apache.bigtop.bigpetstore.datagenerator.PurchasingModelGenerator;
import org.apache.bigtop.bigpetstore.datagenerator.StoreGenerator;
import org.apache.bigtop.bigpetstore.datagenerator.TransactionGenerator;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Customer;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Store;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.Transaction;
import org.apache.bigtop.bigpetstore.datagenerator.datamodels.inputs.InputData;
import org.apache.bigtop.bigpetstore.datagenerator.framework.SeedFactory;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.RouletteWheelSampler;
import org.apache.bigtop.bigpetstore.datagenerator.framework.samplers.Sampler;
import org.apache.bigtop.bigpetstore.datagenerator.generators.purchase.PurchasingModel;

import com.google.common.collect.Lists;

public class Simulation
{
	InputData inputData;
	SeedFactory seedFactory;
	int nStores;
	int nCustomers;
	int nPurchasingModels;
	double simulationTime;
	
	List<Store> stores;
	List<Customer> customers;
	Sampler<PurchasingModel> purchasingModelSampler;
	List<Transaction> transactions;
	
	public Simulation(InputData inputData, int nStores, int nCustomers, int nPurchasingModels, double simulationTime, long seed)
	{
		this.inputData = inputData;
		this.nStores = nStores;
		this.nCustomers = nCustomers;
		this.nPurchasingModels = nPurchasingModels;
		this.simulationTime = simulationTime;
		seedFactory = new SeedFactory(seed);
	}
	
	public void generateStores() throws Exception
	{
		System.out.println("Generating stores");
		StoreGenerator storeGenerator = new StoreGenerator(inputData, seedFactory);
		
		stores = new Vector<Store>();
		for(int i = 0; i < nStores; i++)
		{
			Store store = storeGenerator.generate();
			stores.add(store);
		}
		
		stores = Collections.unmodifiableList(stores);
		
		System.out.println("Generated " + stores.size() + " stores");
	}
	
	public void generateCustomers() throws Exception
	{
		System.out.println("Generating customers");
		CustomerGenerator generator = new CustomerGenerator(inputData, stores, seedFactory);
		
		customers = new Vector<Customer>();
		for(int i = 0; i < nCustomers; i++)
		{
			Customer customer = generator.generate();
			customers.add(customer);
		}
		
		customers = Collections.unmodifiableList(customers);
		
		System.out.println("Generated " + customers.size() + " customers");
	}
	
	public void generatePurchasingProfiles() throws Exception
	{
		System.out.println("Generating purchasing profiles");
		PurchasingModelGenerator generator = new PurchasingModelGenerator(inputData.getProductCategories(), seedFactory);
		
		Collection<PurchasingModel> purchasingProfiles = new Vector<PurchasingModel>();
		for(int i = 0; i < nPurchasingModels; i++)
		{
			PurchasingModel profile = generator.generate();
			purchasingProfiles.add(profile);
		}
		
		System.out.println("Generated " + purchasingProfiles.size() + " purchasing profiles");
		
		purchasingModelSampler = RouletteWheelSampler.createUniform(purchasingProfiles, seedFactory);
	}
	
	public void generateTransactions() throws Exception
	{
		System.out.println("Generating transactions");
		transactions = Lists.newArrayList();
		
		for(int i = 0; i < nCustomers; i++)
		{
			Customer customer = customers.get(i);
			PurchasingModel profile = purchasingModelSampler.sample();
			
			TransactionGenerator generator = new TransactionGenerator(customer,
					profile, inputData.getProductCategories(), seedFactory);
			
			while(true)
			{
				Transaction transaction = generator.generate();
				
				if(transaction.getDateTime() > simulationTime)
					break;
				transactions.add(transaction);
			}
		}
		
		System.out.println("Generated " + transactions.size() + " transactions");
	}
	
	public void simulate() throws Exception
	{
		generateStores();
		generateCustomers();
		generatePurchasingProfiles();
		generateTransactions();
	}

	public List<Store> getStores()
	{
		return stores;
	}

	public List<Customer> getCustomers()
	{
		return customers;
	}

	public List<Transaction> getTransactions()
	{
		return transactions;
	}
	
	public InputData getInputData()
	{
		return inputData;
	}
}
