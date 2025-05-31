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

import org.apache.bigtop.bigpetstore.spark.datamodel.{IOUtils, RawData}
import org.apache.spark.sql.{DataFrame, Encoders, SparkSession}

case class ETLParameters(inputDir: String, outputDir: String)

object SparkETL {

  private val NPARAMS = 2

  private def printUsage(): Unit = {
    val usage: String = "BigPetStore Spark ETL\n" +
      "\n" +
      "Usage: spark-submit ... inputDir outputDir\n" +
      "\n" +
      "inputDir - (string) directory of raw transaction records from data generator\n" +
      "outputDir - (string) directory to write normalized records\n"

    println(usage)
  }

  def parseArgs(args: Array[String]): ETLParameters = {
    if (args.length != NPARAMS) {
      printUsage()
      System.exit(1)
    }

    ETLParameters(args(0), args(1))
  }

  def readRawData(spark: SparkSession, inputDir: String) =
    spark.read.option("timestampFormat", "EEE MMM dd kk:mm:ss z yyyy").schema(Encoders.product[RawData].schema).csv(inputDir + "/transactions")

  def normalizeData(spark: SparkSession, rawDF: DataFrame) = {
    rawDF.createOrReplaceTempView("RawData")

    val storeDF = spark.sql("SELECT DISTINCT storeId, storeZipcode zipcode FROM RawData")

    val locationDF = spark.sql(
      """SELECT DISTINCT(*) FROM (
        |  SELECT storeZipcode zipcode, storeCity city, storeState state FROM RawData
        |  UNION
        |  SELECT customerZipcode zipcode, customerCity city, customerState state FROM RawData
        |)
        |""".stripMargin)

    val customerDF = spark.sql("SELECT DISTINCT customerId, firstName, lastName, customerZipcode zipcode FROM RawData")

    val indexedRawProductDF = spark.sql(
      """SELECT cast(ROW_NUMBER() OVER (ORDER BY product) as BIGINT) productId, product
        |FROM (SELECT DISTINCT txProduct product FROM RawData)
        |""".stripMargin)
    indexedRawProductDF.createOrReplaceTempView("Product")

    val transactionDF = spark.sql(
      """SELECT customerId, txId transactionId, storeId, txDate dateTime, productId
        |FROM RawData a
        |JOIN Product b
        |  ON a.txProduct = b.product
        |""".stripMargin)

    val productDF = spark.sql(
      """SELECT productId, attributes['category'] category, attributes
        |FROM (
        |  SELECT productId, map_filter(str_to_map(product, ';', '='), (k, v) -> 0 < length(k)) attributes
        |  FROM Product
        |)
        |""".stripMargin)

    (locationDF, storeDF, customerDF, productDF, transactionDF)
  }

  /**
   * Runs the ETL and returns the total number of locations, stores, customers, products, transactions.
   */
  def run(spark: SparkSession, parameters: ETLParameters): (Long, Long, Long, Long, Long) = {
    val rawDF = readRawData(spark, parameters.inputDir)
    val (locationDF, storeDF, customerDF, productDF, transactionDF) = normalizeData(spark, rawDF)

    IOUtils.save(parameters.outputDir, locationDF, storeDF, customerDF, productDF, transactionDF)

    (locationDF.count(), storeDF.count(), customerDF.count(), productDF.count(), transactionDF.count())
  }

  def main(args: Array[String]): Unit = {
    val parameters = parseArgs(args)

    println("Creating SparkSession")

    val spark = SparkSession.builder.appName("BPS Data Generator")
      .config("spark.sql.legacy.timeParserPolicy", "LEGACY").getOrCreate()

    run(spark, parameters)

    spark.stop()
  }
}
