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
package org.apache.bigtop.datagenerators.bigpetstore.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.apache.bigtop.datagenerators.bigpetstore.DataLoader;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Customer;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Product;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Store;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.Transaction;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.InputData;
import org.apache.bigtop.datagenerators.bigpetstore.datamodels.inputs.ProductCategory;
import org.apache.bigtop.datagenerators.bigpetstore.generators.purchase.PurchasingModel;
import org.apache.commons.lang3.tuple.Pair;


public class Driver
{
	int nStores;
	int nCustomers;
	int nPurchasingModels;
	double simulationTime;
	long seed;
	File outputDir;

	static final int NPARAMS = 6;

	private void printUsage()
	{
		String usage = "BigPetStore Data Generator\n" +
				"\n" +
				"Usage: java -jar bps-data-generator-v0.2.java outputDir nStores nCustomers nPurchasingModels simulationLength seed\n" +
				"\n" +
				"outputDir - (string) directory to write files\n" +
				"nStores - (int) number of stores to generate\n" +
				"nCustomers - (int) number of customers to generate\n" +
				"nPurchasingModels - (int) number of purchasing models to generate\n" +
				"simulationLength - (float) number of days to simulate\n" +
				"seed - (long) seed for RNG. If not given, one is reandomly generated.\n";

		System.out.println(usage);
	}

	public void parseArgs(String[] args)
	{
		if(args.length != NPARAMS && args.length != (NPARAMS - 1))
		{
			printUsage();
			System.exit(1);
		}

		int i = -1;

		outputDir = new File(args[++i]);
		if(! outputDir.exists())
		{
			System.err.println("Given path (" + args[i] + ") does not exist.\n");
			printUsage();
			System.exit(1);
		}

		if(! outputDir.isDirectory())
		{
			System.err.println("Given path (" + args[i] + ") is not a directory.\n");
			printUsage();
			System.exit(1);
		}

		try
		{
			nStores = Integer.parseInt(args[++i]);
		}
		catch(Exception e)
		{
			System.err.println("Unable to parse '" + args[i] + "' as an integer for nStores.\n");
			printUsage();
			System.exit(1);
		}

		try
		{
			nCustomers = Integer.parseInt(args[++i]);
		}
		catch(Exception e)
		{
			System.err.println("Unable to parse '" + args[i] + "' as an integer for nCustomers.\n");
			printUsage();
			System.exit(1);
		}

		try
		{
			nPurchasingModels = Integer.parseInt(args[++i]);
		}
		catch(Exception e)
		{
			System.err.println("Unable to parse '" + args[i] + "' as an integer for nPurchasingModels.\n");
			printUsage();
			System.exit(1);
		}

		try
		{
			simulationTime = Double.parseDouble(args[++i]);
		}
		catch(Exception e)
		{
			System.err.println("Unable to parse '" + args[i] + "' as a float for simulationLength.\n");
			printUsage();
			System.exit(1);
		}

		try
		{
			seed = Long.parseLong(args[++i]);
		}
		catch(Exception e)
		{
			System.err.println("Unable to parse '" + args[i] + "' as a long for the seed.\n");
			printUsage();
			System.exit(1);
		}
	}

	private void writeTransactions(Collection<Transaction> transactions) throws Exception
	{
		File outputFile = new File(outputDir.toString() + File.separator + "transactions.txt");
		System.out.println(outputFile.toString());
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

		for(Transaction transaction : transactions)
		{
			for(Product product : transaction.getProducts())
			{
				String record = transaction.getId() + ",";
				record += transaction.getDateTime() + ",";
				record += transaction.getStore().getId() + ",";
				record += transaction.getStore().getLocation().getZipcode() + ",";
				record += transaction.getStore().getLocation().getCity() + ",";
				record += transaction.getStore().getLocation().getState() + ",";
				record += transaction.getCustomer().getId() + ",";
				Pair<String, String> name = transaction.getCustomer().getName();
				record += name.getLeft() + " " + name.getRight() + ",";
				record += transaction.getCustomer().getLocation().getZipcode() + ",";
				record += transaction.getCustomer().getLocation().getCity() + ",";
				record += transaction.getCustomer().getLocation().getState() + ",";
				record += product.toString() + "\n";

				outputStream.write(record.getBytes());
			}
		}

		outputStream.close();
	}

	private void writeCustomers(Collection<Customer> customers) throws Exception
	{
		File outputFile = new File(outputDir.toString() + File.separator + "customers.txt");
		System.out.println(outputFile.toString());
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

		for(Customer customer : customers)
		{
			String record = customer.getId() + ",";
			Pair<String, String> name = customer.getName();
			record += name.getLeft() + "," + name.getRight() + ",";
			record += customer.getLocation().getZipcode() + ",";
			record += customer.getLocation().getCity() + ",";
			record += customer.getLocation().getState() + "\n";

			outputStream.write(record.getBytes());
		}

		outputStream.close();
	}

	private void writeStores(Collection<Store> stores) throws Exception
	{
		File outputFile = new File(outputDir.toString() + File.separator + "stores.txt");
		System.out.println(outputFile.toString());
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

		for(Store store : stores)
		{
			String record = store.getId() + ",";
			record += store.getLocation().getZipcode() + ",";
			record += store.getLocation().getCity() + ",";
			record += store.getLocation().getState() + "\n";

			outputStream.write(record.getBytes());
		}

		outputStream.close();
	}

	private void writeProducts(Collection<ProductCategory> productCategories) throws Exception
	{
		File outputFile = new File(outputDir.toString() + File.separator + "products.txt");
		System.out.println(outputFile.toString());
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

		for(ProductCategory category : productCategories)
		{

			for(Product product : category.getProducts())
			{
				String record = category.getCategoryLabel() + ",";
				record += product.toString() + "\n";

				outputStream.write(record.getBytes());
			}
		}

		outputStream.close();
	}

	private void writePurchasingProfiles(List<ProductCategory> productCategories, List<PurchasingModel> profiles) throws Exception
	{
		File outputFile = new File(outputDir.toString() + File.separator + "purchasing_profiles.txt");
		System.out.println(outputFile.toString());
		OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

		for(ProductCategory category : productCategories)
		{
			int i = 0;
			for(PurchasingModel model : profiles)
			{
				Object productModel = model.getProfile(category.getCategoryLabel());
				String record = productModel.toString();

				outputStream.write(record.getBytes());

				i += 1;
			}
		}

		outputStream.close();
	}

	public Simulation buildSimulation(InputData inputData)
	{
		return new Simulation(inputData, nStores, nCustomers, nPurchasingModels, simulationTime, seed);
	}

	private void run(InputData inputData) throws Exception
	{
		Simulation simulation = buildSimulation(inputData);

		simulation.simulate();

		writeStores(simulation.getStores());
		writeCustomers(simulation.getCustomers());
		writeProducts(simulation.getProductCategories());
		writePurchasingProfiles(simulation.getProductCategories(), simulation.getPurchasingProfiles());
		writeTransactions(simulation.getTransactions());
	}
	public void run(String[] args) throws Exception
	{
		parseArgs(args);

		InputData inputData = (new DataLoader()).loadData();

		run(inputData);
	}

	public static void main(String[] args) throws Exception
	{
		Driver driver = new Driver();
		driver.run(args);
	}

	public Double getSimulationLength()
	{
		return simulationTime;
	}

	public int getNCustomers()
	{
		return nCustomers;
	}

	public long getSeed()
	{
		return seed;
	}

	public int getNStores()
	{
		return nStores;
	}

	public File getOutputDir()
	{
		return outputDir;
	}
}
