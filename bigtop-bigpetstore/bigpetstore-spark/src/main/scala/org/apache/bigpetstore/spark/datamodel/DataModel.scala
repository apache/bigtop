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

import java.util.Calendar

case class Customer(customerId: Long, firstName: String,
  lastName: String, zipcode: String)

case class Location(zipcode: String, city: String, state: String)

case class Product(productId: Long, category: String, attributes: Map[String, String])

case class Store(storeId: Long, zipcode: String)

case class Transaction(customerId: Long, transactionId: Long, storeId: Long, dateTime: Calendar, productId: Long)

/**
 * Statistics phase.  To be expanded...
 * */
case class Statistics(transactions:Long)
