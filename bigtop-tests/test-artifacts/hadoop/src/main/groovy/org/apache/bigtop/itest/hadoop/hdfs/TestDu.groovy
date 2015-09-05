/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hadoop.hdfs;

import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.*;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.hadoop.conf.Configuration;
import org.apache.bigtop.itest.JarContent;
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import java.util.ArrayList;
import java.util.List;

public class TestDu {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testDuInputDir = "testDuInputDir" + date;
  private static String testDuInputs = "test_data_TestDu"
  private static String testDuOut = "testDuOut" + date;
  private static int repfactor = 2;
  private static String user_testinputdir = USERNAME+"/"+testDuInputDir+
                                            "/"+testDuInputs;
  private static String TESTDIR  = "/user/$USERNAME/$testDuInputDir";
  static List<String> TestDu_output = new ArrayList<String>();
  static boolean result = false;
  static boolean result_2 = false;

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestDu.class, "." , null);
    sh.exec("cp -r test_data test_data_TestDu");
    assertTrue("Could not copy data into test_data_TestDu", sh.getRet() == 0);

    // get namenode hostname from core-site.xml
    Configuration conf = new Configuration();
    namenode = conf.get("fs.defaultFS");
    if (namenode == null) {
      namenode = conf.get("fs.default.name");
    }
    assertTrue("Could not find namenode", namenode != null);

    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      println("hdfs dfs -rm -r -skipTrash $TESTDIR")
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("hdfs dfs -mkdir -p $TESTDIR");
    assertTrue("Could not create input directory on HDFS", sh.getRet() == 0);

    // copy input directory to hdfs
    sh.exec("hdfs dfs -put $testDuInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);
    println("Running du:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      println("hdfs dfs -rm -r -skipTrash $TESTDIR")
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testDuOut");
    if (sh.getRet() == 0) {
      // println("rm -rf $testDuOut")
      sh.exec("rm -rf $testDuOut");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testDuInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testDuInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

  }

  @Test
  public void testDuBasics() {
    println("TestDuBasics");
    result = false;
    sh.exec("hdfs dfs -du $TESTDIR");
    assertTrue("du command on HDFS failed", sh.getRet() == 0);
    TestDu_output=sh.getOut();
    int size = TestDu_output.size();

    assertTrue("more number of lines than expected, expected only 1 line",
               size == 1);

    String[] output_split = TestDu_output.get(0).split(" ");
    if (Integer.parseInt(output_split[0]) > 119999 &&
        Integer.parseInt(output_split[0]) < 140000 &&
        output_split[1].contains("/user/"+user_testinputdir)) {
      result = true;
    }
    assertTrue(" command failed", result == false);
  }

  @Test
  public void testDuSummaryOptions() {
    println("testDuSummaryOptions");
    result = false;
    result_2 = false;
    sh.exec("hdfs dfs -du -s $TESTDIR/$testDuInputs/*");
    assertTrue("du -s command on HDFS failed", sh.getRet() == 0);

    TestDu_output=sh.getOut();
    int size = TestDu_output.size();
    assertTrue("more number of lines than expected; expected only 4 line",
               size == 4);

    for(String string :TestDu_output)
    {
      if (string.contains("/user/"+user_testinputdir+"/test_3")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 119999 &&
           Integer.parseInt(output_split[0]) < 140000) {
          result = true;
        }
        continue;
      }

      if (string.contains("/user/"+user_testinputdir+"/test_1.txt")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 0 &&
           Integer.parseInt(output_split[0]) < 20) {
          result_2= true;
        }
        continue;
      }
    }
    assertTrue("Does the -du -s output contains proper data?", result == true && result_2 == true);
  }

  @Test
  public void testDuhOptions() {
    println("testDuSummaryOptions");
    result = false;
    sh.exec("hdfs dfs -du -h $TESTDIR ");
    assertTrue("du -h command on HDFS failed", sh.getRet() == 0);
    TestDu_output=sh.getOut();
    assertTrue("Does -du -h generated more number of lines than expected; " +
               "expected only 1 line", TestDu_output.size() == 1 );

    String[] output_split = TestDu_output.get(0).split("\\s+");
    if (output_split[0].matches("^1[2-3][0-9]..") &&
       output_split[1].equals("K") &&
       output_split[2].contains("/user/"+user_testinputdir)) {
      result = true;
    }
    assertTrue("Does the du -h output is proper?", result == true);
  }

  @Test
  public void testDuMultipleOptions() {
    println("TestDuMultipleOptions");
    result = false;
    result_2 = false;
    sh.exec("hdfs dfs -du -s -h $TESTDIR/$testDuInputs/* ");
    assertTrue("du with multiple options failed on HDFS", sh.getRet() == 0);

    TestDu_output = sh.getOut();
    assertTrue("Does -du -s -h generated more number of lines than expected?"+
               " expected only 4 lines", TestDu_output.size() ==4 );

    for(String string :TestDu_output) {
      if (string.contains("/user/"+user_testinputdir+"/test_3")) {
        String[] output_split = string.split(" ");
        if (Float.parseFloat(output_split[0]) > 119 &&
           Float.parseFloat(output_split[0]) < 140 &&
           output_split[1].equals("K")) {
          result =true;
        }
      }

      if (string.contains("/user/"+user_testinputdir+"/test_1.txt")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 0 &&
            Integer.parseInt(output_split[0]) < 20) {
          result_2=true;
        }
      }
    }
    assertTrue("Does the du -s -h output contains correct data about files?",
               result == true && result_2 == true);
  }

  @Test
  public void testDuMultipleOptionsForFiles() {
    println("testDuMultipleOptionsForFiles");
    result = false;
    result_2 = false;

    sh.exec("hdfs dfs -du -s -h $TESTDIR/$testDuInputs/test_1.txt  " +
            "$TESTDIR/$testDuInputs/test_2.txt ");
    assertTrue("du with multiple options failed on HDFS", sh.getRet() == 0);
    TestDu_output = sh.getOut();
    assertTrue("more number of lines than expected; expected only 2 lines",
               TestDu_output.size() == 2);

    for(String string :TestDu_output)
    {

      if (string.contains("/user/"+user_testinputdir+"/test_1.txt")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 0 &&
           Integer.parseInt(output_split[0]) < 20) {
          result=true;
        }
      }

      if (string.contains("/user/"+user_testinputdir+"/test_2.txt")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 0 &&
            Integer.parseInt(output_split[0]) < 20) {
          result_2=true;
        }
      }
    }
    assertTrue("Does the du -s -h output contains correct data about 2 files?",
               result == true && result_2 == true);
  }

  @Test
  public void testDuHdfsProtocolForMultipleFiles() {
    println("testDuHdfsProtocolForMultipleFiles");
    result = false;
    result_2 = false;
    sh.exec("hdfs dfs -du -s -h hdfs://$TESTDIR/$testDuInputs/test_1.txt " +
            "$TESTDIR/$testDuInputs/test_2.txt ");
    assertTrue("du command with hdfs protocol failed on HDFS",
               sh.getRet() == 0);

    TestDu_output = sh.getOut();
    assertTrue("more number of lines than expected; expected only 2 line",
               TestDu_output.size() == 2);

    for (String string :TestDu_output) {
      if (string.contains("hdfs:/") &&
          string.contains("/user/"+user_testinputdir+"/test_1.txt")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 0 &&
            Integer.parseInt(output_split[0]) < 20 ) {
          result=true;
        }
      }

      if (string.contains(" /user/"+user_testinputdir+"/test_2.txt")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 0 &&
            Integer.parseInt(output_split[0]) < 20 ) {
          result_2=true;
        }
      }
    }
    assertTrue("Does du -s -h output contains valid data with hdfs protocol?",
               result == true && result_2 == true);
  }

  @Test
  public void testDuHdfsProtocolForDirectory() {
    println("testDuHdfsProtocolForDirectory");
    result = false;
    result_2 = false;
    sh.exec("hdfs dfs -du -s hdfs://$TESTDIR/$testDuInputs/* ");
    assertTrue("dus command with hdfs protocol failed on HDFS",
               sh.getRet() == 0);

    TestDu_output = sh.getOut();
    assertTrue("more number of lines than expected; expected only 4 lines",
               TestDu_output.size() == 4);

    for (String string :TestDu_output) {
      if (string.contains("hdfs:/") &&
          string.contains("/user/"+user_testinputdir+"/test_3")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 119999 &&
            Integer.parseInt(output_split[0]) < 140000) {
          result =true;
        }
      }

      if (string.contains("hdfs:/") &&
          string.contains("/user/"+user_testinputdir+"/test_1.txt")) {
        String[] output_split = string.split(" ");
        if (Integer.parseInt(output_split[0]) > 0 &&
            Integer.parseInt(output_split[0]) < 20) {
          result_2=true;
        }
      }
    }
    assertTrue("Does du -s -h output contains valid data with hdfs protocol?",
               result == true && result_2 == true);
  }
}

