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

public class TestMv {
  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static final String USERDIR = System.getProperty("user.dir");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testMvInputDir = "testMvInputDir" + date;
  private static String testMvInputs = "test_data_TestMv"
  private static String testMvOut = "testMvOut" + date;
  private static String testMvOutCmp = "testMvOutCmp" + date;
  private static String TESTDIR  = "/user/$USERNAME/$testMvInputDir";

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestMv.class, "." , null);
    sh.exec("cp -r test_data test_data_TestMv");
    assertTrue("Could not copy data into test_data_TestMv", sh.getRet() == 0);

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
    sh.exec("hdfs dfs -put $testMvInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    // set the replication if file exists
    sh.exec("hdfs dfs -test -f $TESTDIR/$testMvInputs/test_2.txt");
    assertTrue("Could not find files on HDFS", sh.getRet() == 0);

    sh.exec("test -d temp_testmv");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf temp_testmv");
    }
    sh.exec("mkdir temp_testmv");
    assertTrue("could not create a dir", sh.getRet() == 0);

    println("Running mv:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testMvOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testMvOut");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
    sh.exec("test -f $testMvOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testMvOutCmp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
    sh.exec("test -d temp_testmv");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf temp_testmv");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testMvInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testMvInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

  }

  @Test
  public void testMvFile() {
    println("testMvFile");
    //mv file from one hdfs location to other
    sh.exec("hdfs dfs -mv $TESTDIR/$testMvInputs/test_1.txt $TESTDIR");
    assertTrue("mv command failed", sh.getRet() == 0);

    sh.exec("hdfs dfs -ls $TESTDIR/$testMvInputs/test_1.txt");
    assertTrue("Able to find original file?", sh.getRet() == 1);

    sh.exec("hdfs dfs -ls $TESTDIR/test_1.txt");
    assertTrue("Able to find file in moved location?", sh.getRet() == 0);

    // Now verify the moved file data
    sh.exec("hdfs dfs -get $TESTDIR/test_1.txt temp_testmv/test_1.txt");
    assertTrue("get command failed", sh.getRet() == 0);
    sh.exec("diff temp_testmv/test_1.txt test_data/test_1.txt");
    assertTrue("files differ", sh.getRet() == 0);
  }

  @Test
  public void testMvDirectory() {
    println("testMvDirectory");
    //mv dir from one hdfs location to other
    sh.exec("hdfs dfs -mv $TESTDIR/$testMvInputs $TESTDIR/test_moved");
    assertTrue("mv command failed", sh.getRet() == 0);

    // check that original contents are no longer exists
    sh.exec("hdfs dfs -ls $TESTDIR/$testMvInputs");
    assertTrue("Able to list deleted files?", sh.getRet() == 1);

    // check the moved location
    sh.exec("hdfs dfs -ls -R $TESTDIR/test_moved");
    assertTrue("Able to list moved files?", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1= false;
    Boolean success_2= false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") &&
          next_val.contains("$TESTDIR/test_moved/test_2.txt")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("-rw-r--r--") && next_val.contains("$USERNAME") &&
          next_val.contains("$TESTDIR/test_moved/test.zip"))  {
        success_2 = true;
        continue;
      }
    }
    assertTrue("Does the moved files details are correct?",
               success_1 == true && success_2 == true);

    // now move back the files
    sh.exec("hdfs dfs -mv $TESTDIR/test_moved $TESTDIR/$testMvInputs");
    assertTrue("mv command failed", sh.getRet() == 0);
  }
 
  @Test
  public void testMvMultipleFiles() {
    println("testMvMultipleFiles");
    sh.exec("hdfs dfs -test -d $TESTDIR/test_moved");
    if (sh.getRet() == 0)
    {
      sh.exec("hdfs dfs -rm -r $TESTDIR/test_moved");
      assertTrue("Able to clear contents of dir $TESTDIR/test_moved?",
                 sh.getRet() == 0);
    }
    //mv multiple files from one hdfs location to other
    sh.exec("hdfs dfs -mkdir $TESTDIR/test_moved");
    assertTrue("could not create a dir", sh.getRet() == 0);
    sh.exec("hdfs dfs -mv $TESTDIR/$testMvInputs/test_2.txt " +
            "$TESTDIR/$testMvInputs/test.zip $TESTDIR/test_moved");
    assertTrue("mv command failed", sh.getRet() == 0);

    // check that files in old location got deleted
    sh.exec("hdfs dfs -ls $TESTDIR/$testMvInputs/test_2.txt");
    assertTrue("Does $TESTDIR/$testMvInputs/test_2.txt not moved?", sh.getRet() == 1);
    sh.exec("hdfs dfs -ls $TESTDIR/$testMvInputs/test.zip");
    assertTrue("Does $TESTDIR/$testMvInputs/test.zip not moved?", sh.getRet() == 1);

    // check that files present in new location
    sh.exec("hdfs dfs -ls $TESTDIR/test_moved/test_2.txt");
    assertTrue("Is $TESTDIR/test_moved/test_2.txt not present?",
               sh.getRet() == 0);
    sh.exec("hdfs dfs -ls $TESTDIR/test_moved/test.zip");
    assertTrue("Is $TESTDIR/test_moved/test_2.txt not present?",
               sh.getRet() == 0);

    // verify the moved files data
    sh.exec("hdfs dfs -get $TESTDIR/test_moved/test.zip temp_testmv");
    assertTrue("get command failed", sh.getRet() == 0);
    sh.exec("diff temp_testmv/test.zip test_data/test.zip");
    assertTrue("files differ", sh.getRet() == 0);

    // Now move back files
    sh.exec("hdfs dfs -mv $TESTDIR/test_moved/test_2.txt " +
            "$TESTDIR/test_moved/test.zip $TESTDIR/$testMvInputs");
    assertTrue("mv command failed", sh.getRet() == 0);
  }

  @Test
  public void testMvWithOutProperInputs() {
    println("testMvWithOutProperInputs");
    //mv files from one hdfs, but dont provide the destination
    sh.exec("hdfs dfs -mv $TESTDIR/$testMvInputs/test_3");
    assertTrue("mv command failed", sh.getRet() == 255);

    List err_msgs = sh.getErr();
    Boolean failure= false;
    String failure_msg = "-mv: Not enough arguments: expected 2 but got 1";
    if (err_msgs.get(0).toString().contains(failure_msg)){
      failure = true;
    }
    assertTrue("get command failed", failure == true);
  }

  @Test
  public void testMvNonexistingFile() {
    println("testMvNonexistingFile");
    //mv non existing file from one hdfs to other
    sh.exec("hdfs dfs -mv $TESTDIR/$testMvInputs/test_13.txt $TESTDIR/temp_moved");
    assertTrue("mv command failed", sh.getRet() == 1);

    List err_msgs = sh.getErr();
    boolean failure= false;
    String failure_msg = "mv: `$TESTDIR/$testMvInputs/test_13.txt': " +
                         "No such file or directory";
    if (err_msgs.get(0).toString().contains(failure_msg)){
      failure = true;
    }
    assertTrue("Does mv returned proper error message for non existing file?",
               failure == true);
  }

  @Test
  public void testMvProtocol() {
    println("testMvProtocol");
    //mv files from one hdfs location to a local location
    sh.exec("hdfs dfs -mv $TESTDIR/$testMvInputs/test_2.txt file://$USERDIR");
    assertTrue("mv command failed", sh.getRet() == 1);
    List err_msgs = sh.getErr();
    Boolean failure= false;
    String failure_msg = "mv: `$TESTDIR/$testMvInputs/test_2.txt': " +
                         "Does not match target filesystem";
    if (err_msgs.get(0).toString().contains(failure_msg)){
      failure = true;
    }
    assertTrue("get command failed", failure == true);

    //mv dir from one local location to another location
    sh.exec("hdfs dfs -mv file://$USERDIR/$testMvInputs/ " +
            "file://$USERDIR/temp_testmv_1");
    assertTrue("mv command failed", sh.getRet() == 0);

    sh.exec("ls $testMvInputs");
    assertTrue("Does mv source files still present?", sh.getRet() == 2);

    sh.exec("ls temp_testmv_1");
    assertTrue("listing file failed", sh.getRet() == 0);

    //revert the changes
    sh.exec("hdfs dfs -mv file://$USERDIR/temp_testmv_1 " +
            "file://$USERDIR/$testMvInputs");
    assertTrue("mv command failed", sh.getRet() == 0);
  }
}

