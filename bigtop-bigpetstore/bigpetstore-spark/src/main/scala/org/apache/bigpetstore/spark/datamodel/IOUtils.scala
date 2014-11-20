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

package org.apache.bigtop.bigpetstore.spark.datamodel

import java.util.Date

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

import org.apache.bigtop.bigpetstore.spark.datamodel._

/**
  * Utility functions for loading and saving data model RDDs.
  */
object IOUtils {
  private val LOCATION_DIR = "locations"
  private val STORE_DIR = "stores"
  private val CUSTOMER_DIR = "customers"
  private val PRODUCT_DIR = "products"
  private val TRANSACTION_DIR = "transactions"

  /**
    * Save RDDs of the data model as Sequence files.
    *
    * @param outputDir Output directory
    * @param locationRDD RDD of Location objects
    * @param storeRDD RDD of Store objects
    * @param customerRDD RDD of Customer objects
    * @param productRDD RDD of Product objects
    * @param transactionRDD RDD of Transaction objects
    */
  def save(outputDir: String, locationRDD: RDD[Location],
    storeRDD: RDD[Store], customerRDD: RDD[Customer],
    productRDD: RDD[Product], transactionRDD: RDD[Transaction]) {

    locationRDD.saveAsObjectFile(outputDir + "/" + LOCATION_DIR)
    storeRDD.saveAsObjectFile(outputDir + "/" + STORE_DIR)
    customerRDD.saveAsObjectFile(outputDir + "/" + CUSTOMER_DIR)
    productRDD.saveAsObjectFile(outputDir + "/" + PRODUCT_DIR)
    transactionRDD.saveAsObjectFile(outputDir + "/" + TRANSACTION_DIR)
  }

  /**
    * Load RDDs of the data model from Sequence files.
    *
    * @param sc SparkContext
    * @param inputDir Directory containing Sequence files
    */
  def load(sc: SparkContext, inputDir: String): (RDD[Location], RDD[Store],
    RDD[Customer], RDD[Product], RDD[Transaction]) = {

    val locationRDD: RDD[Location] =
      sc.objectFile(inputDir + "/" + LOCATION_DIR)

    val storeRDD: RDD[Store] =
      sc.objectFile(inputDir + "/" + STORE_DIR)

    val customerRDD: RDD[Customer] =
      sc.objectFile(inputDir + "/" + CUSTOMER_DIR)

    val productRDD: RDD[Product] =
      sc.objectFile(inputDir + "/" + PRODUCT_DIR)

    val transactionRDD: RDD[Transaction] =
      sc.objectFile(inputDir + "/" + TRANSACTION_DIR)

    (locationRDD, storeRDD, customerRDD, productRDD, transactionRDD)
  }

}
