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

public class TestTouchz {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testTouchzInputDir = "testTouchzInputDir" + date;
  private static String testTouchzInputs = "test_data_TestTouchz";
  private static String testTouchzOut = "testTouchzOut" + date;
  private static String testTouchzOutCmp= "testTouchzOutCmp" + date;
  private static int repfactor = 2;
  private static String TESTDIR  = "/user/$USERNAME/$testTouchzInputDir";
  private static String user_testinputdir = USERNAME+"/"+testTouchzInputDir+
                                            "/"+testTouchzInputs;
  static List<String> TestTouchz_output = new ArrayList<String>();
  static List<String> TestTouchz_error = new ArrayList<String>();
  static boolean result = false;

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestTouchz.class, "." , null);

    sh.exec("cp -r test_data test_data_TestTouchz");
    assertTrue("Could not copy data into test_data_TestTouchz", sh.getRet() == 0);

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
    sh.exec("hdfs dfs -put $testTouchzInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    println("Running touchz:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testTouchzOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testTouchzOut");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
    sh.exec("test -f $testTouchzOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testTouchzOutCmp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testTouchzInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testTouchzInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testTouchzBasics() {
    println("testTouchzBasics");
    // test whether basic stat command works
    sh.exec("hdfs dfs -touchz $TESTDIR/test_3.txt");
    assertTrue("touchz command on HDFS failed", sh.getRet() == 0);
    // check if file is present on hdfs
    sh.exec("hdfs dfs -test -f $TESTDIR/test_3.txt");
    assertTrue("file does not found on HDFS", sh.getRet() == 0);
    // check if path is empty
    sh.exec("hdfs dfs -test -s $TESTDIR/test_3.txt");
    assertTrue("created file is not zero size", sh.getRet() == 1);

    // try crearte multiple files with touchz
    sh.exec("hdfs dfs -touchz $TESTDIR/test_4.txt $TESTDIR/test_5.txt");
    assertTrue("touchz command on HDFS failed", sh.getRet() == 0);

    sh.exec("hdfs dfs -test -f $TESTDIR/test_4.txt");
    assertTrue("file does not found on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -test -s $TESTDIR/test_4.txt");
    assertTrue("created file is not zero size", sh.getRet() == 1);

    sh.exec("hdfs dfs -test -f $TESTDIR/test_5.txt");
    assertTrue("file does not found on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -test -s $TESTDIR/test_5.txt");
    assertTrue("created file is not zero size", sh.getRet() == 1);
  }

  @Test
  public void testTouchzToCreateDirectory() {
    println("testTouchzNegatives");
    sh.exec("hdfs dfs -touchz $TESTDIR/$testTouchzInputs");
    assertTrue("touchz command on HDFS failed", sh.getRet() == 1);

    String errMsg = "touchz: `/user/"+user_testinputdir+"': Is a directory";
    assertTrue("Does touch failed to create zero length directory?",
        sh.getErr().grep(~/.*${errMsg}.*/).size() > 0);
  }

  @Test
  public void testTouchzInNonExistentDirectory() {
    println("testTouchzInNonExistentDirectory");
    sh.exec("hdfs dfs -touchz $TESTDIR/test_dir2/test_6.txt ");
    assertTrue("touchz command on HDFS executed successfully?", sh.getRet() == 1);

    String errMsg = "touchz: `/user/"+USERNAME+"/"+testTouchzInputDir+
                    "/test_dir2/test_6.txt': No such file or directory";
    assertTrue("Does touchz able to create files in non-existent directory?",
        sh.getErr().grep(~/.*${errMsg}.*/).size() > 0);
  }

  @Test
  public void testTouchzOnExistingFile() {
    println("testTouchzInNonExistentDirectory");
    sh.exec("hdfs dfs -touchz $TESTDIR/$testTouchzInputs/test_1.txt ");
    assertTrue("Does touchz command executed properly?", sh.getRet() == 1);

    String errMsg = "touchz: `/user/"+user_testinputdir+
                    "/test_1.txt': Not a zero-length file";
    assertTrue("Does touch able to make existing file as zero size file?",
        sh.getErr().grep(~/.*${errMsg}.*/).size() > 0);
  }
}

