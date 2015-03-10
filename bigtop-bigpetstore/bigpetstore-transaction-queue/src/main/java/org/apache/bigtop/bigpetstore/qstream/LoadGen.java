/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.bigtop.bigpetstore.qstream;

import com.github.rnowling.bps.datagenerator.datamodels.inputs.InputData;
import com.github.rnowling.bps.datagenerator.datamodels.inputs.ProductCategory;
import com.github.rnowling.bps.datagenerator.datamodels.*;
//import com.github.rnowling.bps.datagenerator.*{DataLoader,StoreGenerator,CustomerGenerator => CustGen, PurchasingProfileGenerator,TransactionGenerator}
import com.github.rnowling.bps.datagenerator.*;
import com.github.rnowling.bps.datagenerator.framework.SeedFactory;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This driver uses the data generator API to generate
 * an arbitrarily large data set of petstore transactions.
 * <p/>
 * Each "transaction" consists of many "products", each of which
 * is stringified into what is often called a "line item".
 * <p/>
 * Then, spark writes those line items out as a distributed hadoop file glob.
 */
public abstract class LoadGen {

    /**
     * Overridable for now.
     * @return
     */
    public int getQueueSize(){
        return 100*1000;
    }

    public abstract LinkedBlockingQueue<Transaction> startWriteQueue(int qSize);

    public static boolean TESTING=false;

    int nStores = 1000;
    int nCustomers = 1000;
    double simulationLength = -1;
    long seed = System.currentTimeMillis();
    String outputDir = null;

    protected LoadGen(int nStores, int nCustomers, double simulationLength, long seed) throws Throwable {
        this.nStores = nStores;
        this.nCustomers = nCustomers;
        this.simulationLength = simulationLength;
        this.seed = seed;
    }


    /**
     * Helper function.  Makes sure we sleep for a while
     * when queue is empty and also when we startup.
     */
    public void waitFor(long milliseconds, LinkedBlockingQueue<Transaction> q){
        try{
            Thread.sleep(milliseconds);
        }
        catch(Throwable t){
        }
        /**
         * Sleep for 2 seconds at a time until queue is full.
         */
        while(q.size()<100) {
            try{
                Thread.sleep(2000L);
            }
            catch(Throwable t){

            }
        }
    }

    static final long startTime = System.currentTimeMillis();
    static double total = 0;



    public static void main(String[] args){
        try {
            LoadGen lg = LoadGenFactory.parseArgs(args);
            long start=System.currentTimeMillis();
            int runs = 0;
            //write everything to /tmp, every 20 seconds.
            LinkedBlockingQueue<Transaction> q = lg.startWriteQueue(10000);
            while(true){
                lg.iterateData(q, System.currentTimeMillis());
                runs++;
                /**
                 * if testing , dont run forever.  TODO, make runtime configurable.
                 */
                if(TESTING && runs == 2){
                    System.out.println("DONE...");
                    return;
                }
            }
        }
        catch(Throwable t){
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Thread-friendly data iterator, writes to a blocking queue.
     */
    public void iterateData(LinkedBlockingQueue<Transaction> queue,long rseed) throws Throwable {
        long start = System.currentTimeMillis();
        final InputData inputData = new DataLoader().loadData();
        final SeedFactory seedFactory = new SeedFactory(rseed);

        System.out.println("Generating stores...");
        final ArrayList<Store> stores = new ArrayList<Store>();
        final StoreGenerator storeGenerator = new StoreGenerator(inputData, seedFactory);
        for (int i = 0; i < nStores; i++) {
            Store store = storeGenerator.generate();
            stores.add(store);
        }

        System.out.println("Generating customers...");

        final List<Customer> customers = Lists.newArrayList();
        final CustomerGenerator custGen = new CustomerGenerator(inputData, stores, seedFactory);
        for (int i = 0; i < nCustomers; i++) {
            Customer customer = custGen.generate();
            customers.add(customer);
        }

        System.out.println("...Generated " + customers.size());

        Long nextSeed = seedFactory.getNextSeed();

        Collection<ProductCategory> products = inputData.getProductCategories();
        Iterator<Customer> custIter = customers.iterator();

        if(! custIter.hasNext())
            throw new RuntimeException("No customer data ");
        //Create a new purchasing profile.
        PurchasingProfileGenerator profileGen = new PurchasingProfileGenerator(products, seedFactory);
        PurchasingProfile profile = profileGen.generate();

        /** Stop either if
        * 1) the queue is full
        * 2) run out of customers).
        */
        while(queue.remainingCapacity()>0 && custIter.hasNext()){
            Customer cust = custIter.next();
            int transactionsForThisCustomer = 0;
            TransactionGenerator transGen = new TransactionGenerator(cust, profile, stores, products, seedFactory);
            Transaction trC = transGen.generate();
            while(trC.getDateTime()<simulationLength) {
                queue.put(trC);
                trC=transGen.generate();
            }
        }

    }
}