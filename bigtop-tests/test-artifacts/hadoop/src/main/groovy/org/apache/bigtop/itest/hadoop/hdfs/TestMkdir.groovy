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
import static org.junit.Assert.assertFalse;
import org.junit.AfterClass;
import org.junit.*;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.hadoop.conf.Configuration;
import org.apache.bigtop.itest.JarContent;
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import java.util.ArrayList;
import java.util.List;

public class TestMkdir {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for mkdir absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testMkdirInputDir = "testMkdirInputDir" + date;
  private static String testMkdirOut = "testMkdirOut" + date;
  private static int repfactor = 2;
  private static String user_testinputdir  = USERNAME+"/"+testMkdirInputDir;
  static List<String> TestMkdir_output = new ArrayList<String>();
  static List<String> TestMkdir_error = new ArrayList<String>();
  private static String TESTDIR  = "/user/$USERNAME/$testMkdirInputDir";
  static boolean result = false;

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestMkdir.class, "." , null);
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
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testMkdirOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testMkdirOut");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testMkdirBasics() {
    println("TestMkdirBasics");
    result = false;
    // test whether basic mkdir command works
    sh.exec("hdfs dfs -mkdir $TESTDIR/test10");
    assertTrue("Able to create directory?", sh.getRet() == 0);

    sh.exec("hdfs dfs -ls $TESTDIR ");
    assertTrue("Directory found on HDFS?", sh.getRet() == 0);

    TestMkdir_output=sh.getOut();
    for (String output_String : TestMkdir_output) {
      String[] string= output_String.split(" ");
      if (output_String.contains("test10") &&
          string[0].contains("drwxr-xr-x")) {
        result = true;
        break;
      }
    }
    assertTrue("Does Directory created properly on hdfs?", result == true);
  }

  @Test
  public void testMkdirWithpOption() {
    println("testMkdirWithpOption");
    // test whether mkdir command works with -p option
    sh.exec("hdfs dfs -mkdir -p $TESTDIR/test1/test2");
    assertTrue("Could not create directory on HDFS", sh.getRet() == 0);

    sh.exec("hdfs dfs -ls $TESTDIR/test1 ");
    assertTrue("directory not found on HDFS", sh.getRet() == 0);

    assertTrue("Does $TESTDIR/test1/test2 folder got created with -p option?",
        sh.getOut().grep(~/.*${TESTDIR}\/test1\/test2.*/).size() > 0);
  }

  @Test
  public void testMkdirWithOutpOption() {
    println("testMkdirWithOutpOption");
    /* 
     * test whether a directory can be created without creating a parent directory
     * without using '-p'
     */
    sh.exec("hdfs dfs -mkdir $TESTDIR/test2/test2");
    assertTrue("Directory created on HDFS without -p option", sh.getRet() == 1);

    String errMsg = "mkdir: `$TESTDIR/test2/test2': No such file or directory";
    assertTrue("Does $TESTDIR/test2/test2 folder created without -p option?",
        sh.getErr().grep(~/.*${errMsg}.*/).size() > 0);
  }

  @Test
  public void testMkdirWithExistingDir() {
    println("testMkdirWithExistingDir");
    // test whether msg is thrown if the directory already exists
    sh.exec("hdfs dfs -mkdir $TESTDIR");
    assertTrue("Able to create existing directory?", sh.getRet() == 1);

    String errMsg = "mkdir: `$TESTDIR': File exists";
    assertTrue("Does $TESTDIR folder created",
        sh.getErr().grep(~/.*${errMsg}.*/).size() > 0);
  }

  @Test
  public void testMkdirWithMultipleDirectories() {
    //test creating multiple directories
    sh.exec("hdfs dfs -mkdir -p $TESTDIR/test2 $TESTDIR/test3");
    assertTrue("Able to Create multiple directories on HDFS?",
               sh.getRet() == 0);

    sh.exec("hdfs dfs -ls -d $TESTDIR/test2");
    assertTrue("ls command failed on HDFS", sh.getRet() == 0);

    assertTrue("Does $TESTDIR/test2 folder created?",
        sh.getOut().grep(~/.*${TESTDIR}\/test2.*/).size() > 0);

    sh.exec("hdfs dfs -ls -d $TESTDIR/test3");
    assertTrue("ls command failed on HDFS", sh.getRet() == 0);

    assertTrue("Does $TESTDIR/test3 folder created?",
        sh.getOut().grep(~/.*${TESTDIR}\/test3.*/).size() > 0);
  }
}
