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
