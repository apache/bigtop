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

public class TestGet {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static final String USERDIR = System.getProperty("user.dir");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testGetInputDir = "testGetInputDir" + date;
  private static String testGetInputs = "test_data_TestGet"
  private static String TESTDIR  = "/user/$USERNAME/$testGetInputDir";

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestGet.class, "." , null);

    sh.exec("cp -r test_data $testGetInputs");
    logError(sh);
    assertTrue("Could not copy data into $testGetInputs .", sh.getRet() == 0);

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
    sh.exec("hdfs dfs -put $testGetInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    sh.exec("test -d temp_testget");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf temp_testget");
    }
    sh.exec("mkdir temp_testget");
    assertTrue("could not create a dir", sh.getRet() == 0);

    println("Running get:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -d temp_testget");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf temp_testget");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testGetInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testGetInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d temp_testget/test_optionscrc");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf temp_testget/test_optionscrc");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testGetFile() {
    println("testGetFile");

    //get test_1.txt file from hdfs to local
    sh.exec("hdfs dfs -get $TESTDIR/$testGetInputs/test_1.txt temp_testget");
    assertTrue("get command failed", sh.getRet() == 0);
    sh.exec("diff temp_testget/test_1.txt $testGetInputs/test_1.txt");
    logError(sh);
    assertTrue("files differ in content", sh.getRet() == 0);
  }

  @Test
  public void testGetDirectory() {
    println("testGetDirectory");
    // get a dir from hdfs to local
    sh.exec("hdfs dfs -get $TESTDIR/$testGetInputs temp_testget");
    assertTrue("get command failed", sh.getRet() == 0);
    sh.exec("ls -l temp_testget/$testGetInputs");
    assertTrue("listing files/directories failed on HDFS", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Boolean success_1= false;
    Boolean success_2= false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") &&
          next_val.contains("test_2.txt")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") &&
          next_val.contains("test_3"))  {
        success_2 = true;
        continue;
      }
    }
    assertTrue("Able to find Downloaded files?",
               success_1 == true && success_2 == true);
  }

  @Test
  public void testGetFileWhenFileExistsAtLocal() {
    println("testGetFileWhenFileExistsAtLocal");
    //get test_2.txt file from hdfs a location where the file already exists
    sh.exec("test -f temp_testget/$testGetInputs/test_2.txt");
    if (sh.getRet() == 1) {
      sh.exec("hdfs dfs -get $TESTDIR/$testGetInputs temp_testget");
      assertTrue("get command failed for directory", sh.getRet() == 0);
    }

    sh.exec("hdfs dfs -get $TESTDIR/$testGetInputs/test_2.txt " +
            "temp_testget/$testGetInputs/test_2.txt");
    assertTrue("get command failed on HDFS", sh.getRet() == 1);

    String failure_msg = "get: `temp_testget/$testGetInputs/test_2.txt': " +
                         "File exists";
    assertTrue("Does get command properly failed to download an existing file?",
        sh.getErr().grep(~/${failure_msg}.*/).size() > 0);
  }

  @Test
  public void testGetFileNonExistingFile() {
    println("testGetFileNonExistingFile");
    //get test_4.txt(non existing) from hdfs to local
    sh.exec("hdfs dfs -get $TESTDIR/$testGetInputs/test_4.txt temp_testget");
    assertTrue("get command failed on HDFS", sh.getRet() == 1);

    String failure_msg = "get: `$TESTDIR/$testGetInputs/test_4.txt': " +
                         "No such file or directory";
    assertTrue("Does get command failed to download non existing file?",
        sh.getErr().grep(~/${failure_msg}.*/).size() > 0);
  }

  @Test
  public void testGetFileWithOutSpecifyingDestination() {
    println("testGetFileWithOutSpecifyingDestination");
    //get test_2.txt from hdfs to local, with out specifying any destination
    sh.exec("hdfs dfs -get $TESTDIR/$testGetInputs/test_2.txt");
    assertTrue("Does Get command worked when no destination is specified?",
               sh.getRet() == 0);

    sh.exec("diff test_2.txt $testGetInputs/test_2.txt");
    assertTrue("files differ in content", sh.getRet() == 0);

    sh.exec("rm -f test_2.txt");
    assertTrue("could not remove a file", sh.getRet() == 0);
  }

  @Test
  public void testGetWithCrc() {
    println("testGetWithCrc");
    sh.exec("mkdir -p temp_testget/test_optionscrc");
    //get a text file with crc
    assertTrue("Able to create directory?", sh.getRet() == 0);

    //get test_2.txt file from hdfs a location where the file already exists
    sh.exec("hdfs dfs -get -crc $TESTDIR/$testGetInputs/test_2.txt " +
            "temp_testget/test_optionscrc/test_4.txt");
    assertTrue("Does get command worked properly with crc option?",
               sh.getRet() == 0);

    sh.exec("ls -la temp_testget/test_optionscrc");
    assertTrue("listing files/directories failed on HDFS", sh.getRet() == 0);

    assertTrue("Does get command download file with crc?",
        sh.getOut().grep(~/.*.test_4.txt.crc.*/).size() > 0);
    // now compare the contents
    sh.exec("diff temp_testget/test_optionscrc/test_4.txt $testGetInputs/test_2.txt");
    logError(sh);
    assertTrue("files differ in content", sh.getRet() == 0);
  }
 
  @Test
  public void testGetWithoutCrc() {
    println("testGetWithoutCrc");

    //get a text file without crc
    sh.exec("hdfs dfs -get -ignoreCrc $TESTDIR/$testGetInputs/test_1.txt " +
            "temp_testget/test_5.txt");
    assertTrue("get command failed on HDFS", sh.getRet() == 0);

    sh.exec("ls -la temp_testget");
    assertTrue("listing files/directories failed on HDFS", sh.getRet() == 0);

    assertTrue("Does get command skipped crc file properly?",
        sh.getOut().grep(~/.*.test_5.txt.crc.*/).size() == 0);
    // now compare the contents
    sh.exec("diff $testGetInputs/test_1.txt temp_testget/test_5.txt");
    logError(sh);
    assertTrue("files differ in content", sh.getRet() == 0);
  }
}

