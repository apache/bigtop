/*Licensed to the Apache Software Foundation (ASF) under one
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

public class TestCmdText {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for ls absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testCmdTextInputDir = "testCmdTextInputDir" + date;
  private static String testCmdTextInputs = "test_data_TestCmdText"
  private static String testCmdTextOut = "testCmdTextOut" + date;
  private static String testCmdTextOutCmp = "testCmdTextOutCmp" + date;
  private static int repfactor = 2;
  private static String TESTDIR  = "/user/$USERNAME/$testCmdTextInputDir";
  private static String user_testinputdir  = USERNAME+"/"+testCmdTextInputDir+
                                             "/"+testCmdTextInputs

  @BeforeClass
  public static void setUp() {

    // unpack resource
    JarContent.unpackJarContainer(TestCmdText.class, "." , null);

    sh.exec("cp -r test_data test_data_TestCmdText");
    assertTrue("Could not copy data into test_data_TestCmdText",
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
      println("hdfs dfs -rm -r -skipTrash $TESTDIR")
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("hdfs dfs -mkdir -p $TESTDIR");
    assertTrue("Could not create input directory on HDFS", sh.getRet() == 0);

    // copy input directory to hdfs
    println("hdfs dfs -put $testCmdTextInputs $TESTDIR");
    sh.exec("hdfs dfs -put $testCmdTextInputs $TESTDIR");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    println("Running cmdtext:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d $TESTDIR");
    if (sh.getRet() == 0) {
      //  println("hdfs dfs -rm -r -skipTrash $TESTDIR")
      sh.exec("hdfs dfs -rm -r -skipTrash $TESTDIR");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -f $testCmdTextOut");
    if (sh.getRet() == 0) {
      // println("rm -rf $testCmdTextOut")
      sh.exec("rm -rf $testCmdTextOut from local disk");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -f $testCmdTextOutCmp");
    if (sh.getRet() == 0) {
      // println("rm -rf $testCmdTextOutCmp")
      sh.exec("rm -rf $testCmdTextOutCmp");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }

    sh.exec("test -d $testCmdTextInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testCmdTextInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testCmdTextSingleFile() {
    println("testCmdTextSingleFile");
    // invoke text with single file
    sh.exec("hdfs dfs -text $TESTDIR/$testCmdTextInputs/test_1.txt | " +
            "sed '/INFO/'d &> $testCmdTextOut");
    assertTrue("text command on HDFS failed", sh.getRet() == 0);
    sh.exec("diff $testCmdTextInputs/test_1.txt $testCmdTextOut");
    assertTrue("Input provided and output returned are different",
               sh.getRet() == 0);
  }
 
  @Test
  public void testCmdTextMultipleFiles() {
    println("testCmdTextMultipleFiles");
    // invoke text with 2 files
    sh.exec("hdfs dfs -text $TESTDIR/$testCmdTextInputs/test_1.txt " +
            "$TESTDIR/$testCmdTextInputs/test_3 | " +
            "sed '/INFO/'d &> $testCmdTextOut");
    assertTrue("text command for multiple sources on HDFS failed",
               sh.getRet() == 0);

    sh.exec("cat $testCmdTextInputs/test_1.txt " +
            "$testCmdTextInputs/test_3 &> " +
            "$testCmdTextOutCmp");
    assertTrue("cannot cat both the files", sh.getRet() == 0);
    sh.exec("diff $testCmdTextOutCmp $testCmdTextOut");
    assertTrue("Input provided and output returned are different",
               sh.getRet() == 0);
  }

  @Test
  public void testCmdWithZipFile() {
    println("testCmdWithZipFile");
    // invoke -text with zip file
    sh.exec("hdfs dfs -text $TESTDIR/$testCmdTextInputs/test.zip | " +
            "sed '/INFO/'d &> $testCmdTextOut");
    assertTrue("text command for zip on HDFS failed", sh.getRet() == 0);

    sh.exec("diff $testCmdTextInputs/test.zip $testCmdTextOut");
    assertTrue("Input provided and output returned are different",
               sh.getRet() == 0);
  }
 
  @Test
  public void testCmdWithNonExistenZipFile() {
    println("testCmdWithNonExistenZipFile");
    sh.exec("hdfs dfs -text $TESTDIR/$testCmdTextInputs/test1.zip");
    assertTrue("text command for non-created zip on HDFS executed successfully",
               sh.getRet() == 1);

    String searchStr = "text: `$TESTDIR/$testCmdTextInputs/test1.zip': No such file or directory";
    assertTrue("expected pattern not found in the output file",
               lookForGivenString(sh.getErr(), searchStr) == true);
  }

  @Test
  public void testCmdWithDirectory() {
    println("testCmdWithDirectory");
    sh.exec("hdfs dfs -text $TESTDIR/$testCmdTextInputs");
    assertTrue("text command for existing directory on HDFS executed successfully",
               sh.getRet() == 1);

    String searchStr = "text: `/user/"+user_testinputdir+".: Is a directory";
    assertTrue("expected pattern not found in the output file",
               lookForGivenString(sh.getErr(), searchStr) == false);
  }

  /**
   * lookForGivenString check the given
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
