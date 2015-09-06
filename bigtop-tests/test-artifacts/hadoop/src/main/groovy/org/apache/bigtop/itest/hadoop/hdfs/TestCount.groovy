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

public class TestCount {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testCountInputDir = "testCountInputDir" + date;
  private static String testCountInputs = "test_data_TestCount"
  private static String testCountOut = "testCountOut" + date;
  private static String testCountOutCmp = "testCountOutCmp" + date;
  private static String TESTDIR  = "/user/$USERNAME/$testCountInputDir";

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestCount.class, "." , null);

    sh.exec("cp -r test_data test_data_TestCount");
    assertTrue("Could not copy data into test_data_TestCount",
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
    sh.exec("hdfs dfs -put $testCountInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    // set the replication if file exists
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCountInputs/test_2.txt");
    assertTrue("Could not find files on HDFS", sh.getRet() == 0);

    println("Running count:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      println("hdfs dfs -rm -r -skipTrash $TESTDIR")
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testCountOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCountOut from local disk");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -f $testCountOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCountOutCmp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testCountInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCountInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testCountWithqOption() {
    println("testCountWithqOption");
    sh.exec("hdfs dfs -count -q $TESTDIR");
    assertTrue("count command failed on HDFS", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    String file_name = "$TESTDIR";
    String output = out_msgs.get(0).toString();
    // Check the output contents
    if (!(output.contains("none") && output.contains("inf")  &&
      output.contains("2") && output.contains("4") &&
      output.contains(file_name))) {
      assertTrue("Does count output is not having correct data?", false);
    }
    // now check the size displayed
    String[] count_data = output.split("\\s+");
    sh.exec("hdfs dfs -du -s $TESTDIR");
    assertTrue("du command failed on HDFS", sh.getRet() == 0);

    List du_out_msgs = sh.getOut();
    String[] du_data = du_out_msgs.get(0).toString().split(" ");
    if (!(count_data[7].equals(du_data[0]))) {
      assertTrue("Is Size value in Count outut correct?", false);
    }

    // now check count option with inner directory
    sh.exec("hdfs dfs -count -q $TESTDIR/$testCountInputs");
    assertTrue("count command failed on HDFS", sh.getRet() == 0);
    out_msgs = sh.getOut();
    file_name = "$TESTDIR/$testCountInputs";
    output = out_msgs.get(0).toString();
    // Check the output contents
    if (!(output.contains("none") && output.contains("inf")  &&
        output.contains("1") && output.contains("4") &&
        output.contains(file_name))) {
      assertTrue("Does count output is not having correct data?", false);
    }
    // now check the size displayed
    count_data = output.split("\\s+");
    sh.exec("hdfs dfs -du -s $TESTDIR/$testCountInputs");
    assertTrue("du command failed on HDFS", sh.getRet() == 0);

    du_out_msgs = sh.getOut();
    du_data = du_out_msgs.get(0).toString().split(" ");
    if (!( count_data[7].equals(du_data[0]))) {
       assertTrue("Is Size value in Count outut correct?", false);
    }
  }

  @Test
  public void testCountWithEmptyDirectory() {
    println("testCountWithEmptyDirectory");
    sh.exec("hdfs dfs -mkdir $TESTDIR/$testCountInputs/empty_dir");
    sh.exec("hdfs dfs -count -q $TESTDIR/$testCountInputs/empty_dir");
    assertTrue("count command failed on HDFS", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    String file_name = "$TESTDIR/$testCountInputs/empty_dir";
    String output = out_msgs.get(0).toString();
    // Check the output contents
    if (!(output.contains("none") && output.contains("inf")  &&
        output.contains("1") && output.contains("0") &&
        output.contains(file_name))) {
      assertTrue("Does count output is not having correct data?", false);
    }

    String[] count_data = output.split("\\s+");
    sh.exec("hdfs dfs -du -s $TESTDIR/$testCountInputs/empty_dir");
    assertTrue("du command failed on HDFS", sh.getRet() == 0);

    List du_out_msgs = sh.getOut();
    String[] du_data = du_out_msgs.get(0).toString().split(" ");
    println(count_data[7]);
    if (!( count_data[7].equals(du_data[0]))) {
      assertTrue("count command failed for empty dir", false);
    }
  }

  @Test
  public void testCountWithNonExistentDirectory() {
    println("testCountWithNonExistentDirectory");
    sh.exec("hdfs dfs -count -q $TESTDIR/$testCountInputs/test");
    assertTrue("count command executed successfully with non existing dir " +
               "on HDFS", sh.getRet() == 1);
    List err_msgs = sh.getErr();
    Boolean failure = false;
    String failure_msg = "count: `$TESTDIR/$testCountInputs/test\': " +
                         "No such file or directory";
    String file_name = "$TESTDIR/$testCountInputs/test";
    // quota  remaining quota  directory count  file count  file name
    if (err_msgs.get(0).toString().contains(failure_msg)) {
      failure = true;
    }
    assertTrue("count command executed successfully for non existing file",
               failure == true);
  }

  @Test
  public void testCountOnDirectory() {
    println("testCountOnDirectory");
    sh.exec("hdfs dfs -count $TESTDIR");
    assertTrue("count command failed on HDFS", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    String file_name = "$TESTDIR";
    String output = out_msgs.get(0).toString();
    // Check the output contents
    if (!(output.contains("3") && output.contains("4") &&
        output.contains(file_name))) {
      assertTrue("Does count output is not having correct data?", false);
    }

    String[] count_data = output.split("\\s+");
    sh.exec("hdfs dfs -du -s $TESTDIR");
    assertTrue("du command failed on HDFS", sh.getRet() == 0);

    List du_out_msgs = sh.getOut();
    String[] du_data = du_out_msgs.get(0).toString().split(" ");
    if (!( count_data[3].equals(du_data[0]))) {
      assertTrue("count command failed for empty dir", false);
    }
  }
}

