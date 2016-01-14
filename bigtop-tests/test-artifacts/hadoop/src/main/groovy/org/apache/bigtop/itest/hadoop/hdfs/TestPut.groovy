/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional infoPutation
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
 * See the License for the specific language governing pePutissions and
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
import static org.apache.bigtop.itest.LogErrorsUtils.logError;
import java.util.ArrayList;
import java.util.List;

public class TestPut {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testPutInputDir = "testPutInputDir" + date;
  private static String testPutInputs = "test_data_TestPut"
  private static String testPutOut = "testPutOut" + date;
  private static String testPutOutCmp = "testPutOutCmp" + date;
  private static String user_testinputdir = USERNAME+"/"+testPutInputDir+"/"+
                                             testPutInputs;
  static List<String> TestPut_output = new ArrayList<String>();
  static List<String> TestPut_error = new ArrayList<String>();
  private static String TESTDIR  = "/user/$USERNAME/$testPutInputDir";
  static boolean result = false;

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestPut.class, "." , null);
    sh.exec("cp -r test_data test_data_TestPut");
    assertTrue("Could not copy data into test_data_TestPut", sh.getRet() == 0);

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

    println("Running Put:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testPutOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testPutOut");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
    sh.exec("test -f $testPutOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testPutOutCmp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testPutInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testPutInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testPutDirectory() {
    println("testPutDirectory");
    // upload directory to hdfs
    sh.exec("hdfs dfs -put $testPutInputs $TESTDIR ");
    assertTrue("Could not put files to HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -ls -R $TESTDIR/$testPutInputs ");
    assertTrue("could not find the copied directory on hdfs",
               sh.getRet() == 0);

    assertTrue("Able to find uploaded files on hdfs?",
        sh.getOut().grep(~/.*test_3.*/).size() > 0);
    result = false;
  }

  @Test
  public void testPutFile() {
    println("testPutFile");
    // upload single files
    sh.exec("hdfs dfs -put $testPutInputs/test_1.txt $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -cat $TESTDIR/test_1.txt &> $testPutOut");
    assertTrue("Able to cat data from $TESTDIR/test_1.txt from hdfs?",
               sh.getRet() == 0);

    sh.exec("cat $testPutInputs/test_1.txt &> $testPutOutCmp");
    assertTrue("Able to cat data from $testPutInputs/test_1.txt from local?",
               sh.getRet() == 0);

    sh.exec("diff $testPutOutCmp $testPutOut");
    assertTrue("Uploaded file data differs with local file?",
               sh.getRet() == 0);
  }

  @Test
  public void testPutMutltipleFiles() {
    println("TestPutAdvanced");
    // copy multiple input files to hdfs
    sh.exec("hdfs dfs -put $testPutInputs/test_2.txt $testPutInputs/test.zip "+
            "$testPutInputs/test_3 $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    sh.exec("hdfs dfs -ls -R $TESTDIR ");
    assertTrue("could not find the copied directory on hdfs",
               sh.getRet() == 0);

    assertTrue("Does test_2.txt uploaded properly?",
        sh.getOut().grep(~/.*test_2.txt.*/).size() > 0);

    assertTrue("Does test.zip uploaded properly?",
        sh.getOut().grep(~/.*test.zip.*/).size() > 0);

    assertTrue("Does test_3 uploaded properly?",
        sh.getOut().grep(~/.*test_3.*/).size() > 0);
  }

  @Test
  public void testPutNonExistingFile() {
    println("testPutNonExistingFile");
    sh.exec("hdfs dfs -put $testPutInputs/test_3.txt $TESTDIR");
    assertTrue("A non existing file got copied to hdfs", sh.getRet() == 1);

    String searchToken = "put: `"+testPutInputs+"/test_3.txt': " +
                         "No such file or directory";
    println(searchToken);
    assertTrue("Able to Upload non-existing file?",
        sh.getErr().grep(~/.*${searchToken}.*/).size() > 0);
  }

  @Test
  public void testPutToOverWriteFile() {
    println("testPutNonExistingFile");
    // copy a file which is already present on HDFS at the destination
    sh.exec("hdfs dfs -test -f $TESTDIR/test_1.txt");
    if (sh.getRet() == 1){
      sh.exec("hdfs dfs -put $testPutInputs/test_1.txt $TESTDIR");
      assertTrue("Able to upload file?", sh.getRet() == 0);
    }

    sh.exec("hdfs dfs -put $testPutInputs/test_1.txt $TESTDIR ");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 1);

    String searchToken = "put: `/user/"+USERNAME+"/"+
                          testPutInputDir+"/test_1.txt': File exists";
    assertTrue("Able to Upload non-existing file?",
        sh.getErr().grep(~/.*${searchToken}.*/).size() > 0);
  }
}
