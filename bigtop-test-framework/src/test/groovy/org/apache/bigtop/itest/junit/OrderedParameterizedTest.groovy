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

package org.apache.bigtop.itest.junit

import org.junit.runner.RunWith
import org.junit.Test
import org.apache.bigtop.itest.junit.OrderedParameterized.RunStage
import org.junit.runners.Parameterized.Parameters
import org.junit.AfterClass
import static org.junit.Assert.assertEquals

@RunWith(OrderedParameterized)
class OrderedParameterizedTest {
  int parameter;
  static List order = [];

  @RunStage(level = 1)
  @Test
  public void lateTest() {
    order.add(1);
  }

  @RunStage(level = -1)
  @Test
  public void earlyTest() {
    order.add(-1);
  }

  @Test
  public void defaultTest() {
    order.add(0);
  }

  OrderedParameterizedTest(int p) {
    parameter = p;
  }

  @Parameters
  public static Map<String, Object[]> generateTests() {
    HashMap<String, Object[]> res = new HashMap();
    res.put("test name", [1] as Object[]);
    return res;
  }

  @AfterClass
  static void verifyOrder() {
    assertEquals("tests were NOT executed in the desired order",
      [-1, 0, 1], order);
  }
}
