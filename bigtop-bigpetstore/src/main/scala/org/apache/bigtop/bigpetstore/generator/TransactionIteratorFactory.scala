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
package org.apache.bigtop.bigpetstore.generator;

import java.util.Date
import org.apache.bigtop.bigpetstore.generator.util.State
import org.apache.commons.lang3.StringUtils
import java.util.Arrays.asList
import java.util.Random
import scala.collection.Iterator
import com.sun.org.apache.xml.internal.serializer.ToStream
import java.util.{Iterator => JavaIterator}
import scala.collection.JavaConversions.asJavaIterator
import org.apache.bigtop.bigpetstore.generator.util.Product
import org.apache.commons.lang3.Range;
import org.apache.bigtop.bigpetstore.generator.util.ProductType

/**
 * This class generates our data. Over time we will use it to embed bias which
 * can then be teased out, i.e. by clustering/classifiers. For example:
 *
 * certain products <--> certain years or days
 */
class TransactionIteratorFactory(private val records: Int,
        private val customerIdRange: Range[java.lang.Long],
        private val state: State) {
  assert(records > 0, "Number of records must be greater than 0 to generate a data iterator!")
  private val random = new Random(state.hashCode)

  def data: JavaIterator[TransactionIteratorFactory.KeyVal[String, String]] = {
    new TransactionIteratorFactory.DataIterator(records, customerIdRange, state, random)
  }
}

object TransactionIteratorFactory {
  class KeyVal[K, V](val key: K, val value: V)

  private class DataIterator(records: Int,
          customerIdRange: Range[java.lang.Long],
          state: State,
          r: Random) extends Iterator[KeyVal[String, String]] {
    private var firstName: String = null
    private var lastName: String = null
    private var elementsProcducedCount = 0
    private var repeatCount = 0
    private var currentCustomerId = customerIdRange.getMinimum
    private var currentProductType = selectRandomProductType;

    def hasNext =
      elementsProcducedCount < records && currentCustomerId <= customerIdRange.getMaximum


    def next(): TransactionIteratorFactory.KeyVal[String,String] = {
      val date = DataForger.randomDateInPastYears(50);
      setIteratorState();

      val product = randomProductOfCurrentlySelectedType
      val key = StringUtils.join(asList("BigPetStore", "storeCode_" + state.name(),
              elementsProcducedCount.toString), ",")
      val value = StringUtils.join(asList(currentCustomerId, firstName, lastName, product.id,
              product.name.toLowerCase, product.price, date), ",")

      elementsProcducedCount += 1
      new TransactionIteratorFactory.KeyVal(key, value)
    }

    private def setIteratorState() = {
      /** Some customers come back for more :) We repeat a customer up to ten times */
      if (repeatCount > 0) {
        repeatCount -= 1
      } else {
        firstName = DataForger.firstName(r)
        lastName = DataForger.lastName(r)
        // this sometimes generates numbers much larger than 10. We don't really need Gaussian
        // distribution since number of transactions per customer can be truly arbitrary.
        repeatCount = (r.nextGaussian * 4f) toInt;
        println("####Repeat: " + repeatCount)
        currentCustomerId += 1
        currentProductType = selectRandomProductType;
      }
    }

    private def selectRandomProductType = {
      ProductType.values.apply(r.nextInt(ProductType.values.length))
    }

    private def randomProductOfCurrentlySelectedType = {
      currentProductType.getProducts.get(r.nextInt(currentProductType.getProducts.size))
    }
  }
}