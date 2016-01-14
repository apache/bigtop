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

public class TestStat {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testStatInputDir = "testStatInputDir" + date;
  private static String testStatInputs = "test_data_TestStat";
  private static String testStatOut = "testStatOut" + date;
  private static String testStatOutCmp= "testStatOutCmp" + date;
  private static int repfactor = 2;
  private static String TESTDIR  = "/user/$USERNAME/$testStatInputDir";
  static boolean result = false;

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestStat.class, "." , null);

    sh.exec("cp -r test_data test_data_TestStat");
    assertTrue("Could not copy data into test_data_TestStat",
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
    assertTrue("Could not create input directory on HDFS",
               sh.getRet() == 0);

    // copy input directory to hdfs
    sh.exec("hdfs dfs -put $testStatInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    // set the replication if file exists
    sh.exec("hdfs dfs -test -f $TESTDIR/$testStatInputs/test_2.txt");
    assertTrue("Could not find files on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -setrep $repfactor " +
            "$TESTDIR/$testStatInputs/test_3");
    assertTrue("Could not set replication factor", sh.getRet() == 0);

    println("Running stat:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }
    sh.exec("hdfs dfs -test -d $testStatOut");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $testStatOut");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }
    sh.exec("hdfs dfs -test -d $testStatOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $testStatOutCmp");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -d $testStatInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testStatInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testStatBasics() {
    println("TestStatBasics");
    // test whether basic stat command works
    sh.exec("hdfs dfs -stat $TESTDIR/$testStatInputs | awk -F ':' " +
            "'BEGIN { OFS=\":\"} {print \$1,\$2}' &> $testStatOut");
    assertTrue("stat command on HDFS failed", sh.getRet() == 0);

    // compare -stat output with -ls output
    sh.exec("hdfs dfs -ls $TESTDIR | tail -1 | awk '{print \$6, \$7}' &> " +
            "$testStatOutCmp");
    assertTrue("ls command on HDFS failed", sh.getRet() == 0);

    sh.exec("diff $testStatOut $testStatOutCmp");
    assertTrue("Does stat command shows correct data?", sh.getRet() == 0);

    sh.exec("hdfs dfs -stat $TESTDIR/$testStatInputs/* | awk -F ':' " +
            "'BEGIN { OFS=\":\"} {print \$1,\$2}' &> $testStatOut");
    assertTrue("stat command on HDFS failed", sh.getRet() == 0);

    // compare -stat output with -ls output
    sh.exec("hdfs dfs -ls $TESTDIR/$testStatInputs | tail -4 | " +
            "awk '{print \$6, \$7}' &> $testStatOutCmp");
    assertTrue("ls command on HDFS failed", sh.getRet() == 0);

    sh.exec("diff $testStatOut $testStatOutCmp");
    assertTrue("Does stat provides correct data?", sh.getRet() == 0);
  }

  @Test
  public void testStatReplication() {
    println("testStatReplication");

    sh.exec("hdfs dfs -stat \"%r\" $TESTDIR/$testStatInputs/test_3");
    assertTrue("Able to get Replication details of a file using stat",
               sh.getRet() == 0);

    if (!(sh.getOut().get(0).toString().equals("2"))) {
      assertTrue("Does stat provides correct value for replication?", false);
    }
  }

  @Test
  public void testStatForFileType() {
    println("testStatForFileType");

    sh.exec("hdfs dfs -stat \"%F\" $TESTDIR/$testStatInputs/test_3");
    assertTrue("Able to get file type details of a file using stat",
               sh.getRet() == 0);

    if (!(sh.getOut().get(0).toString().equals("regular file"))) {
      assertTrue("Does stat provides correct value for File type?", false);
    }

    sh.exec("hdfs dfs -stat \"%F\" $TESTDIR/$testStatInputs");
    assertTrue("Able to get file type  details of a file using stat",
               sh.getRet() == 0);

    if (!(sh.getOut().get(0).toString().equals("directory"))) {
      assertTrue("Does stat provides correct value for File type?", false);
    }
  }

  @Test
  public void testStatForName() {
    println("testStatForName");

    sh.exec("hdfs dfs -stat \"%n\" $TESTDIR/$testStatInputs/test_3");
    assertTrue("Able to get name of a file using stat",
               sh.getRet() == 0);

    if (!(sh.getOut().get(0).toString().equals("test_3"))) {
      assertTrue("Does stat provides correct value for name?", false);
    }
  }

  @Test
  public void testStatWithAllOptions() {
    println("testStatWithAllOptions");

    sh.exec("hdfs dfs -stat \"%F %u:%g %b %y %n\" "+
            "$TESTDIR/$testStatInputs/test_3");
    assertTrue("Able to get all details of a file using stat",
               sh.getRet() == 0);

    String output = sh.getOut().get(0).toString();
    if (!(output.contains("regular file") && output.contains("135008") && output.contains("test_3") ) ) {
      assertTrue("Does stat provides correct values in stat ouput?", false);
    }
  }

  @Test
  public void testStatNegative() {
    println("TestStatNegative");
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("hdfs dfs -stat $TESTDIR/$testStatInputs");
    assertTrue("stat command on HDFS failed", sh.getRet() == 1);

    String errMsg = "stat: `$TESTDIR/$testStatInputs': " +
                    "No such file or directory";
    assertTrue("Does stat provides correct message for non-existent fodler?",
        sh.getErr().grep(~/.*${errMsg}.*/).size() > 0);
  }
}
