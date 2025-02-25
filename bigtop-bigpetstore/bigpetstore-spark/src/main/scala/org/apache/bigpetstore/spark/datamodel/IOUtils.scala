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

import java.io.File
import java.nio.file.Files
import java.nio.charset.StandardCharsets

import org.apache.spark.SparkContext
import org.apache.spark.rdd._

import org.json4s.jackson.Serialization
import org.json4s._
import org.json4s.jackson.Serialization.{read, write}

/**
  * Utility functions for loading and saving data model RDDs.
  */
object IOUtils {
  private val LOCATION_DIR = "locations"
  private val STORE_DIR = "stores"
  private val CUSTOMER_DIR = "customers"
  private val PRODUCT_DIR = "products"
  private val TRANSACTION_DIR = "transactions"

  private val ANALYTICS_STATS_DIR = "analytics_stats"

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
    productRDD: RDD[Product], transactionRDD: RDD[Transaction]): Unit = {

    locationRDD.saveAsObjectFile(outputDir + "/" + LOCATION_DIR)
    storeRDD.saveAsObjectFile(outputDir + "/" + STORE_DIR)
    customerRDD.saveAsObjectFile(outputDir + "/" + CUSTOMER_DIR)
    productRDD.saveAsObjectFile(outputDir + "/" + PRODUCT_DIR)
    transactionRDD.saveAsObjectFile(outputDir + "/" + TRANSACTION_DIR)
  }

  def saveLocalAsJSON(outputDir: File, statistics: Statistics): Unit = {
    //load the write/read methods.
    implicit val formats = Serialization.formats(NoTypeHints)
    val json:String = write(statistics)
    Files.write(outputDir.toPath, json.getBytes(StandardCharsets.UTF_8))
  }

  def readLocalAsStatistics(jsonFile: File):Statistics = {
    //load the write/read methods.
    implicit val formats = Serialization.formats(NoTypeHints)
    //Read file as String, and serialize it into Stats object.
    //See http://json4s.org/ examples.
    read[Statistics](scala.io.Source.fromFile(jsonFile).getLines().reduceLeft(_+_))
  }

  def saveLocalAsJSON(outputDir: File, recommendations:ProductRecommendations): Unit = {
    //load the write/read methods.
    implicit val formats = Serialization.formats(NoTypeHints)
    val json:String = write(recommendations)
    Files.write(outputDir.toPath, json.getBytes(StandardCharsets.UTF_8))
  }

  def readLocalAsProductRecommendations(jsonFile: File):ProductRecommendations = {
    //load the write/read methods.
    implicit val formats = Serialization.formats(NoTypeHints)
    //Read file as String, and serialize it into Stats object.
    //See http://json4s.org/ examples.
    read[ProductRecommendations](scala.io.Source.fromFile(jsonFile).getLines().reduceLeft(_+_))
  }


  /**
    * Load RDDs of the data model from Sequence files.
    *
    * @param sc SparkContext
    * @param inputDir Directory containing Sequence files
    *
    * TODO Should take path, not string, this makes input validation complex.
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
