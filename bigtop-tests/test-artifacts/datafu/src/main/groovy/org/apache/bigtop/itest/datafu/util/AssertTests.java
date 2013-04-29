package org.apache.bigtop.itest.datafu.util;

import static org.junit.Assert.*;

import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class AssertTests extends PigTests
{
  @Test
  public void shouldAssertWithMessageOnZero() throws Exception
  {
    try
    {
      PigTest test = createPigTest("datafu/util/assertWithMessageTest.pig");
      
      this.writeLinesToFile("input", "0");
      
      test.runScript();
      
      this.getLinesForAlias(test, "data2");
      
      fail("test should have failed, but it didn't");
    }
    catch (Exception e)
    {
    }
  }
  
  @Test
  public void shouldNotAssertWithMessageOnOne() throws Exception
  {
    PigTest test = createPigTest("datafu/util/assertWithMessageTest.pig");
    
    this.writeLinesToFile("input", "1");
    
    test.runScript();
    
    this.getLinesForAlias(test, "data2");
  }
  
  @Test
  public void shouldAssertWithoutMessageOnZero() throws Exception
  {
    try
    {
      PigTest test = createPigTest("datafu/util/assertWithoutMessageTest.pig");
      
      this.writeLinesToFile("input", "0");
      
      test.runScript();
      
      this.getLinesForAlias(test, "data2");
      
      fail("test should have failed, but it didn't");
    }
    catch (Exception e)
    {
    }
  }
  
  @Test
  public void shouldNotAssertWithoutMessageOnOne() throws Exception
  {
    PigTest test = createPigTest("datafu/util/assertWithoutMessageTest.pig");
    
    this.writeLinesToFile("input", "1");
    
    test.runScript();
    
    this.getLinesForAlias(test, "data2");
  }
}
