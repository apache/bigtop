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
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestCp {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static final String USERDIR = System.getProperty("user.dir");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testCpInputDir = "testCpInputDir" + date;
  private static String testCpInputs = "test_data_TestCp"
  private static String testCpOut = "testCpOut" + date;
  private static String testCpOutCmp = "testCpOutCmp" + date;
  private static String TESTDIR  = "/user/$USERNAME/$testCpInputDir";

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestCp.class, "." , null);
    sh.exec("cp -r test_data test_data_TestCp");
    logError(sh);
    assertTrue("Could not copy data into test_data_TestCp.", sh.getRet() == 0);

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

    sh.exec("test -d temp_testcp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf temp_testcp");
    }
    sh.exec("mkdir temp_testcp");
    assertTrue("could not create a dir", sh.getRet() == 0);

    // copy input directory to hdfs
    sh.exec("hdfs dfs -put $testCpInputs $TESTDIR");
    logError(sh);
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    // set the replication if file exists
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCpInputs/test_2.txt");
    assertTrue("Could not find files on HDFS", sh.getRet() == 0);

    println("Running cp:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      println("hdfs dfs -rm -r -skipTrash $TESTDIR")
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testCpOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCpOut from local disk");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -f $testCpOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCpOutCmp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d temp_testcp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf temp_testcp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testCpInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCpInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testCpForFiles() {
    println("testCpForFiles");
    // first delete the test_3 file
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCpInputs/test_3.txt");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm $TESTDIR/$testCpInputs/test_3.txt");
      assertTrue("failed to cleanup file from destination", sh.getRet() == 0);
    }
    // copy test_1.txt file to test_3.txt on hdfs
    sh.exec("hdfs dfs -cp $TESTDIR/$testCpInputs/test_1.txt $TESTDIR/$testCpInputs/test_3.txt");
    assertTrue("copy command failed on HDFS", sh.getRet() == 0);

    sh.exec("hdfs dfs -get $TESTDIR/$testCpInputs/test_1.txt temp_testcp/test_1.txt");
    assertTrue("get command for 1st file failed on HDFS", sh.getRet() == 0);

    sh.exec("hdfs dfs -get $TESTDIR/$testCpInputs/test_3.txt temp_testcp/test_3.txt");
    assertTrue("get command for 2nd file failed on HDFS", sh.getRet() == 0);

    sh.exec("diff temp_testcp/test_1.txt temp_testcp/test_3.txt");
    logError(sh);
    assertTrue("files differ in content", sh.getRet() == 0);

    sh.exec("rm -f temp_testcp/test_1.txt temp_testcp/test_3.txt");
    assertTrue("could not remove the files", sh.getRet() == 0);
  }

  @Test
  public void testCpForDirectories() {
    sh.exec("hdfs dfs -cp $TESTDIR/$testCpInputs $TESTDIR/test_temp");
    assertTrue("copy command failed on HDFS", sh.getRet() == 0);

    sh.exec("hdfs dfs -ls -R $TESTDIR/test_temp");
    assertTrue("listing files/directories failed on HDFS", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1= false;
    Boolean success_2= false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") && next_val.contains("$TESTDIR/test_temp/test_2.txt")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") && next_val.contains("$TESTDIR/test_temp/test_1.txt"))  {
        success_2 = true;
        continue;
      }
    }
    assertTrue("Copied files do not match", success_1 == true && success_2 == true);
  }

  @Test
  public void testCopyExistingFile() {
    println("testCopyExistingFile");
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCpInputs/test_3.txt");
    if (sh.getRet() == 1) {
      sh.exec("hdfs dfs -cp $TESTDIR/$testCpInputs/test_1.txt $TESTDIR/$testCpInputs/test_3.txt");
      assertTrue("failed to copy a file to HDFS", sh.getRet() == 0);
    }

    //copy test_2.txt file to test_3.txt on hdfs, see if it gets overwritten
    sh.exec("hdfs dfs -cp $TESTDIR/$testCpInputs/test_2.txt $TESTDIR/$testCpInputs/test_3.txt");
    assertTrue("copy command failed on HDFS", sh.getRet() == 1);
    List err_msgs = sh.getErr();
    Boolean failure= false;
    String failure_msg = "cp: `$TESTDIR/$testCpInputs/test_3.txt': File exists";
    if (err_msgs.get(0).toString().contains(failure_msg)){
      failure = true;
    }
    assertTrue("copy command failed", failure == true);
  }
 
  @Test
  public void testCopyOverwriteFile() {
    println("testCopyOverwriteFile");
    sh.exec("hdfs dfs -test -f $TESTDIR/$testCpInputs/test_3.txt");
    if (sh.getRet() == 1) {
      sh.exec("hdfs dfs -cp $TESTDIR/$testCpInputs/test_1.txt $TESTDIR/$testCpInputs/test_3.txt");
      assertTrue("failed to copy a file to HDFS", sh.getRet() == 0);
    }

    //copy test_2.txt file to test_3.txt on hdfs, with overwrite flag
    sh.exec("hdfs dfs -cp -f $TESTDIR/$testCpInputs/test_2.txt $TESTDIR/$testCpInputs/test_3.txt");
    assertTrue("copy command failed on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -get $TESTDIR/$testCpInputs/test_2.txt temp_testcp/test_2.txt");
    assertTrue("get command for 1st file failed on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -get $TESTDIR/$testCpInputs/test_3.txt temp_testcp/test_3.txt");
    assertTrue("get command for 2nd file failed on HDFS", sh.getRet() == 0);
    sh.exec("diff temp_testcp/test_2.txt temp_testcp/test_3.txt");
    assertTrue("files differ in content", sh.getRet() == 0);
    sh.exec("rm -f temp_testcp/test_2.txt temp_testcp/test_3.txt");
    assertTrue("could not remove the files", sh.getRet() == 0);
  }
 
  @Test
  public void testCopyOverwriteFileInNewDirectory() {
    println("testCopyOverwriteFileInNewDirectory");
    //copy test_2.txt file to a newly created directory on hdfs
    sh.exec("hdfs dfs -mkdir $TESTDIR/temp_testcp");
    assertTrue("could not create directory on hdfs", sh.getRet() == 0);
    sh.exec("hdfs dfs -cp -f $TESTDIR/$testCpInputs/test_2.txt $TESTDIR/temp_testcp");
    assertTrue("copy command failed on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -get $TESTDIR/$testCpInputs/test_2.txt temp_testcp/test_2.txt");
    assertTrue("get command for 1st file failed on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -get $TESTDIR/temp_testcp/test_2.txt temp_testcp/test_3.txt");
    assertTrue("get command for 2nd file failed on HDFS", sh.getRet() == 0);
    sh.exec("diff temp_testcp/test_2.txt temp_testcp/test_3.txt");
    assertTrue("files differ in content", sh.getRet() == 0);
    sh.exec("rm -f temp_testcp/test_2.txt temp_testcp/test_3.txt");
    assertTrue("could not remove the files", sh.getRet() == 0);
  }
 
  @Test
  public void testCopyNonExistingFile() {
    println("testCopyNonExistingFile");
    //copy test_4.txt (non existing file) to another location on hdfs
    sh.exec("hdfs dfs -cp -f $TESTDIR/$testCpInputs/test_4.txt $TESTDIR/temp_testcp");
    assertTrue("copy command should not get executed for a non existing file on HDFS", sh.getRet() == 1);
    List err_msgs = sh.getErr();
    boolean failure= false;
    String failure_msg = "cp: `$TESTDIR/$testCpInputs/test_4.txt': No such file or directory";
    if (err_msgs.get(0).toString().contains(failure_msg)){
      failure = true;
    }
    assertTrue("copy command failed", failure == true);
  }

  @Test
  public void TestCpFileProtocolWithFile() {
    println("TestCpFileProtocolWithFile");
    //copy test_1.txt from local to a new creatde dir on hdfs
    sh.exec("hdfs dfs -mkdir $TESTDIR/temp_testcp_1");
    sh.exec("hdfs dfs -cp file:///$USERDIR/$testCpInputs/test_1.txt " +
            "$TESTDIR/temp_testcp_1");
    assertTrue("copy command from local to hdfs failed", sh.getRet() == 0);
    sh.exec("hdfs dfs -get $TESTDIR/temp_testcp_1/test_1.txt " +
            "temp_testcp/test_1.txt");
    assertTrue("get command for 1st file failed on HDFS", sh.getRet() == 0);
    sh.exec("diff temp_testcp/test_1.txt $testCpInputs/test_1.txt");
    assertTrue("files differ in content", sh.getRet() == 0);
    sh.exec("rm -f temp_testcp/test_1.txt");
    assertTrue("could not remove the files", sh.getRet() == 0);
    sh.exec("hdfs dfs -rm -skipTrash $TESTDIR/temp_testcp_1/test_1.txt");
    assertTrue("could not remove file from hdfs", sh.getRet() == 0);
  }
 
  @Test
  public void TestCpFileProtocolWithDirectory() {
    println("TestCpFileProtocolWithDirectory");
    //copy a dir from local to hdfs
    sh.exec("hdfs dfs -cp file:///$USERDIR/$testCpInputs $TESTDIR/test_temp_1");
    assertTrue("copy command failed on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -ls -R $TESTDIR/test_temp_1");
    assertTrue("listing files/directories failed on HDFS", sh.getRet() == 0);
    List out_msgs_fp = sh.getOut();
    Boolean success_fp_1= false;
    Boolean success_fp_2= false;
    Iterator out_iter_fp = out_msgs_fp.iterator();
    while (out_iter_fp.hasNext()) {
      String next_val = out_iter_fp.next();
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") &&
          next_val.contains("$TESTDIR/test_temp_1/test_2.txt")) {
        success_fp_1 = true;
      }
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") &&
          next_val.contains("$TESTDIR/test_temp_1/test_3"))  {
        success_fp_2 = true;
      }
    }
    assertTrue("Copied files do not match",
               success_fp_1 == true && success_fp_2 == true);
  }
}
