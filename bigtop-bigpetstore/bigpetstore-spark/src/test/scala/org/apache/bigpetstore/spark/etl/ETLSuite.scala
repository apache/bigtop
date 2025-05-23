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

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith
import org.apache.bigtop.bigpetstore.spark.datamodel._
import org.apache.spark.sql.SparkSession

import java.sql.Timestamp

/**
 * This class tests that, when we read records from the generator, the
 * data model classes are populated with correct information.
 *
 * This is a critical test, since all subsequent phases of BigPetStore will use the
 * datamodel objects (i.e. Transaction, Customer, and so on) for the analytics which
 * we do.
 *
 * Other BigPetStore unit tests may not need to mock / test data with this granular of precision.
 * RunWith annotation is just a hack for running tests with Gradle
 */
@RunWith(classOf[JUnitRunner])
class ETLSuite extends AnyFunSuite with BeforeAndAfterAll {

  case class TransactionProduct(customerId: Long, transactionId: Long,
                                storeId: Long, dateTime: Timestamp, product: String)

  /**
   * TODO : We are using Option monads as a replacement for nulls.
   * Lets move towards immutable spark context instead, if possible ?
   */
  private val spark = SparkSession.builder.appName("BPS Data Generator Test Suite").master("local[*]").getOrCreate()

  var rawRecords: Option[Array[RawData]] = None
  var transactions: Option[Array[Transaction]] = None

  private val stores = Array(Store(5L, "11553"), Store(1L, "98110"), Store(6L, "66067"))
  private val locations =
    Array(
      Location("11553", "Uniondale", "NY"),
      Location("98110", "Bainbridge Islan", "WA"),
      Location("66067", "Ottawa", "KS"),
      Location("20152", "Chantilly", "VA"))
  private val customers = Array(Customer(999L, "Cesareo", "Lamplough", "20152"))
  private val products =
    Array(
      Product(2L, "dry dog food", Map("category" -> "dry dog food", "brand" -> "Happy Pup", "flavor" -> "Fish & Potato", "size" -> "30.0", "per_unit_cost" -> "2.67")),
      Product(3L, "poop bags", Map("category" -> "poop bags", "brand" -> "Dog Days", "color" -> "Blue", "size" -> "60.0", "per_unit_cost" -> "0.21")),
      Product(1L, "dry cat food", Map("category" -> "dry cat food", "brand" -> "Feisty Feline", "flavor" -> "Chicken & Rice", "size" -> "14.0", "per_unit_cost" -> "2.14")))

  override def beforeAll(): Unit = {

    val cal1 = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"), Locale.US)
    val cal2 = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"), Locale.US)
    val cal3 = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"), Locale.US)

    cal1.set(2015, 10, 3, 1, 8, 11)
    cal2.set(2015, 10, 2, 17, 51, 37)
    cal3.set(2015, 9, 12, 4, 29, 46)

    val ts1 = new Timestamp(cal1.getTimeInMillis)
    val ts2 = new Timestamp(cal2.getTimeInMillis)
    val ts3 = new Timestamp(cal3.getTimeInMillis)

    rawRecords = Some(Array(
      RawData(stores(0).storeId, stores(0).zipcode, locations(0).city, locations(0).state, customers(0).customerId,
        customers(0).firstName, customers(0).lastName, customers(0).zipcode, locations(3).city, locations(3).state,
        32L, ts1, "category=dry dog food;brand=Happy Pup;flavor=Fish & Potato;size=30.0;per_unit_cost=2.67;"),

      RawData(stores(1).storeId, stores(1).zipcode, locations(1).city, locations(1).state, customers(0).customerId,
        customers(0).firstName, customers(0).lastName, customers(0).zipcode, locations(3).city, locations(3).state,
        31L, ts2, "category=poop bags;brand=Dog Days;color=Blue;size=60.0;per_unit_cost=0.21;"),

      RawData(stores(2).storeId, stores(2).zipcode, locations(2).city, locations(2).state, customers(0).customerId,
        customers(0).firstName, customers(0).lastName, customers(0).zipcode, locations(3).city, locations(3).state,
        30L, ts3, "category=dry cat food;brand=Feisty Feline;flavor=Chicken & Rice;size=14.0;per_unit_cost=2.14;"),
    ))

    transactions = Some(Array(
      Transaction(999L, 31L, 1L, ts2, 3L),
      Transaction(999L, 30L, 6L, ts3, 1L),
      Transaction(999L, 32L, 5L, ts1, 2L)))
  }

  override def afterAll(): Unit = {
    spark.stop()
  }

  test("Generation of unique sets of transaction attributes") {
    val rawDF = spark.createDataFrame(rawRecords.get)
    val (locationRDD, storeRDD, customerRDD, productRDD, transactionRDD) = SparkETL.normalizeData(spark, rawDF)

    import spark.implicits._
    assert(storeRDD.as[Store].collect().toSet === stores.toSet)
    assert(locationRDD.as[Location].collect().toSet === locations.toSet)
    assert(customerRDD.as[Customer].collect().toSet === customers.toSet)
    assert(productRDD.as[Product].collect().toSet === products.toSet)
    assert(transactionRDD.as[Transaction].collect().toSet === transactions.get.toSet)
  }
}
