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

import _root_.org.apache.spark.SparkConf
import org.apache.bigtop.bigpetstore.spark.datamodel.{IOUtils, Statistics,Transaction}
import scala.Nothing;
import com.github.rnowling.bps.datagenerator.datamodels.inputs.ZipcodeRecord
import com.github.rnowling.bps.datagenerator.datamodels._
import com.github.rnowling.bps.datagenerator.{DataLoader,StoreGenerator,CustomerGenerator => CustGen, PurchasingProfileGenerator,TransactionGenerator}
import com.github.rnowling.bps.datagenerator.framework.SeedFactory
import org.apache.hadoop.fs.Path
import scala.collection.JavaConversions._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

import java.util.ArrayList
import scala.util.Random
import java.io.File
import java.util.Date

object PetStoreStatistics {

    private def printUsage() {
      val usage: String =
        "BigPetStore Analytics Module.\n" +
          "Usage: inputDir\n" ;

      System.err.println(usage)
    }

  /**
   * Scala details. Some or None are an idiomatic way, in scala, to
   * return an optional value.  This allows us to signify, to the caller, that the
   * method may fail.  The caller can decide how to deal with failure (i.e. using getOrElse).
   * @param args
   * @return
   */
    def parseArgs(args: Array[String]):Option[Path] = {
      if(args.length != 1) {
        printUsage();
        return None;
      }
      //success, return path.
      Some(new Path(args(0)));
    }

    /**
     * Here we generate an RDD of all the petstore transactions,
     * by generating the static data first (stores, customers, ...)
     * followed by running the simulation as a distributed spark task.
     */
    def totalTransactions(r:(_,_,_,_,RDD[Transaction]), sc: SparkContext): Statistics = {
      return Statistics(r._5.count());
    }

    /**
    * We keep a "run" method which can be called easily from tests and also is used by main.
    */
    def run(transactionsInputDir:String, sc:SparkContext): Boolean = {
      System.out.println("input : " + transactionsInputDir);
      val t=totalTransactions(IOUtils.load(sc,transactionsInputDir), sc);
      System.out.println("Transaction count = " + t);
      sc.stop()
      true;
    }

    def main(args: Array[String]) {
      // Get or else : On failure (else) we exit.
      val inputPath = parseArgs(args).getOrElse {
        System.exit(1);
      };

      System.out.println("Running w/ input = " + inputPath);
      val conf = new SparkConf().setAppName("BPS Data Generator")
      run(inputPath.toString,new SparkContext(conf));
    }

}