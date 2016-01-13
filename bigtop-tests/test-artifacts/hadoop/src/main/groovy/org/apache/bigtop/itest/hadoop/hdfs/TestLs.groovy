/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

public class TestLs {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testLsInputDir = "testLsInputDir" + date;
  private static String testLsInputs = "test_data_TestLs"
  private static String user_testinputdir  = USERNAME+"/"+testLsInputDir+"/"+
                                             testLsInputs;
  static List<String> TestLs_output = new ArrayList<String>();
  static boolean result = false;

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestLs.class, "." , null);
    // get namenode hostname from core-site.xml
    Configuration conf = new Configuration();
    namenode = conf.get("fs.defaultFS");
    if (namenode == null) {
      namenode = conf.get("fs.default.name");
    }
    assertTrue("Could not find namenode", namenode != null);

    sh.exec("cp -r test_data test_data_TestLs");
    logError(sh);
    assertTrue("Could not copy data into test_data_TestLs", sh.getRet() == 0);

    sh.exec("hadoop fs -test -d /user/$USERNAME/$testLsInputDir");
    if (sh.getRet() == 0) {
      println("hadoop fs -rm -r -skipTrash /user/$USERNAME/$testLsInputDir")
      sh.exec("hadoop fs -rm -r -skipTrash /user/$USERNAME/$testLsInputDir");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("hadoop fs -mkdir $testLsInputDir");
    assertTrue("Could not create input directory", sh.getRet() == 0);

    // copy input directory to hdfs
    sh.exec("hadoop fs -put $testLsInputs /user/$USERNAME/$testLsInputDir");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    // set the replication if file exists
    sh.exec("hdfs dfs -test -f /user/$USERNAME/$testLsInputDir/" +
            "$testLsInputs/test_2.txt");
    assertTrue("Could not find files on HDFS", sh.getRet() == 0);

    println("Running ls:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hadoop fs -test -d /user/$USERNAME/$testLsInputDir");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rm -r -skipTrash /user/$USERNAME/$testLsInputDir");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -d $testLsInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testLsInputs");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }
  }

  @Test
  public void testLs() {
    println("TestLs");
    // test whether root listing of ls command works
    sh.exec("hdfs dfs -ls / ");
    assertTrue("Able to list /user contents?",
        sh.getOut().grep(~/.*\/user/).size() > 0);

    // test whether a directory exists with the user name under '/user'
    sh.exec("hdfs dfs -ls /user/$USERNAME ");
    assertTrue("ls command on HDFS failed", sh.getRet() == 0);
    assertTrue("Able to list /user contents?",
        sh.getOut().grep(~/.*\/user\/${USERNAME}.*/).size() > 0);
  }

  @Test
  public void testLsWithRegularExpressions() {
    println("testLsWithRegularExpressions");
    //test whether a one can list the files with reqular expression
    sh.exec("hdfs dfs -ls /user/$USERNAME/$testLsInputDir/*");
    assertTrue("ls command on HDFS failed", sh.getRet() == 0);
    assertTrue("Able to list contents with regular expressions?",
        sh.getOut().grep(~/.*\/user\/${user_testinputdir}.*/).size() > 0);
  }

  @Test
  public void testLsVerifyOutputStructureForDirectory() {
    println("TestLsVerifyOutputStructure");
    result = false;
    //verify the structure of the output of ls command for a directory
    sh.exec("hdfs dfs -ls -R /user/$USERNAME/$testLsInputDir");
    assertTrue("ls command on HDFS failed", sh.getRet() == 0);
    TestLs_output=sh.getOut();

    // verify that default permissions are listed in ls output for directory
    assertTrue("Does ls outputs directory permissons properly?",
        TestLs_output.grep(~/.*drwxr-xr-x.*/).size() > 0);

    result = false;
    String searchDir = "/user/"+USERNAME+"/"+testLsInputDir;
    for( String output_String : TestLs_output) {
      String[] string = output_String.split("\\s+");
      if(string[1].equals("-") && string[2].equals(USERNAME) &&
         string[4].equals("0") && output_String.contains(searchDir)) {
        result = true;
        break;
      }
    }
    // verify that no replication factor is set in the output
    assertTrue("Does ls output contains proper data?", result == true);
  }

  @Test
  public void testLsVerifyOutputStructureForFile() {
    println("testLsVerifyOutputStructureForFile");
    result = false;
    // verify the structure of the output of ls command for a file
    sh.exec("hdfs dfs -ls /user/$USERNAME/$testLsInputDir/" +
            "$testLsInputs/test_2.txt");
    assertTrue("ls command on HDFS failed", sh.getRet() == 0);
    String fileName = "/user/"+user_testinputdir+"/test_2.txt";
    TestLs_output=sh.getOut();
    for( String output_String : TestLs_output)
    {
      String[] string = output_String.split("\\s+");
      if(output_String.contains("-rw-r--r--") && string[1].equals("3") &&
         string[2].equals(USERNAME) && output_String.contains(fileName)) {
        result = true;
        break;
      }
    }
    assertTrue("Does the file listing happened properly?", result == true);
    result = false;
  }

  @Test
  public void testLsWithNonExistingDirectory() {
   println("testLsWithNonExistingDirectory");
    // verify that commands fails when wrong arguments are provided
    sh.exec("hdfs dfs -ls -r /user/$USERNAME/$testLsInputDir/$testLsInputs");
    assertTrue("wrong parameter ls -r command successfully executed on HDFS",
               sh.getRet() == 255);
  }

  @Test
  public void testLsWithdOption() {
    println("testLsWithdOption");
    //verify that when '-d' is used parent directory is listed
    String dirName = "/user/$USERNAME/$testLsInputDir/$testLsInputs";
    sh.exec("hdfs dfs -ls -d $dirName");
 
    assertTrue("listing directory failed on HDFS", sh.getRet() == 0);
    TestLs_output = sh.getOut();
    String output_String = TestLs_output.get(0).toString();
    assertTrue("Does output Contains only One Line?",
               TestLs_output.size() == 1);
    assertTrue("Does output Contains only directory Name?",
        TestLs_output.grep(~/.*${dirName}.*/).size() > 0);
  }

  @Test
  public void testLsForRecursiveListing() {
    println("testLsForRecursiveListing");
    //verify that when '-R' is used the files are listed recursively
    sh.exec("hdfs dfs -ls -R /user/$USERNAME/$testLsInputDir/$testLsInputs");
    assertTrue("listing recursive directory structure failed on HDFS",
                sh.getRet() == 0);

    TestLs_output = sh.getOut();
    String fileName = "/user/"+user_testinputdir+"/test_1.txt";
    assertTrue("Does ls output contains file " + fileName + "?",
        TestLs_output.grep(~/.*${fileName}.*/).size() > 0);

    fileName = "/user/"+user_testinputdir+"/test_2.txt";
    assertTrue("Does ls output contains file " + fileName + "?",
        TestLs_output.grep(~/.*${fileName}.*/).size() > 0);

    fileName = "/user/"+user_testinputdir+"/test.zip";
    assertTrue("Does ls output contains file " + fileName + "?",
        TestLs_output.grep(~/.*${fileName}.*/).size() > 0);
  }

  @Test
  public void testLsForHumanReadableFormat() {
    println("testLsForHumanReadableFormat");
    //verify that when '-h' and '-R' are used the files are listed recursively
    sh.exec("hdfs dfs -ls -h /user/$USERNAME/$testLsInputDir/" +
            "$testLsInputs/test_3 ");
    assertTrue("Able to list file size in human readable format?",
               sh.getRet() == 0);
    TestLs_output=sh.getOut();

    String fileName = "/user/$USERNAME/$testLsInputDir/$testLsInputs/test_3";
    assertTrue("Does output contains proper size value for " + fileName + "?",
        sh.getOut().grep(~/.*131.8 K.*/).size() > 0);
  }
}

