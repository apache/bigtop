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

import java.sql.Timestamp

/**
 * Statistics phase.  Represents JSON for a front end.
 */
case class StatisticsTxByMonth(month: Int, count: Long)

case class StatisticsTxByProductZip(productId: Long, zipcode: String, count: Long)

case class StatisticsTxByProduct(count: Long, productId: Long)

case class Statistics(totalTransactions: Long,
  transactionsByMonth: Array[StatisticsTxByMonth],
  transactionsByProduct: Array[StatisticsTxByProduct],
  transactionsByProductZip: Array[StatisticsTxByProductZip],
  productDetails: Array[Product])

case class RawData(storeId: Long, storeZipcode: String, storeCity: String, storeState: String,
  customerId: Long, firstName: String, lastName: String,
  customerZipcode: String, customerCity: String, customerState: String,
  txId: Long, txDate: Timestamp, txProduct: String)

case class Customer(customerId: Long, firstName: String, lastName: String, zipcode: String)

case class Location(zipcode: String, city: String, state: String)

case class Product(productId: Long, category: String, attributes: Map[String, String])

case class Store(storeId: Long, zipcode: String)

case class Transaction(customerId: Long, transactionId: Long, storeId: Long, dateTime: Timestamp, productId: Long)

case class UserProductRecommendations(customerId: Long, productIds: Array[Long])

case class ProductRecommendations(customers: Array[Customer], products: Array[Product], recommendations: Array[UserProductRecommendations])
