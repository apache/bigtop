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
import java.sql.Timestamp

import scala.Nothing

import org.apache.spark.sql._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

import org.joda.time.DateTime
import org.json4s.JsonDSL.WithBigDecimal._

import org.apache.bigtop.bigpetstore.spark.datamodel._

object PetStoreStatistics {

    private def printUsage() {
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
   * @param args
   * @return
   */
    def parseArgs(args: Array[String]):(Option[String],Option[String]) = {
      if(args.length < 1) {
        (None, None)
      } else if (args.length == 1) {
        (Some(args(0)), None)
      } else {
        (Some(args(0)), Some(args(1)))
      }
    }

  def productMap(r:Array[Product]) : Map[Long,Product] = {
    r map (prod => prod.productId -> prod) toMap
  }

  def queryTxByMonth(sqlContext: SQLContext): Array[StatisticsTxByMonth] = {
    import sqlContext._

    val results: DataFrame = sql("SELECT count(*), month FROM Transactions GROUP BY month")
    val transactionsByMonth = results.collect()
    for(x<-transactionsByMonth){
      println(x)
    }

    transactionsByMonth.map { r =>
      StatisticsTxByMonth(r.getInt(1), r.getLong(0))
    }
  }

  def queryTxByProductZip(sqlContext: SQLContext): Array[StatisticsTxByProductZip] = {
    import sqlContext._

    val results: DataFrame = sql(
      """SELECT count(*) c, productId, zipcode
FROM Transactions t
JOIN Stores s ON t.storeId = s.storeId
GROUP BY productId, zipcode""")

    val groupedProductZips = results.collect()

    //get list of all transactionsData
    for(x<-groupedProductZips){
      println("grouped product:zip " + x)
    }

    //Map JDBC Row into a Serializable case class.
    groupedProductZips.map { r =>
      StatisticsTxByProductZip(r.getLong(1),r.getString(2),r.getLong(0))
    }
  }

  def queryTxByProduct(sqlContext: SQLContext): Array[StatisticsTxByProduct] = {
    import sqlContext._

    val results: DataFrame = sql(
      """SELECT count(*) c, productId FROM Transactions GROUP BY productId""")

    val groupedProducts = results.collect()

    //Map JDBC Row into a Serializable case class.
    groupedProducts.map { r =>
      StatisticsTxByProduct(r.getLong(1),r.getLong(0))
    }
  }


  def runQueries(r:(RDD[Location], RDD[Store], RDD[Customer], RDD[Product],
    RDD[Transaction]), sc: SparkContext): Statistics = {

    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext._
    import sqlContext.implicits._

    // Transform the Non-SparkSQL Calendar into a SparkSQL-friendly field.
    val mappableTransactions:RDD[TransactionSQL] =
      r._5.map { trans => trans.toSQL() }

    r._1.toDF().registerTempTable("Locations")
    r._2.toDF().registerTempTable("Stores")
    r._3.toDF().registerTempTable("Customers")
    r._4.toDF().registerTempTable("Product")
    mappableTransactions.toDF().registerTempTable("Transactions")


    val txByMonth = queryTxByMonth(sqlContext)
    val txByProduct = queryTxByProduct(sqlContext)
    val txByProductZip = queryTxByProductZip(sqlContext)

    return Statistics(
      txByMonth.map { s => s.count }.reduce(_+_),  // Total number of transactions
      txByMonth,
      txByProduct,
      txByProductZip,
      r._4.collect()) // Product details
  }

    /**
    * We keep a "run" method which can be called easily from tests and also is used by main.
    */
    def run(txInputDir:String, statsOutputFile:String,
      sc:SparkContext) {

      System.out.println("Running w/ input = " + txInputDir)

      System.out.println("input : " + txInputDir)
      val etlData = IOUtils.load(sc, txInputDir)

      val stats = runQueries(etlData, sc)

      IOUtils.saveLocalAsJSON(new File(statsOutputFile), stats)

      System.out.println("Output JSON Stats stored : " + statsOutputFile)
    }

  def main(args: Array[String]) {
    // Get or else : On failure (else) we exit.
    val (inputPath,outputPath) = parseArgs(args)

    if(! (inputPath.isDefined && outputPath.isDefined)) {
      printUsage()
      System.exit(1)
    }

    val sc = new SparkContext(new SparkConf().setAppName("PetStoreStatistics"))

    run(inputPath.get, outputPath.get, sc)

    sc.stop()
  }
}
