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
import java.util.ArrayList;
import java.util.List;

public class TestCmdTest {

  private static Shell sh = new Shell("/bin/bash -s");
  // extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testCmdTestInputDir = "testCmdTestInputDir" + date;
  private static String testCmdTestInputs = "test_data_TestCmdTest"
  private static String testCmdTestOut = "testCmdTestOut" + date;
  private static String testCmdTestOutCmp = "testCmdTestOutCmp" + date;
  private static String user_testinputdir  = USERNAME+"/"+testCmdTestInputDir+
                                             "/"+testCmdTestInputs;
  private static String TESTDIR  = "/user/$USERNAME/$testCmdTestInputDir";

  private String teststr = "test: Too many arguments: expected 1 but got 2";

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestCmdTest.class, "." , null);

    sh.exec("cp -r test_data test_data_TestCmdTest");
    assertTrue("Could not copy data into test_data_TestCmdTest",
               sh.getRet() == 0);

    // get namenode hostname from core-site.xml
    Configuration conf = new Configuration();
    namenode = conf.get("fs.defaultFS");
    if (namenode == null) {
      namenode = conf.get("fs.default.name");
    }
    assertTrue("Could not find namenode", namenode != null);

    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("hdfs dfs -mkdir -p $TESTDIR");
    assertTrue("Could not create input directory on HDFS", sh.getRet() == 0);

    // copy input directory to hdfs
    sh.exec("hdfs dfs -put $testCmdTestInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    println("Running cmdtest:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testCmdTestOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCmdTestOut from local disk");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -f $testCmdTestOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCmdTestOutCmp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testCmdTestInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCmdTestInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testForDirectory() {
    // test whether if the given path is a directory or not
    sh.exec("hdfs dfs -test -d $TESTDIR/$testCmdTestInputs");
    assertTrue("test command for directory on HDFS failed", sh.getRet() == 0);
  }

  @Test
  public void testDirectoryExists() {
    println("testDirectoryExists");
    sh.exec("hdfs dfs -test -e $TESTDIR/$testCmdTestInputs");
    assertTrue("test command for directory on HDFS failed",
               sh.getRet() == 0);
  }

  @Test
  public void testIfFileExists() {
    println("testIfFileExists");
    sh.exec("hdfs dfs -test -e $TESTDIR/$testCmdTestInputs/test_1.txt ");
    assertTrue("test command for file on HDFS failed", sh.getRet() == 0);
  }

  @Test
  public void testWithFile() {
    println("testWithFile");
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCmdTestInputs/test_2.txt ");
    assertTrue("test command for file on HDFS failed", sh.getRet() == 0);
  }

  @Test
  public void testForNonEmptyPath() {
    println("testForNonEmptyPath");
    sh.exec("hdfs dfs -test -s $TESTDIR/$testCmdTestInputs/test_2.txt ");
    assertTrue("test command for non-empty file on HDFS failed",
               sh.getRet() == 0);
  }

  @Test
  public void testForEmptyPath() {
    println("testForNonEmptyPath");
    // create empty file
    sh.exec("touch test_3.txt");
    assertTrue("touch command failed", sh.getRet() == 0);

    // move the file to hdfs
    sh.exec("hdfs dfs -put test_3.txt $TESTDIR/$testCmdTestInputs/. ");
    assertTrue("could not copy input to HDFS", sh.getRet() == 0);

    // now check with -test -z
    sh.exec("hdfs dfs -test -z $TESTDIR/$testCmdTestInputs/test_3.txt ");
    assertTrue("test command for empty file failed on HDFS", sh.getRet() == 0);
  }

  @Test
  public void testdoptionWithFile() {
    println("testdoptionWithFile");
    sh.exec("hdfs dfs -test -d $TESTDIR/$testCmdTestInputs/test_1.txt ");
    assertTrue("test command for directory worked on a file",
               sh.getRet() == 1);
  }

  @Test
  public void testdoptionWithNonExistentDirectory() {
    println("testdoptionWithNonExistentDirectory");
    sh.exec("hdfs dfs -test -d $TESTDIR/test_dir ");
    assertTrue("test command for non-existing directory executed " +
               "successfully on HDFS", sh.getRet() == 1);
  }

  @Test
  public void testfoptionWithDirectory() {
    println("testfoptionWithDirectory");
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCmdTestInputs");
    assertTrue("test command for file worked on a directory",
               sh.getRet() == 1);
  }

  @Test
  public void testsoptionWithDirectory() {
    println("testsoptionWithDirectory");
    sh.exec("hdfs dfs -test -s $TESTDIR/$testCmdTestInputs");
    assertTrue("test command for file worked on a directory",
               sh.getRet() == 1);
  }

  @Test
  public void testzoptionWithDirectory() {
    println("testsoptionWithDirectory");
    sh.exec("hdfs dfs -test -z $TESTDIR/$testCmdTestInputs ");
    assertTrue("test command for directory failed", sh.getRet() == 0);
  }

  @Test
  public void testeoptionWithNonExistentFile() {
    println("testeoptionWithNonExistentFile");
    sh.exec("hdfs dfs -test -e $TESTDIR/$testCmdTestInputs/test_4.txt");
    assertTrue("test command for non existing file worked", sh.getRet() == 1);
  }

  @Test
  public void testfoptionWithNonExistentFile() {
    println("testfoptionWithNonExistentFile");
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCmdTestInputs/test_4.txt ");
    assertTrue("test command for non existing file worked", sh.getRet() == 1);
  }

  @Test
  public void testsoptionWithNonExistentFile() {
    println("testsoptionWithNonExistentFile");
    sh.exec("hdfs dfs -test -s $TESTDIR/$testCmdTestInputs/test_4.txt ");
    assertTrue("test command for non existing file worked", sh.getRet() == 1);
  }

  @Test
  public void testzoptionWithNonExistentFile() {
    println("testzoptionWithNonExistentFile");
    sh.exec("hdfs dfs -test -z $TESTDIR/$testCmdTestInputs/test_4.txt ");
    assertTrue("test command for non existing file worked", sh.getRet() == 1);
  }

  @Test
  public void testzoptionWithNonEmptyFile() {
    println("testzoptionWithNonEmptyFile");
    sh.exec("hdfs dfs -test -z $TESTDIR/$testCmdTestInputs/test_1.txt ");
    assertTrue("test -z command on non-empty file worked", sh.getRet() == 1);
  }
 
  @Test
  public void testdoptionWithMultipleDirectories() {
    println("testForMultipleDirectories");
    // invoke test comamnd with multiple directory names
    sh.exec("hdfs dfs -test -d $TESTDIR $TESTDIR/$testCmdTestInputs");
    assertTrue("test command accepted multiple directory paths",
               sh.getRet() == 255);

    assertTrue("Does test command worked with multiple directories?",
               lookForGivenString(sh.getErr(), teststr) == true);
  }

  @Test
  public void testeoptionWithMultipleDirectories() {
    println("testDirectoryExistsForMultipleDirectories");
    // check if test command work with multiple directories with -e option
    sh.exec("hdfs dfs -test -e $TESTDIR $TESTDIR/$testCmdTestInputs");
    assertTrue("test command accepted multiple directory paths",
               sh.getRet() == 255);

    assertTrue("Does -test -e worked with multiple Directories?",
               lookForGivenString(sh.getErr(), teststr) == true);
  }

  @Test
  public void testeoptionWithMultipleFilesExists() {
    println("testForMultipleFilesExists");
    sh.exec("hdfs dfs -test -e $TESTDIR/$testCmdTestInputs/test_1.txt "+
            "$TESTDIR/$testCmdTestInputs/test_2.txt ");
    assertTrue("test command accepted multiple file paths",
               sh.getRet() == 255);

    assertTrue("Does -test -e worked with multiple files?",
               lookForGivenString(sh.getErr(), teststr) == true);
  }

  @Test
  public void testfoptionWithMultipleFiles() {
    println("testForMultipleFiles");
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCmdTestInputs/test_1.txt "+
            "$TESTDIR/$testCmdTestInputs/test_2.txt ");
    assertTrue("test command accepted multiple file paths", sh.getRet() == 255);

    assertTrue("Does -test -f worked with multiple files?",
               lookForGivenString(sh.getErr(), teststr) == true);
  }

  @Test
  public void testsoptionWithMultipleFiles() {
    println("testForNonEmptyWithMultipleFiles");
    sh.exec("hdfs dfs -test -s /$TESTDIR/$testCmdTestInputs/test_1.txt "+
            "$TESTDIR/$testCmdTestInputs/test_2.txt ");
    assertTrue("test command accepted multiple file paths", sh.getRet() == 255);

    assertTrue("expected pattern not found in the output file",
               lookForGivenString(sh.getErr(), teststr) == true);
  }

  @Test
  public void testzoptionWithMultipleFiles() {
    println("testForEmptyWithMultipleFiles");
    sh.exec("hdfs dfs -test -z $TESTDIR/$testCmdTestInputs/test_1.txt " +
            "$TESTDIR/$testCmdTestInputs/test_3.txt ");
    assertTrue("test command accepted multiple directory paths",
               sh.getRet() == 255);

    assertTrue("Does -test -z worked with multiple directories?",
               lookForGivenString(sh.getErr(), teststr) == true);
  }

  /**
   * lookForGivenString checks whether the given search string is present
   * in the provided data list.
   */
  private boolean lookForGivenString(List<String> data,
                                     String searchString) {
    boolean result = false;
    for( String output_String : data) {
      if(output_String.contains(searchString)) {
        result = true;
        break;
      }
    }
    return result;
  }
}
