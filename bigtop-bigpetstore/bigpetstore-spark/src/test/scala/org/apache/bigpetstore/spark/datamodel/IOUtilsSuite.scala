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

import java.nio.file.Files
import java.util.Calendar
import java.util.Locale

import org.apache.spark.{SparkContext, SparkConf}

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

// hack for running tests with Gradle
@RunWith(classOf[JUnitRunner])
class IOUtilsSuite extends AnyFunSuite with BeforeAndAfterAll {

  val conf = new SparkConf().setAppName("BPS Data Generator Test Suite").setMaster("local[2]")
  val sc = new SparkContext(conf)

  override def afterAll(): Unit = {
    sc.stop()
  }

  test("Saving & Loading data") {

    val tmpDir = Files.createTempDirectory("ioUtilsSuite").toFile().toString()

    val locations = Array(Location("11111", "Sunnyvale", "CA"),
      Location("22222", "Margate", "FL"))
    val customers = Array(Customer(1L, "James", "Madison", "11111"),
      Customer(2L, "George", "Washington", "11111"),
      Customer(3L, "Matt", "Steele", "22222"),
      Customer(4L, "George", "Foreman", "22222"))
    val products = Array(
      Product(1L, "dog food", Map("brand" -> "Dog Days", "flavor" -> "Chicken & Rice")),
      Product(2L, "dog food", Map("brand" -> "Wellfed", "flavor" -> "Pork & Beans")),
      Product(3L, "cat food", Map("brand" -> "Fatty Catty", "flavor" -> "Squirrel")))

    val stores = Array(Store(1L, "11111"), Store(2L, "22222"), Store(3L, "11111"))

    val txDate1 = Calendar.getInstance(Locale.US)
    val txDate2 = Calendar.getInstance(Locale.US)
    val txDate3 = Calendar.getInstance(Locale.US)

    val transactions = Array(
      Transaction(1L, 1L, 1L, txDate1, 1L),
      Transaction(1L, 1L, 1L, txDate1, 2L),
      Transaction(2L, 1L, 2L, txDate2, 3L),
      Transaction(2L, 2L, 1L, txDate3, 1L))

    val locationRDD = sc.parallelize(locations)
    val storeRDD = sc.parallelize(stores)
    val customerRDD = sc.parallelize(customers)
    val productRDD = sc.parallelize(products)
    val transactionRDD = sc.parallelize(transactions)

    IOUtils.save(tmpDir, locationRDD, storeRDD, customerRDD, productRDD,
      transactionRDD)

    val rdds = IOUtils.load(sc, tmpDir)

    val readLocationRDD = rdds._1
    val readStoreRDD = rdds._2
    val readCustomerRDD = rdds._3
    val readProductRDD = rdds._4
    val readTransactionRDD = rdds._5

    assert(locationRDD.collect().toSet === readLocationRDD.collect().toSet)
    assert(storeRDD.collect().toSet === readStoreRDD.collect().toSet)
    assert(customerRDD.collect().toSet === readCustomerRDD.collect().toSet)
    assert(productRDD.collect().toSet === readProductRDD.collect().toSet)
    assert(transactionRDD.collect().toSet === readTransactionRDD.collect().toSet)
  }
}
