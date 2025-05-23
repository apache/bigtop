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

package org.apache.bigtop.bigpetstore.spark.analytics

import java.io.File

import org.apache.spark.sql._

import org.apache.bigtop.bigpetstore.spark.datamodel._

object PetStoreStatistics {

  private def printUsage(): Unit = {
    val usage: String = "BigPetStore Analytics Module." +
      "\n" +
      "Usage: spark-submit ... inputDir outputFile\n " +
      "inputDir - (string) Path to ETL'd data\n" +
      "outputFile - (string) is a JSON file.  For schema, see the code.\n"

    System.err.println(usage)
  }

  /**
   * Scala details. Some or None are an idiomatic way, in scala, to
   * return an optional value.  This allows us to signify, to the caller, that the
   * method may fail.  The caller can decide how to deal with failure (i.e. using getOrElse).
   *
   * @param args
   * @return
   */
  def parseArgs(args: Array[String]): (Option[String], Option[String]) = {
    if (args.length < 1) {
      (None, None)
    } else if (args.length == 1) {
      (Some(args(0)), None)
    } else {
      (Some(args(0)), Some(args(1)))
    }
  }

  def queryTxByMonth(spark: SparkSession): Array[StatisticsTxByMonth] = {
    import spark.implicits._
    val results = spark.sql("SELECT month(dateTime) month, count(*) count FROM Transactions GROUP BY month")
    results.show()
    results.as[StatisticsTxByMonth].collect()
  }

  def queryTxByProductZip(spark: SparkSession): Array[StatisticsTxByProductZip] = {
    import spark.implicits._
    val results: DataFrame = spark.sql(
      """SELECT productId, zipcode, count(*) count
        |FROM Transactions t
        |JOIN Stores s
        |ON t.storeId = s.storeId
        |GROUP BY productId, zipcode""".stripMargin)
    results.show()
    results.as[StatisticsTxByProductZip].collect()
  }

  def queryTxByProduct(spark: SparkSession): Array[StatisticsTxByProduct] = {
    import spark.implicits._
    val results = spark.sql("SELECT productId, count(*) count FROM Transactions GROUP BY productId")
    results.show()
    results.as[StatisticsTxByProduct].collect()
  }


  def runQueries(spark: SparkSession, locationDF: DataFrame, storeDF: DataFrame,
                 customerDF: DataFrame, productDF: DataFrame, transactionDF: DataFrame): Statistics = {
    import spark.implicits._

    locationDF.createOrReplaceTempView("Locations")
    storeDF.createOrReplaceTempView("Stores")
    customerDF.createOrReplaceTempView("Customers")
    productDF.createOrReplaceTempView("Product")
    transactionDF.createOrReplaceTempView("Transactions")

    val txByMonth = queryTxByMonth(spark)
    val txByProduct = queryTxByProduct(spark)
    val txByProductZip = queryTxByProductZip(spark)

    Statistics(
      txByMonth.map(_.count).sum, // Total number of transactions
      txByMonth,
      txByProduct,
      txByProductZip,
      productDF.as[Product].collect()) // Product details
  }

  /**
   * We keep a "run" method which can be called easily from tests and also is used by main.
   */
  def run(txInputDir: String, statsOutputFile: String, spark: SparkSession): Unit = {

    System.out.println("Running w/ input = " + txInputDir)

    System.out.println("input : " + txInputDir)
    val (locationDF, storeDF, customerDF, productDF, transactionDF) = IOUtils.load(spark, txInputDir)

    val stats = runQueries(spark, locationDF, storeDF, customerDF, productDF, transactionDF)

    IOUtils.saveLocalAsJSON(new File(statsOutputFile), stats)

    System.out.println("Output JSON Stats stored : " + statsOutputFile)
  }

  def main(args: Array[String]): Unit = {
    // Get or else : On failure (else) we exit.
    val (inputPath, outputPath) = parseArgs(args)

    if (!(inputPath.isDefined && outputPath.isDefined)) {
      printUsage()
      System.exit(1)
    }

    val spark = SparkSession.builder.appName("PetStoreStatistics").getOrCreate()

    run(inputPath.get, outputPath.get, spark)

    spark.stop()
  }
}
