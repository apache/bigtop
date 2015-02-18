package org.apache.bigpetstore.spark.analytics

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

import com.google.common.collect.ImmutableMap
import org.apache.bigtop.bigpetstore.spark.analytics.PetStoreStatistics
import org.apache.bigtop.bigpetstore.spark.datamodel.Product
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.junit.JUnitRunner

import Array._

import java.io.File
import java.nio.file.Files
import java.util.Calendar
import java.util.Locale


// hack for running tests with Gradle
@RunWith(classOf[JUnitRunner])
class AnalyticsSuite extends FunSuite with BeforeAndAfterAll {

  test("product mapper") {
    val p = Product(1L, "cat1", Map(("a","a1"), ("b","b1")))
    assert(PetStoreStatistics.productMap(Array(p)).get(1L).get === p)
  }
}
