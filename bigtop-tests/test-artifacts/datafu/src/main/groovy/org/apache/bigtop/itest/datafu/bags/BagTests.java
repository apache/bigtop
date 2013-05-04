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

package org.apache.bigtop.itest.datafu.bags;

import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;


public class BagTests extends PigTests
{
  @Test
  public void nullToEmptyBagTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/nullToEmptyBagTest.pig");
            
    writeLinesToFile("input", 
                     "({(1),(2),(3),(4),(5)})",
                     "()",
                     "{(4),(5)})");
            
    test.runScript();
        
    assertOutput(test, "data2",
                 "({(1),(2),(3),(4),(5)})",
                 "({})",
                 "({(4),(5)})");
  }
  
  @Test
  public void appendToBagTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/appendToBagTest.pig");
    
    writeLinesToFile("input", 
                     "1\t{(1),(2),(3)}\t(4)",
                     "2\t{(10),(20),(30),(40),(50)}\t(60)");
                  
    test.runScript();
            
    assertOutput(test, "data2",
                 "(1,{(1),(2),(3),(4)})",
                 "(2,{(10),(20),(30),(40),(50),(60)})");
  }

   @Test
  public void firstTupleFromBagTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/firstTupleFromBagTest.pig");

    writeLinesToFile("input", "1\t{(4),(9),(16)}");

    test.runScript();

    assertOutput(test, "data2", "(1,(4))");
  }

  
  @Test
  public void prependToBagTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/prependToBagTest.pig");
    
    writeLinesToFile("input", 
                     "1\t{(1),(2),(3)}\t(4)",
                     "2\t{(10),(20),(30),(40),(50)}\t(60)");
                  
    test.runScript();
            
    assertOutput(test, "data2",
                 "(1,{(4),(1),(2),(3)})",
                 "(2,{(60),(10),(20),(30),(40),(50)})");
  }
  
  @Test
  public void bagConcatTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/bagConcatTest.pig");

    writeLinesToFile("input", 
                     "({(1),(2),(3)}\t{(3),(5),(6)}\t{(10),(13)})",
                     "({(2),(3),(4)}\t{(5),(5)}\t{(20)})");
                  
    test.runScript();
            
    assertOutput(test, "data2",
                 "({(1),(2),(3),(3),(5),(6),(10),(13)})",
                 "({(2),(3),(4),(5),(5),(20)})");
  }
  
  @Test
  public void unorderedPairsTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/unorderedPairsTests.pig");
    
    String[] input = {
      "{(1),(2),(3),(4),(5)}"
    };
    
    String[] output = {
        "(1,2)",
        "(1,3)",
        "(1,4)",
        "(1,5)",
        "(2,3)",
        "(2,4)",
        "(2,5)",
        "(3,4)",
        "(3,5)",
        "(4,5)"
      };
    
    test.assertOutput("data",input,"data4",output);
  }
  
  @Test
  public void unorderedPairsTest2() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/unorderedPairsTests2.pig");
        
    this.writeLinesToFile("input", "1\t{(1),(2),(3),(4),(5)}");
    
    String[] output = {
        "(1,2)",
        "(1,3)",
        "(1,4)",
        "(1,5)",
        "(2,3)",
        "(2,4)",
        "(2,5)",
        "(3,4)",
        "(3,5)",
        "(4,5)"
      };
    
    test.runScript();
    this.getLinesForAlias(test, "data3");
    
    this.assertOutput(test, "data3",
                      "(1,(1),(2))",
                      "(1,(1),(3))",
                      "(1,(1),(4))",
                      "(1,(1),(5))",
                      "(1,(2),(3))",
                      "(1,(2),(4))",
                      "(1,(2),(5))",
                      "(1,(3),(4))",
                      "(1,(3),(5))",
                      "(1,(4),(5))");    
  }
 
  @Test
  public void bagSplitTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/bagSplitTest.pig",
                                 "MAX=5");
    
    writeLinesToFile("input", 
                     "{(1,11),(2,22),(3,33),(4,44),(5,55),(6,66),(7,77),(8,88),(9,99),(10,1010),(11,1111),(12,1212)}");
    
    test.runScript();
    
    assertOutput(test, "data3",
                 "({(1,11),(2,22),(3,33),(4,44),(5,55)})",
                 "({(6,66),(7,77),(8,88),(9,99),(10,1010)})",
                 "({(11,1111),(12,1212)})");
  }
  
  @Test
  public void bagSplitWithBagNumTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/bagSplitWithBagNumTest.pig",
                                 "MAX=10");
    
    writeLinesToFile("input", 
                     "{(1,11),(2,22),(3,33),(4,44),(5,55),(6,66),(7,77),(8,88),(9,99),(10,1010),(11,1111),(12,1212)}");
    
    test.runScript();
    
    assertOutput(test, "data3",
                 "({(1,11),(2,22),(3,33),(4,44),(5,55),(6,66),(7,77),(8,88),(9,99),(10,1010)},0)",
                 "({(11,1111),(12,1212)},1)");
  }
  
  @Test
  public void enumerateWithReverseTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/enumerateWithReverseTest.pig");
       
    writeLinesToFile("input", 
                     "({(10,{(1),(2),(3)}),(20,{(4),(5),(6)}),(30,{(7),(8)}),(40,{(9),(10),(11)}),(50,{(12),(13),(14),(15)})})");
    
    test.runScript();
    
    assertOutput(test, "data4",
                 "(10,{(1),(2),(3)},5)",
                 "(20,{(4),(5),(6)},4)",
                 "(30,{(7),(8)},3)",
                 "(40,{(9),(10),(11)},2)",
                 "(50,{(12),(13),(14),(15)},1)");
  }
  
  @Test
  public void enumerateWithStartTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/enumerateWithStartTest.pig");
       
    writeLinesToFile("input", 
                     "({(10,{(1),(2),(3)}),(20,{(4),(5),(6)}),(30,{(7),(8)}),(40,{(9),(10),(11)}),(50,{(12),(13),(14),(15)})})");
    
    test.runScript();
    
    assertOutput(test, "data4",
                 "(10,{(1),(2),(3)},1)",
                 "(20,{(4),(5),(6)},2)",
                 "(30,{(7),(8)},3)",
                 "(40,{(9),(10),(11)},4)",
                 "(50,{(12),(13),(14),(15)},5)");
  }
  
  @Test
  public void enumerateTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/enumerateTest.pig");
       
    writeLinesToFile("input",
                     "({(10,{(1),(2),(3)}),(20,{(4),(5),(6)}),(30,{(7),(8)}),(40,{(9),(10),(11)}),(50,{(12),(13),(14),(15)})})");
    
    test.runScript();
    
    assertOutput(test, "data4",
                 "(10,{(1),(2),(3)},0)",
                 "(20,{(4),(5),(6)},1)",
                 "(30,{(7),(8)},2)",
                 "(40,{(9),(10),(11)},3)",
                 "(50,{(12),(13),(14),(15)},4)");
  }
  
  @Test
  public void comprehensiveBagSplitAndEnumerate() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/comprehensiveBagSplitAndEnumerate.pig");
    
    writeLinesToFile("input",
                     "({(A,1.0),(B,2.0),(C,3.0),(D,4.0),(E,5.0)})");
    
    test.runScript();
    
    assertOutput(test, "data_out",
                 // bag #1
                 "(A,1.0,1)",
                 "(B,2.0,1)",
                 "(C,3.0,1)",
                 // bag #2
                 "(D,4.0,2)",
                 "(E,5.0,2)");
  }
  
  @Test
  public void aliasBagFieldsTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/aliasBagFieldsTest.pig");
    
    writeLinesToFile("input",
                     "({(A,1,0),(B,2,0),(C,3,0),(D,4,0),(E,5,0)})");
    
    test.runScript();
    
    assertOutput(test, "data4",
                 "(A,1)",
                 "(B,2)",
                 "(C,3)",
                 "(D,4)",
                 "(E,5)");
  }

  @Test
  public void distinctByTest() throws Exception
  {
    PigTest test = createPigTest("datafu/bags/distinctByTest.pig");
    
    writeLinesToFile("input",
                     "({(Z,1,0),(A,1,0),(A,1,0),(B,2,0),(B,22,1),(C,3,0),(D,4,0),(E,5,0)})");
    
    test.runScript();
    
    assertOutput(test, "data2",
                 "({(Z,1,0),(A,1,0),(B,2,0),(C,3,0),(D,4,0),(E,5,0)})");
  }

}
