package org.apache.bigtop.itest.datafu.date;

import org.apache.pig.pigunit.PigTest;
import org.junit.Test;

import org.apache.bigtop.itest.datafu.PigTests;

public class TimeTests extends PigTests
{  
  @Test
  public void timeCountPageViewsTest() throws Exception
  {
    PigTest test = createPigTest("datafu/date/timeCountPageViewsTest.pig",
                                 "TIME_WINDOW=30m",
                                 "JAR_PATH=" + getJarPath());
        
    String[] input = {
      "1\t100\t2010-01-01T01:00:00Z",
      "1\t100\t2010-01-01T01:15:00Z",
      "1\t100\t2010-01-01T01:31:00Z",
      "1\t100\t2010-01-01T01:35:00Z",
      "1\t100\t2010-01-01T02:30:00Z",

      "1\t101\t2010-01-01T01:00:00Z",
      "1\t101\t2010-01-01T01:31:00Z",
      "1\t101\t2010-01-01T02:10:00Z",
      "1\t101\t2010-01-01T02:40:30Z",
      "1\t101\t2010-01-01T03:30:00Z",      

      "1\t102\t2010-01-01T01:00:00Z",
      "1\t102\t2010-01-01T01:01:00Z",
      "1\t102\t2010-01-01T01:02:00Z",
      "1\t102\t2010-01-01T01:10:00Z",
      "1\t102\t2010-01-01T01:15:00Z",
      "1\t102\t2010-01-01T01:25:00Z",
      "1\t102\t2010-01-01T01:30:00Z"
    };
    
    String[] output = {
        "(1,100,2)",
        "(1,101,5)",
        "(1,102,1)"
      };
    
    test.assertOutput("views",input,"view_counts",output);
  }
}
