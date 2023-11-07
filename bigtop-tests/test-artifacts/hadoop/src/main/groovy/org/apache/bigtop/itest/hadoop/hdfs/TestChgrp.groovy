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

public class TestChgrp {

  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell shHDFS = new Shell("/bin/bash -s","hdfs");
  // extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static final String USERDIR = System.getProperty("user.dir");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testChgrpInputDir = "testChgrpInputDir" + date;
  private static String testChgrpInputs = "test_data_TestChgrp"
  private static String testChgrpOut = "testChgrpOut" + date;
  private static String testChgrpOutCmp = "testChgrpOutCmp" + date;
  private static String user_testinputdir = USERNAME+"/"+testChgrpInputDir+
                                             "/"+testChgrpInputs;
  private static String TESTDIR = "/user/$USERNAME/$testChgrpInputDir";
  static List<String> TestChgrp_output = new ArrayList<String>();
  static List<String> TestChgrp_error = new ArrayList<String>();
  static boolean result = false;

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestChgrp.class, "." , null);

    sh.exec("cp -r test_data test_data_TestChgrp");
    assertTrue("Could not copy data into test_data_TestChgrp", sh.getRet() == 0);

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
    assertTrue("Could not create input directory on HDFS",
               sh.getRet() == 0);

    // copy input directory to hdfs
    sh.exec("hdfs dfs -put $testChgrpInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    // set the replication if file exists
    sh.exec("hdfs dfs -test -f $TESTDIR/$testChgrpInputs/test_2.txt");
    assertTrue("Could not find files on HDFS", sh.getRet() == 0);
    sh.exec("hdfs dfs -chmod -R o+w $TESTDIR/$testChgrpInputs");
    logError(sh);
    assertTrue("Could not change permissions", sh.getRet() == 0);

    println("Running chgrp:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      println("hdfs dfs -rm -r -skipTrash $TESTDIR")
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory",
                 sh.getRet() == 0);
    }

    sh.exec("test -f $testChgrpOut");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testChgrpOut from local disk");
      assertTrue("Could not remove output directory/file",
                 sh.getRet() == 0);
    }

    sh.exec("test -f $testChgrpOutCmp");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testChgrpOutCmp");
      assertTrue("Could not remove output directory/file",
                 sh.getRet() == 0);
    }

    sh.exec("test -d $testChgrpInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testChgrpInputs");
      assertTrue("Could not remove output directory/file",
                 sh.getRet() == 0);
    }
  }

  @Test
  public void testChgrpWithOutgroupName() {
    println("testChgrpWithOutgroupName");
    sh.exec("hdfs dfs -chgrp $TESTDIR/$testChgrpInputs ");
    assertTrue("chgrp command executed successfully without group name",
               sh.getRet() == 255);

    String searchStr = "chgrp: Not enough arguments: expected 2 but got 1";
    assertTrue("expected pattern not found in the output file ",
               lookForGivenString(sh.getErr(),searchStr) == true);
  }

  @Test
  public void testChgrpWithWithInvalidPath() {
    println("testChgrpWithWithInvalidPath");
    sh.exec("hdfs dfs -chgrp $TESTDIR/$testChgrpInputs random");
    assertTrue("Does chgrp worked with wrong path?", sh.getRet() == 1);

    String searchStr = "chgrp: `random': No such file or directory";
    assertTrue("expected pattern not found in the output file ",
               lookForGivenString(sh.getErr(),searchStr) == true);
  }

  @Test
  public void testChgrpWithWithInvalidGroupName() {
    sh.exec("hdfs dfs -chgrp random $TESTDIR/$testChgrpInputs ");
    assertTrue("Does chgrp worked with wrong group?", sh.getRet() == 1);

    String searchStr = "chgrp: changing ownership of '/user/"+
                       user_testinputdir+
                       "': User does not belong to random";
    assertTrue("expected pattern not found in the output file ",
               lookForGivenString(sh.getErr(),searchStr) == true);
  }

  @Test
  public void testChgrp() {
    println("testChgrp");
    sh.exec("id | awk \'{print \$3}\' | awk -F\'[()]\' \'{print \$2}\'");
    List out_grp = sh.getOut();
    String group_name = out_grp.get(0);

    // first make sure that all the files in the directory belongs to a group
    sh.exec("hdfs dfs -chgrp -R $group_name $TESTDIR");
    assertTrue("chgrp command failed on HDFS", sh.getRet() == 0);

    sh.exec("hdfs dfs -ls $TESTDIR");
    assertTrue("Able to list files?", sh.getRet() == 0);

    String searchString = "$USERNAME $group_name";
    assertTrue("chgrp failed for a proper group name",
                lookForGivenString(sh.getOut(), searchString) == true);
 
    shHDFS.exec("hdfs dfs -chgrp hdfs $TESTDIR/$testChgrpInputs");
    assertTrue("chgrp command failed with hdfs user on HDFS",
               shHDFS.getRet() == 0);

    shHDFS.exec("hdfs dfs -ls $TESTDIR");
    assertTrue("Able to list files?", shHDFS.getRet() == 0);

    // check that parent for directory only group name got changed

    searchString = "$USERNAME hdfs";
    assertTrue("chgrp applied only to parent directory?",
               lookForGivenString(shHDFS.getOut(), searchString) == true);

    // check that chgrp does not applied to files inside the directory
    shHDFS.exec("hdfs dfs -ls -R $TESTDIR/$testChgrpInputs");
    searchString = "$USERNAME hdfs";
    assertTrue("chgrp does not applied to files inside the directory?",
               lookForGivenString(shHDFS.getOut(), searchString) == false);

    // now change the group recursively
    shHDFS.exec("hdfs dfs -chgrp -R hdfs $TESTDIR/$testChgrpInputs");
    assertTrue("chgrp command with hdfs user failed on HDFS",
               shHDFS.getRet() == 0);

    shHDFS.exec("hdfs dfs -ls -R $TESTDIR");
    assertTrue("listing directories failed", shHDFS.getRet() == 0);

    searchString = "$USERNAME $group_name";
    assertTrue("chgrp failed to execute recursively on directory",
               lookForGivenString(shHDFS.getOut(), searchString) == false);
  }

  /**
   * lookForGivenString check the given string is present in the list data
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

