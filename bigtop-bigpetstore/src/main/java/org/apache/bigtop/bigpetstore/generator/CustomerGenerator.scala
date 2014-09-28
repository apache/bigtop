/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.bigpetstore.generator

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.bigtop.bigpetstore.generator.util.State
import org.apache.hadoop.fs.Path
import parquet.org.codehaus.jackson.format.DataFormatDetector
import org.slf4j.LoggerFactory
import java.util.{Collection => JavaCollection}
import scala.collection.JavaConversions.asJavaCollection
import java.util.Random
import scala.collection.mutable.{HashMap, Set, MultiMap}
import scala.collection.immutable.NumericRange

/**
 * This class generates random customer data. The generated customer
 * ids will be consecutive. The client code that generates the transactions
 * records needs to know the available customer ids. If we keep the customer
 * ids consecutive here. we don't have to store those ids in memory, or perform
 * costly lookups. Once we introduce something that allows efficient lookup
 * of data, we can do something else as well.
 *
 * The generated customer ids will start from 1. So, if we have 100 customers,
 * the ids will be [1, 100].
 */
class CustomerGenerator(val desiredCustomerCount: Int, val outputPath: Path) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val random = new Random;
  private val assertion = "The generateCustomerRecords() hasn't been called yet";
  private var customerFileGenerated = false
  private val _stateToCustomerIds = new HashMap[State, NumericRange[Long]]

  def isCustomerFileGenrated = customerFileGenerated

  def customerIds(state: State) = {
    assert(customerFileGenerated, assertion)
    _stateToCustomerIds(state)
  }

  def generateCustomerRecords() = {
    val config = new Configuration
    val fs = FileSystem.getLocal(config)

    assert(!fs.exists(outputPath))

    val outputStream = fs.create(outputPath)

    var currentId: Long = 1
    logger.info("Generating customer records at: {}", fs.pathToFile(outputPath))
    for (state <- State.values();
            stateCustomerCount = (state.probability * desiredCustomerCount) toLong;
            random = new Random(state.hashCode);
            i <- 1L to stateCustomerCount) {
      val customerRecord = CustomerGenerator.createRecord(currentId, state, random);
      logger.info("generated customer: {}", customerRecord)
      outputStream.writeBytes(customerRecord)

      if(i == 1) {
        val stateCustomerIdRange = currentId until (currentId + stateCustomerCount);
        _stateToCustomerIds += (state -> stateCustomerIdRange)
      }
      currentId += 1
    }

    println(_stateToCustomerIds)
    outputStream.flush
    outputStream.close
    customerFileGenerated = true
  }
}

object CustomerGenerator {
  val OUTPUT_FILE_NAME = "customers"

  private def createRecord(id: Long, state: State, r: Random) = {
    val firstName = DataForger.firstName
    val lastName = DataForger.lastName
    s"$id\t${DataForger.firstName(r)}\t${DataForger.lastName(r)}\t${state.name}\n"
  }
}