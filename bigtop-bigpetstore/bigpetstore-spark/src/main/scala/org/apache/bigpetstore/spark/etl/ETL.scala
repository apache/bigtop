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

package org.apache.bigtop.bigpetstore.spark.etl

import org.apache.bigtop.bigpetstore.spark.datamodel._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util._

case class TransactionProduct(customerId: Long, transactionId: Long,
  storeId: Long, dateTime: Calendar, product: String)

case class ETLParameters(inputDir: String, outputDir: String)

object SparkETL {

  private val NPARAMS = 2

  private def printUsage() {
    val usage: String = "BigPetStore Spark ETL\n" +
      "\n" +
      "Usage: spark-submit ... inputDir outputDir\n" +
      "\n" +
      "inputDir - (string) directory of raw transaction records from data generator\n" +
      "outputDir - (string) directory to write normalized records\n"

    println(usage)
  }

  def parseArgs(args: Array[String]): ETLParameters = {
    if(args.length != NPARAMS) {
      printUsage()
      System.exit(1)
    }

    ETLParameters(args(0), args(1))
  }

  def readRawData(sc: SparkContext, inputDir: String): RDD[String] = {
    val rawRecords = sc.textFile(inputDir + "/transactions")
      .flatMap(_.split("\n"))

    rawRecords
  }

  def parseRawData(rawRecords: RDD[String]):
      RDD[(Store, Location, Customer, Location, TransactionProduct)] = {
    val splitRecords = rawRecords.map { r =>
      val cols = r.split(",")

      val storeId = cols(0).toInt
      val storeZipcode = cols(1)
      val storeCity = cols(2)
      val storeState = cols(3)

      val storeLocation = Location(storeZipcode, storeCity, storeState)
      val store = Store(storeId, storeZipcode)

      val customerId = cols(4).toInt
      val firstName = cols(5)
      val lastName = cols(6)
      val customerZipcode = cols(7)
      val customerCity = cols(8)
      val customerState = cols(9)

      val customerLocation = Location(customerZipcode, customerCity,
        customerState)
      val customer = Customer(customerId, firstName, lastName,
        customerZipcode)

      val txId = cols(10).toInt
      val df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.US)
      val txDate = df.parse(cols(11))
      val txCal = Calendar.getInstance(Locale.US)
      txCal.setTime(txDate)
      txCal.set(Calendar.MILLISECOND, 0)
      val txProduct = cols(12)

      val transaction = TransactionProduct(customerId, txId,
        storeId, txCal, txProduct)

      (store, storeLocation, customer, customerLocation, transaction)
    }

    splitRecords
  }

  def normalizeData(rawRecords: RDD[(Store, Location, Customer,
    Location, TransactionProduct)]): (RDD[Location], RDD[Store],
      RDD[Customer], RDD[Product], RDD[Transaction]) = {
    // extract stores
    val storeRDD = rawRecords.map {
      case (store, _, _, _, _) =>
        store
    }.distinct()

    // extract store locations
    val storeLocationRDD = rawRecords.map {
      case (_, location, _, _, _) =>
        location
    }.distinct()

    // extract customers
    val customerRDD = rawRecords.map {
      case (_, _, customer, _, _) =>
        customer
    }.distinct()

    // extract customer locations
    val customerLocationRDD = rawRecords.map {
      case (_, _, _, location, _) =>
        location
    }.distinct()

    // extract and normalize products
    val productStringRDD = rawRecords.map {
      case (_, _, _, _, tx) =>
        tx.product
    }
    .distinct()
    .zipWithUniqueId()

    val productRDD = productStringRDD.map {
      case (productString, id) =>
        // products are key-value pairs of the form:
        // key=value;key=value;
        val prodKV = productString
          .split(";")
          .filter(_.trim().length > 0)
          .map { pair =>
            val pairString = pair.split("=")
            (pairString(0), pairString(1))
           }
          .toMap

        Product(id, prodKV("category"), prodKV)
    }

    // extract transactions, map products to productIds
    val productTransactionRDD = rawRecords.map {
      case (_, _, _, _, tx) =>
       (tx.product, tx)
    }

    val joinedRDD: RDD[(String, (TransactionProduct, Long))]
      = productTransactionRDD.join(productStringRDD)

    val transactionRDD = joinedRDD.map {
      case (productString, (tx, productId)) =>
        Transaction(tx.customerId, tx.transactionId,
          tx.storeId, tx.dateTime, productId)
    }

    val locationRDD = storeLocationRDD.
      union(customerLocationRDD).
      distinct()

    (locationRDD, storeRDD, customerRDD, productRDD, transactionRDD)
  }

  /**
   * Runs the ETL and returns the total number of locations,stores,customers,products,transactions.
   */
  def run(sc:SparkContext, parameters:ETLParameters) : (Long,Long,Long,Long,Long) = {
    val rawStringRDD = readRawData(sc, parameters.inputDir)
    val rawRecordRDD = parseRawData(rawStringRDD)
    val normalizedRDDs = normalizeData(rawRecordRDD)

    val locationRDD = normalizedRDDs._1
    val storeRDD = normalizedRDDs._2
    val customerRDD = normalizedRDDs._3
    val productRDD = normalizedRDDs._4
    val transactionRDD = normalizedRDDs._5

    IOUtils.save(parameters.outputDir, locationRDD, storeRDD,
      customerRDD, productRDD, transactionRDD)

    return (locationRDD.count(),
        storeRDD.count(),
        customerRDD.count(),
        productRDD.count(),
        transactionRDD.count()
        );
  }

  def main(args: Array[String]) {
    val parameters = parseArgs(args)

    println("Creating SparkConf")

    val conf = new SparkConf().setAppName("BPS Data Generator")

    println("Creating SparkContext")

    val sc = new SparkContext(conf)

    run(sc, parameters)

    sc.stop()
  }
}
