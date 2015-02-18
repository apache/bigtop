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
import java.util.Calendar

import org.apache.spark.sql
import org.joda.time.DateTime
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JString, JField, JInt, JObject}

/**
 * Statistics phase.  Represents JSON for a front end.
 */

case class StatisticsTxByMonth(month: Int, count: Long)

case class StatisticsTxByProductZip(productId:Long, zipcode:String, count:Long)

case class StatisticsTxByProduct(count: Long, productId: Long)

case class Statistics(totalTransactions: Long,
  transactionsByMonth: Array[StatisticsTxByMonth],
  transactionsByProduct: Array[StatisticsTxByProduct],
  transactionsByProductZip: Array[StatisticsTxByProductZip],
  productDetails:Array[Product])

case class Customer(customerId: Long, firstName: String,
  lastName: String, zipcode: String)

case class Location(zipcode: String, city: String, state: String)

case class Product(productId: Long, category: String, attributes: Map[String, String])

case class Store(storeId: Long, zipcode: String)

case class Transaction(customerId: Long, transactionId: Long, storeId: Long, dateTime: Calendar, productId: Long){

  /**
   * Convert to TransactionSQL.
   * There possibly could be a conversion.
   */
  def toSQL(): TransactionSQL = {
    val dt = new DateTime(dateTime)
    val ts = new Timestamp(dt.getMillis)
    return TransactionSQL(customerId,transactionId,storeId,
      new Timestamp(
        new DateTime(dateTime).getMillis),
        productId,
        dt.getYearOfEra,dt.getMonthOfYear,dt.getDayOfMonth,dt.getHourOfDay,dt.getMinuteOfHour)
  }
}

/**
 * A Transaction which we can create from the natively stored transactions.
 */
case class TransactionSQL(customerId: Long, transactionId: Long, storeId: Long, timestamp:Timestamp, productId: Long,
                          year:Int, month:Int, day:Int, hour:Int, minute:Int )

case class UserProductRecommendations(customerId: Long, productIds: Array[Long])

case class ProductRecommendations(customers: Array[Customer], products: Array[Product], recommendations: Array[UserProductRecommendations])
