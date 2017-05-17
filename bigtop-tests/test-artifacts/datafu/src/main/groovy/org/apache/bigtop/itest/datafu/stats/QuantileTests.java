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

package org.apache.bigtop.itest.datafu.stats;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class QuantileTests  extends PigTests
{
  @Test
  public void quantileTest() throws Exception
  {
    PigTest test = createPigTest("datafu/stats/quantileTest.pig",
                                 "QUANTILES='0.0','0.25','0.5','0.75','1.0'");

    String[] input = {"1","2","3","4","10","5","6","7","8","9"};
    writeLinesToFile("input", input);
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(1.0,3.0,5.5,8.0,10.0)", output.get(0).toString());
  }
  
  @Test
  public void quantile2Test() throws Exception
  {
    PigTest test = createPigTest("datafu/stats/quantileTest.pig",
                                 "QUANTILES='5'");

    String[] input = {"1","2","3","4","10","5","6","7","8","9"};
    writeLinesToFile("input", input);
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(1.0,3.0,5.5,8.0,10.0)", output.get(0).toString());
  }
  
  @Test
  public void medianTest() throws Exception
  {
    PigTest test = createPigTest("datafu/stats/medianTest.pig");

    String[] input = {"4","5","6","9","10","7","8","2","3","1"};
    writeLinesToFile("input", input);
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(5.5)", output.get(0).toString());
  }
  
  @Test
  public void streamingMedianTest() throws Exception
  {
    PigTest test = createPigTest("datafu/stats/streamingMedianTest.pig");

    String[] input = {"0","4","5","6","9","10","7","8","2","3","1"};
    writeLinesToFile("input", input);
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(5.0)", output.get(0).toString());
  }

  @Test
  public void streamingQuantileTest() throws Exception {
    PigTest test = createPigTest("datafu/stats/streamingQuantileTest.pig",
                                 "QUANTILES='5'");

    String[] input = {"1","2","3","4","10","5","6","7","8","9"};
    writeLinesToFile("input", input);
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(1.0,3.0,5.0,8.0,10.0)", output.get(0).toString());
  }
  
  @Test
  public void streamingQuantile2Test() throws Exception {
    PigTest test = createPigTest("datafu/stats/streamingQuantileTest.pig",
                                 "QUANTILES='0.5','0.75','1.0'");

    String[] input = {"1","2","3","4","10","5","6","7","8","9"};
    writeLinesToFile("input", input);
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(5.0,8.0,10.0)", output.get(0).toString());
  }
  
  @Test
  public void streamingQuantile3Test() throws Exception {
    PigTest test = createPigTest("datafu/stats/streamingQuantileTest.pig",
                                 "QUANTILES='0.07','0.03','0.37','1.0','0.0'");

    List<String> input = new ArrayList<String>();
    for (int i=1000; i>=1; i--)
    {
      input.add(Integer.toString(i));
    }
    
    writeLinesToFile("input", input.toArray(new String[0]));
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(70.0,30.0,370.0,1000.0,1.0)", output.get(0).toString());
  }
  
  @Test
  public void streamingQuantile4Test() throws Exception {
    PigTest test = createPigTest("datafu/stats/streamingQuantileTest.pig",
                                 "QUANTILES='0.0013','0.0228','0.1587','0.5','0.8413','0.9772','0.9987'");

    List<String> input = new ArrayList<String>();
    for (int i=100000; i>=0; i--)
    {
      input.add(Integer.toString(i));
    }
    
    writeLinesToFile("input", input.toArray(new String[0]));
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(130.0,2280.0,15870.0,50000.0,84130.0,97720.0,99870.0)", output.get(0).toString());
  }
  

  
  @Test
  public void quantile3Test() throws Exception {
    PigTest test = createPigTest("datafu/stats/quantileTest.pig",
                                 "QUANTILES='0.0013','0.0228','0.1587','0.5','0.8413','0.9772','0.9987'");

    List<String> input = new ArrayList<String>();
    for (int i=100000; i>=0; i--)
    {
      input.add(Integer.toString(i));
    }
    
    writeLinesToFile("input", input.toArray(new String[0]));
        
    test.runScript();
    
    List<Tuple> output = getLinesForAlias(test, "data_out", true);
    
    assertEquals(1,output.size());
    assertEquals("(130.0,2280.0,15870.0,50000.0,84130.0,97720.0,99870.0)", output.get(0).toString());
  }
}
