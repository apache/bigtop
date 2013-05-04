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

import java.util.Iterator;

import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class MarkovPairTests extends PigTests
{
  @Test
  public void markovPairDefaultTest() throws Exception
  {
    PigTest test = createPigTest("datafu/stats/markovPairDefault.pig",
                                 "schema=(data: bag {t: tuple(val:int)})");
    
    writeLinesToFile("input", "{(10),(20),(30),(40),(50),(60)}");
    
    String[] expectedOutput = {
        "({((10),(20)),((20),(30)),((30),(40)),((40),(50)),((50),(60))})"
      };
    
    test.runScript();
    
    Iterator<Tuple> actualOutput = test.getAlias("data_out");
    
    assertTuplesMatch(expectedOutput, actualOutput);
  }
  
  @Test
  public void markovPairMultipleInput() throws Exception
  {    
    PigTest test = createPigTest("datafu/stats/markovPairDefault.pig",
                                 "schema=(data: bag {t: tuple(val1:int,val2:int)})");
    
    writeLinesToFile("input", "{(10,100),(20,200),(30,300),(40,400),(50,500),(60,600)}");
    
    String[] expectedOutput = {
        "({((10,100),(20,200)),((20,200),(30,300)),((30,300),(40,400)),((40,400),(50,500)),((50,500),(60,600))})"
      };    
    
    
    test.runScript();
    
    Iterator<Tuple> actualOutput = test.getAlias("data_out");
    
    assertTuplesMatch(expectedOutput, actualOutput);
  }
  
  @Test
  public void markovPairLookaheadTest() throws Exception
  {
    PigTest test = createPigTest("datafu/stats/markovPairLookahead.pig", 
                                 "schema=(data: bag {t: tuple(val:int)})",
                                 "lookahead=3");
    
    writeLinesToFile("input", "{(10),(20),(30),(40),(50)}");
    
    String[] expectedOutput = {
        "({((10),(20)),((10),(30)),((10),(40)),((20),(30)),((20),(40)),((20),(50)),((30),(40)),((30),(50)),((40),(50))})"
      };
    
    test.runScript();
    
    Iterator<Tuple> actualOutput = test.getAlias("data_out");
    
    assertTuplesMatch(expectedOutput, actualOutput);
  }
  
  private void assertTuplesMatch(String[] expectedOutput, Iterator<Tuple> actualOutput)
  {
    Iterator<Tuple> tuples = actualOutput;
    
    for (String outputLine : expectedOutput)
    {
      assertTrue(tuples.hasNext());
      Tuple outputTuple = tuples.next();
      System.out.println(String.format("expected: %s", outputLine));
      System.out.println(String.format("actual: %s", outputTuple.toString()));
      assertEquals(outputLine,outputTuple.toString());
    }
  }
}
