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


import java.sql.Timestamp

import org.joda.time.DateTime

import _root_.org.apache.spark.SparkConf
import org.apache.bigtop.bigpetstore.spark.datamodel._
import org.apache.spark.sql._;
import scala.Nothing;
import org.apache.hadoop.fs.Path
import scala.collection.JavaConversions._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

import java.util.{Calendar, ArrayList, Date}
import scala.util.Random
import java.io.File
import org.json4s.JsonDSL.WithBigDecimal._

object PetStoreStatistics {

    private def printUsage() {
      val usage: String =
        "BigPetStore Analytics Module. Usage: inputDir, outputDir.\n " +
        "Ouptut is a JSON file in outputDir.  For schema, see the code." ;

      System.err.println(usage)
    }

  /**
   * Scala details. Some or None are an idiomatic way, in scala, to
   * return an optional value.  This allows us to signify, to the caller, that the
   * method may fail.  The caller can decide how to deal with failure (i.e. using getOrElse).
   * @param args
   * @return
   */
    def parseArgs(args: Array[String]):(Option[Path],Option[File]) = {
      (if(args.length < 1) { System.err.println("ERROR AT ARG 1: Missing INPUT path"); None } else Some(new Path(args(0))),
       if(args.length < 2) { System.err.println("ERROR AT ARG 2: Missing OUTPUT path");; None } else Some(new File(args(1))))
    }

  def productMap(r:Array[Product]) : Map[Long,Product] = {
    r map (prod => prod.productId -> prod) toMap
  }

    def totalTransactions(r:(RDD[Location], RDD[Store], RDD[Customer], RDD[Product], RDD[Transaction]),
                          sc: SparkContext): Statistics = {
      val sqlContext = new org.apache.spark.sql.SQLContext(sc);

      import sqlContext._;

      /**
       * Transform the non-sparksql mappable calendar
       * into a spark sql freindly field.
       */
      val mappableTransactions:RDD[TransactionSQL] =
        /**
        * Map the RDD[Transaction] -> RDD[TransactionSQL] so that we can run SparkSQL against it.
        */
        r._5.map(trans => trans.toSQL())

        mappableTransactions.registerTempTable("transactions");

        r._2.registerTempTable("Stores")

      val results: SchemaRDD = sql("SELECT month,count(*) FROM transactions group by month")
      val transactionsByMonth = results.collect();
      for(x<-transactionsByMonth){
        println(x);
      }

      val results2: SchemaRDD = sql(
        """SELECT count(*) c, productId , zipcode
FROM transactions t
JOIN Stores s ON t.storeId = s.storeId
GROUP BY productId, zipcode""")
      val groupedProductZips = results2.collect();

      //get list of all transactionsData
      for(x<-groupedProductZips){
        println("grouped product:zip " + x);
      }

      return Statistics(
        results.count(), // Total number of transaction
        results2.collect().map(r => {
          //Map JDBC Row into a Serializable case class.
          StatisticsTrByZip(r.getLong(0),r.getLong(1),r.getString(2))
        }),
        r._4.collect()); // Product details.
    }

    /**
    * We keep a "run" method which can be called easily from tests and also is used by main.
    */
    def run(transactionsInputDir:String, sc:SparkContext): Statistics = {
      System.out.println("input : " + transactionsInputDir);
      val stats = totalTransactions(IOUtils.load(sc,transactionsInputDir), sc);
      sc.stop()
      stats
    }

  def main(args: Array[String]) {
      main(
        args,
        new SparkContext(new SparkConf().setAppName("PetStoreStatistics")));
  }

  def main(args: Array[String], context:SparkContext) = {
      // Get or else : On failure (else) we exit.
      val (inputPath,outputPath)= parseArgs(args);

      if(! (inputPath.isDefined && outputPath.isDefined)) {
        printUsage()
        System.exit(1)
      }

      System.out.println("Running w/ input = " + inputPath);

      val stats:Statistics = run(inputPath.get.toUri.getPath, context);

      IOUtils.saveLocalAsJSON(outputPath.get, stats)

      System.out.println("Output JSON Stats stored : " + outputPath.get);

    }

}