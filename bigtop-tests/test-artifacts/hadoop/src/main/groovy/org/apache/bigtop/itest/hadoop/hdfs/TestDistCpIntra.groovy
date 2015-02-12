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


public class TestDistCpIntra {

  private static Shell sh = new Shell("/bin/bash -s");
  //extracting user identity for distcp absolute path
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).replaceAll("\\s", "").replaceAll(":", "");
  private static String namenode = "";
  private static String testDistcpInputs = "testDistcpInputs" + date;
  private static String testDistcpOutputs = "testDistcpOutputs" + date;
  private static String dcpfile = "dcpfile" + date;
  private static String testDistcpIn = "testDistcpIn" + date;
  private static String testDistcpOut = "testDistcpOut" + date;

  @BeforeClass
  public static void setUp() {
    // get namenode hostname from core-site.xml
    Configuration conf = new Configuration();
    namenode = conf.get("fs.defaultFS");
    if (namenode == null) {
      namenode = conf.get("fs.default.name");
    }
    assertTrue("Could not find namenode", namenode != null);

    sh.exec("hadoop fs -mkdir $testDistcpInputs");
    assertTrue("Could not create input directory", sh.getRet() == 0);

    for (int i = 4; i <= 7; i++) {
      sh.exec("hadoop fs -mkdir $testDistcpInputs$i");
      assertTrue("Could not create input directory", sh.getRet() == 0);
    }

    sh.exec("hadoop fs -mkdir $testDistcpOutputs");
    assertTrue("Could not create output directory", sh.getRet() == 0);

    // create sample input files
    for (int i = 1; i <= 2; i++) {
      String dcpfile_i = "$dcpfile" + "$i" + ".txt";
      sh.exec("echo \"test$i\" > $dcpfile_i");
    }

    // copy sample input files to hdfs
    sh.exec("hadoop fs -put $dcpfile* $testDistcpInputs");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    // create and copy sample input files - multiple sources , source file
    for (int i = 4; i <= 7; i++) {
      String dcpfile_i = "$dcpfile" + "$i" + ".txt";
      sh.exec("echo \"test$i\" > $dcpfile_i");
      sh.exec("hadoop fs -put $dcpfile_i $testDistcpInputs$i");
      assertTrue("Could not copy file to HDFS", sh.getRet() == 0);
    }

    // do clean up of local dcpfiles 
    sh.exec("rm -rf $dcpfile*");

    System.out.println("Running Distcp:");
  }

  @AfterClass
  public static void tearDown() {
    // clean up of existing folders
    sh.exec("hadoop fs -test -e $testDistcpInputs");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $testDistcpInputs");
      assertTrue("Deletion of previous testDistcpInputs from HDFS failed",
        sh.getRet() == 0);
    }

    for (int i = 4; i <= 7; i++) {
      sh.exec("hadoop fs -test -e $testDistcpInputs$i");
      if (sh.getRet() == 0) {
        sh.exec("hadoop fs -rmr -skipTrash $testDistcpInputs$i");
        assertTrue("Deletion of previous testDistcpInputs from HDFS failed",
          sh.getRet() == 0);
      }
    }

    sh.exec("hadoop fs -test -e $testDistcpOutputs");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $testDistcpOutputs");
      assertTrue("Deletion of previous testDistcpOutputs from HDFS failed",
        sh.getRet() == 0);
    }

  }

  @Test
  public void testDistcpIntra() {
    for (int i = 1; i <= 2; i++) {
      String dcpfile_i = "$dcpfile" + "$i" + ".txt";
      // running distcp from namenode/src to namenode/dest
      sh.exec("hadoop distcp $namenode/user/$USERNAME/$testDistcpInputs/$dcpfile_i $namenode/user/$USERNAME/$testDistcpOutputs");
      assertTrue("Distcp $i failed", sh.getRet() == 0);

      // confirm that copied file is the same as original file
      sh.exec("hadoop fs -cat $namenode/user/$USERNAME/$testDistcpInputs/$dcpfile_i > $testDistcpIn");
      sh.exec("hadoop fs -cat $namenode/user/$USERNAME/$testDistcpOutputs/$dcpfile_i > $testDistcpOut");
      sh.exec("if diff $testDistcpIn $testDistcpOut >/dev/null; then echo \"success\"; else echo \"failure\"; fi");
      assertTrue("Files corrupted while being copied", sh.getOut().get(0) == "success");

      // clean up
      sh.exec("rm -rf $testDistcpIn", "rm -rf $testDistcpOut");
    }
  }

  @Test
  public void testDistcpIntra_MultipleSources() {
    String distcp_sources = "distcp_sources" + date;
    String dcpfile4 = "$testDistcpInputs" + "4/$dcpfile" + "4.txt"
    String dcpfile5 = "$testDistcpInputs" + "5/$dcpfile" + "5.txt"
    String dcpfile6 = "$testDistcpInputs" + "6/$dcpfile" + "6.txt"
    String dcpfile7 = "$testDistcpInputs" + "7/$dcpfile" + "7.txt"
    // distcp mulitple sources
    sh.exec("hadoop distcp $namenode/user/$USERNAME/$dcpfile4 $namenode/user/$USERNAME/$dcpfile5 $namenode/user/$USERNAME/$testDistcpOutputs");
    assertTrue("Distcp multiple sources failed", sh.getRet() == 0);

    // distcp source file (-f option)
    sh.exec("echo \"$namenode/user/$USERNAME/$dcpfile6\" > $distcp_sources", "echo \"$namenode/user/$USERNAME/$dcpfile7\" >> $distcp_sources");
    sh.exec("hadoop fs -put $distcp_sources $namenode/user/$USERNAME/$testDistcpInputs");
    sh.exec("rm -rf $distcp_sources");
    sh.exec("hadoop distcp -f $namenode/user/$USERNAME/$testDistcpInputs/$distcp_sources $namenode/user/$USERNAME/$testDistcpOutputs");
    assertTrue("Distcp with a source file failed", sh.getRet() == 0);

    // confirm that copied files are the same as original files for multiple sources and source file
    for (int i = 4; i <= 7; i++) {
      String dcpfile_i = "$dcpfile" + "$i" + ".txt";
      sh.exec("hadoop fs -cat $namenode/user/$USERNAME/$testDistcpInputs$i/$dcpfile_i > $testDistcpIn");
      sh.exec("hadoop fs -cat $namenode/user/$USERNAME/$testDistcpOutputs/$dcpfile_i > $testDistcpOut");
      sh.exec("if diff $testDistcpIn $testDistcpOut >/dev/null; then echo \"success\"; else echo \"failure\"; fi");
      assertTrue("Files corrupted while being copied", sh.getOut().get(0) == "success");
      // clean up
      sh.exec("rm -rf $testDistcpIn", "rm -rf $testDistcpOut");
    }

  }

}

