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

import org.apache.bigtop.bigpetstore.spark.datamodel._
import org.apache.spark.ml.recommendation.{ALS, ALSModel}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.io.File

object RecommendProducts {

  case class PRParameters(inputDir: String, outputFile: String)

  private def printUsage(): Unit = {
    val usage = "BigPetStore Product Recommendation Module\n" +
      "\n" +
      "Usage: transformed_data recommendations\n" +
      "\n" +
      "transformed_data - (string) directory of ETL'd data\n" +
      "recommendations - (string) output file of recommendations\n"

    println(usage)
  }

  def parseArgsOrDie(args: Array[String]): PRParameters = {
    if (args.length != 2) {
      printUsage();
      System.exit(1)
    }

    PRParameters(args(0), args(1))
  }

  def prepareRatings(spark: SparkSession, tx: DataFrame): DataFrame = {
    tx.createOrReplaceTempView("Transactions")
    spark.sql("SELECT customerId, productId, count(*) rating FROM Transactions GROUP BY customerId, productId")
  }

  def trainModel(ratings: DataFrame, nIterations: Int, rank: Int, alpha: Double, lambda: Double) =
    new ALS().setImplicitPrefs(true)
      .setAlpha(alpha)
      .setMaxIter(nIterations)
      .setRank(rank)
      .setRegParam(lambda)
      .setUserCol("customerId")
      .setItemCol("productId")
      .setRatingCol("rating")
      .fit(ratings.cache())

  def recommendProducts(spark: SparkSession, model: ALSModel, nRecommendations: Int) = {
    model.recommendForAllUsers(nRecommendations).createOrReplaceTempView("Recommendations")
    import spark.implicits._
    spark.sql(
      """SELECT customerId, collect_list(productId) productIds
        |FROM (
        |  SELECT customerId, productId, rating
        |  FROM (SELECT customerId, inline(recommendations) FROM Recommendations)
        |  DISTRIBUTE BY customerId SORT BY rating DESC
        |)
        |GROUP BY customerId
        |""".stripMargin).as[UserProductRecommendations].collect()
  }

  /**
   * We keep a "run" method which can be called easily from tests and also is used by main.
   */
  def run(txInputDir: String, recOutputFile: String, spark: SparkSession,
          nIterations: Int = 20, alpha: Double = 40.0, rank: Int = 10, lambda: Double = 1.0,
          nRecommendations: Int = 5): Unit = {

    println("input : " + txInputDir)
    println(spark)
    val (_, _, customerDF, productDF, transactionDF) = IOUtils.load(spark, txInputDir)
    System.out.println("Transaction count = " + transactionDF.count())

    val ratings = prepareRatings(spark, transactionDF)
    val model = trainModel(ratings, nIterations, rank, alpha, lambda)
    val userProdRec = recommendProducts(spark, model, nRecommendations)

    import spark.implicits._
    val prodRec = ProductRecommendations(customerDF.as[Customer].collect(), productDF.as[Product].collect(), userProdRec)

    IOUtils.saveLocalAsJSON(new File(recOutputFile), prodRec)
  }

  def main(args: Array[String]): Unit = {
    val params: PRParameters = parseArgsOrDie(args)

    val spark = SparkSession.builder.appName("BPS Product Recommendations").getOrCreate()

    run(params.inputDir, params.outputFile, spark)

    spark.stop()
  }
}
