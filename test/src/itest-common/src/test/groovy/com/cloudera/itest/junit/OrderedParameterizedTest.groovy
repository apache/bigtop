/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.itest.junit

import org.junit.runner.RunWith
import org.junit.Test
import com.cloudera.itest.junit.OrderedParameterized.RunStage
import org.junit.runners.Parameterized.Parameters
import org.junit.AfterClass
import static org.junit.Assert.assertEquals

@RunWith(OrderedParameterized)
class OrderedParameterizedTest {
  int parameter;
  static List order = [];

  @RunStage(level=1)
  @Test
  public void lateTest() {
    order.add(1);
  }

  @RunStage(level=-1)
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
