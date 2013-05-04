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

package org.apache.bigtop.itest.datafu.util;

import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class IntBoolConversionPigTests extends PigTests
{
  @Test
  public void intToBoolTest() throws Exception
  {
    PigTest test = createPigTest("datafu/util/intToBoolTest.pig");
        
    String[] input = {
      "", // null
      "0",
      "1"
    };
    
    String[] output = {
        "(false)",
        "(false)",
        "(true)"
      };
    
    test.assertOutput("data",input,"data2",output);
  }
  
  @Test
  public void intToBoolToIntTest() throws Exception
  {
    PigTest test = createPigTest("datafu/util/intToBoolToIntTest.pig");
        
    String[] input = {
      "", // null
      "0",
      "1",
      "2",
      "-1",
      "-2",
      "0",
      ""
    };
    
    String[] output = {
        "(0)",
        "(0)",
        "(1)",
        "(1)",
        "(1)",
        "(1)",
        "(0)",
        "(0)"
      };
    
    test.assertOutput("data",input,"data3",output);
  }
}
