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

package org.apache.bigtop.bigpetstore.spark.generator

import org.apache.bigtop.datagenerators.bigpetstore.datamodels._
import org.apache.bigtop.datagenerators.bigpetstore._
import org.apache.bigtop.datagenerators.samplers.SeedFactory

import scala.jdk.CollectionConverters._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._

import java.util.Date
import scala.util.Random

/**
 * This driver uses the data generator API to generate
 * an arbitrarily large data set of petstore transactions.
 *
 * Each "transaction" consists of many "products", each of which
 * is stringified into what is often called a "line item".
 *
 * Then, spark writes those line items out as a distributed hadoop file glob.
 */
object SparkDriver {
  private var nStores: Int = -1
  private var nCustomers: Int = -1
  private var simulationLength: Double = -1.0
  private var seed: Long = -1
  private var outputDir: String = ""
  private val NPARAMS = 5
  private val BURNIN_TIME = 7.0 // days

  private def printUsage(): Unit = {
    val usage: String =
      "BigPetStore Data Generator.\n" +
        "Usage: spark-submit ... outputDir nStores nCustomers simulationLength [seed]\n" +
        "outputDir - (string) directory to write files\n" +
        "nStores - (int) number of stores to generate\n" +
        "nCustomers - (int) number of customers to generate\n" +
        "simulationLength - (float) number of days to simulate\n" +
        "seed - (long) seed for RNG. If not given, one is randomly generated.\n"
    System.err.println(usage)
  }

  def parseArgs(args: Array[String]): Unit = {
    if (args.length != NPARAMS && args.length != (NPARAMS - 1)) {
      printUsage()
      System.exit(1)
    }
    outputDir = args(0)
    try {
      nStores = args(1).toInt
    }
    catch {
      case _: NumberFormatException =>
        System.err.println("Unable to parse '" + args(1) + "' as an integer for nStores.\n")
        printUsage()
        System.exit(1)
    }
    try {
      nCustomers = args(2).toInt
    }
    catch {
      case _: NumberFormatException =>
        System.err.println("Unable to parse '" + args(2) + "' as an integer for nCustomers.\n")
        printUsage()
        System.exit(1)
    }
    try {
      simulationLength = args(3).toDouble
    }
    catch {
      case _: NumberFormatException =>
        System.err.println("Unable to parse '" + args(3) + "' as a float for simulationLength.\n")
        printUsage()
        System.exit(1)
    }

    //If seed isn't present, then no is used seed.
    if (args.length == NPARAMS) {
      try {
        seed = args(4).toLong
      }
      catch {
        case _: NumberFormatException =>
          System.err.println("Unable to parse '" + args(4) + "' as a long for seed.\n")
          printUsage()
          System.exit(1)
      }
    }
    else {
      seed = new Random().nextLong()
    }
  }

  /**
   * Here we generate an RDD of all the petstore transactions,
   * by generating the static data first (stores, customers, ...)
   * followed by running the simulation as a distributed spark task.
   */
  def generateData(sc: SparkContext): RDD[Transaction] = {
    val inputData = new DataLoader().loadData()
    val seedFactory = new SeedFactory(seed)

    println("Generating stores...")
    val storeGenerator = new StoreGenerator(inputData, seedFactory)
    val stores = (1 to nStores).foldLeft(Nil: List[Store])((stores, _) => storeGenerator.generate() :: stores)
    println("...Done generating stores.")

    println("Generating customers...")
    val customerGenerator = new CustomerGenerator(inputData, stores.asJava, seedFactory)
    val customers = (1 to nCustomers).foldLeft(Nil: List[Customer])((customers, _) => customerGenerator.generate() :: customers)
    println("...Done generating customers.")

    println("Broadcasting products...")
    val productBC = sc.broadcast(inputData.getProductCategories)
    val customerRDD = sc.parallelize(customers)
    // This substitution into a constant value is a must, since it is referred to in an RDD transformation function.
    // See BIGTOP-2148 for details.
    val simLen = simulationLength
    val nextSeed = seedFactory.getNextSeed()
    println("...Done broadcasting products.")

    println("Defining transaction DAG...")

    /**
     * See inline comments below regarding how we
     * generate TRANSACTION objects from CUSTOMERs.
     */
    val transactionRDD = customerRDD.mapPartitionsWithIndex {
      (index, customers) => {
        // Create a new RNG
        val seedFactory = new SeedFactory(nextSeed ^ index)
        val products = productBC.value
        val profileGen = new PurchasingModelGenerator(products, seedFactory)
        val profile = profileGen.generate()
        val transactions = customers.map {
          customer => {
            // Create a new purchasing profile.
            val transGen = new TransactionGenerator(customer, profile, products, seedFactory)
            // Create a list of this customer's transactions for the time period
            Iterator.continually(transGen.generate()).takeWhile(_.getDateTime < simLen).foldLeft(Nil: List[Transaction])(
              (transactions, transaction) => if (transaction.getDateTime > BURNIN_TIME) transaction :: transactions else transactions
            ).reverse
          }
        }
        transactions
      }
    }.flatMap(s => s)

    println("...Done defining transaction DAG.")

    println("Generating transactions...")

    // forces RDD materialization.
    val nTrans = transactionRDD.count()
    println(s"... Done Generating $nTrans transactions.")

    /**
     * Return the RDD representing all the petstore transactions.
     * This RDD contains a distributed collection of instances where
     * a customer went to a pet store, and bought a variable number of items.
     * We can then serialize all the contents to disk.
     */
    transactionRDD
  }

  private def lineItem(t: Transaction, date: Date, p: Product): String = {
    t.getStore.getId.toString + "," +
      t.getStore.getLocation.getZipcode + "," +
      t.getStore.getLocation.getCity + "," +
      t.getStore.getLocation.getState + "," +
      t.getCustomer.getId + "," +
      t.getCustomer.getName.getKey + "," +
      t.getCustomer.getName.getValue + "," +
      t.getCustomer.getLocation.getZipcode + "," +
      t.getCustomer.getLocation.getCity + "," +
      t.getCustomer.getLocation.getState + "," +
      t.getId + "," +
      date + "," + p
  }

  def writeData(transactionRDD: RDD[Transaction]): Unit = {
    val initialDate: Long = new Date().getTime

    val transactionStringsRDD = transactionRDD.flatMap { transaction =>
      transaction.getProducts.asScala.map { product =>
        // days -> milliseconds = days * 24 h / day * 60 min / hr * 60 sec / min * 1000 ms / sec
        val dateMS = (transaction.getDateTime * 24.0 * 60.0 * 60.0 * 1000.0).toLong
        // Return a stringified "line item", which represents a single item bought.
        lineItem(transaction, new Date(initialDate + dateMS), product)
      }
    }
    // Distributed serialization of the records to part-r-* files...
    transactionStringsRDD.saveAsTextFile(outputDir + "/transactions")
  }

  def main(args: Array[String]): Unit = {
    parseArgs(args)
    val conf = new SparkConf().setAppName("BPS Data Generator")
    val sc = new SparkContext(conf)
    val transactionRDD = generateData(sc)
    writeData(transactionRDD)
    sc.stop()
  }
}
