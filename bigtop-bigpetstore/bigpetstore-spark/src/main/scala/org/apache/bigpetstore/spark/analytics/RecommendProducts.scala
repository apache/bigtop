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
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd._
import org.apache.spark.mllib.recommendation._

import java.io.File

case class PRParameters(inputDir: String, outputFile: String)

object RecommendProducts {

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
    if(args.length != 2) {
      printUsage();
      System.exit(1)
    }

    PRParameters(args(0), args(1))
  }

  def prepareRatings(tx: RDD[Transaction]): RDD[Rating] = {
    val productPairs = tx.map { t => ((t.customerId, t.productId), 1) }
    val pairCounts = productPairs.reduceByKey { (v1, v2) => v1 + v2 }
    val ratings = pairCounts.map { p => Rating(p._1._1.toInt, p._1._2.toInt, p._2) }

    ratings
  }

  def trainModel(ratings: RDD[Rating], nIterations: Int, rank: Int, alpha: Double,
    lambda: Double): MatrixFactorizationModel = {
    ratings.cache()
    val model = ALS.trainImplicit(ratings, nIterations, rank, lambda, alpha)

    model
  }

  def recommendProducts(customers: RDD[Customer],
    model: MatrixFactorizationModel, sc: SparkContext, nRecommendations: Int):
      Array[UserProductRecommendations] = {

    customers.collect().map { c =>
      val ratings = model.recommendProducts(c.customerId.toInt, nRecommendations)

      val productIds = ratings.map { r => r.product.toLong}

      UserProductRecommendations(c.customerId, productIds.toArray)
    }
  }

  /**
    * We keep a "run" method which can be called easily from tests and also is used by main.
    */
  def run(txInputDir: String, recOutputFile: String, sc: SparkContext,
  nIterations: Int = 20, alpha: Double = 40.0, rank:Int = 10, lambda: Double = 1.0,
  nRecommendations: Int = 5): Unit = {

    println("input : " + txInputDir)
    println(sc)
    val rdds = IOUtils.load(sc, txInputDir)
    val tx = rdds._5
    val products = rdds._4
    val customers = rdds._3
    System.out.println("Transaction count = " + tx.count())

    val ratings = prepareRatings(tx)
    val model = trainModel(ratings, nIterations, rank, alpha, lambda)
    val userProdRec = recommendProducts(customers, model, sc, nRecommendations)

    val prodRec = ProductRecommendations(customers.collect(),
      products.collect(),
      userProdRec)

    IOUtils.saveLocalAsJSON(new File(recOutputFile), prodRec)
  }

  def main(args: Array[String]): Unit = {
    val params: PRParameters = parseArgsOrDie(args)

    val conf = new SparkConf().setAppName("BPS Product Recommendations")
    val sc = new SparkContext(conf)

    run(params.inputDir, params.outputFile, sc)

    sc.stop()
  }

}
