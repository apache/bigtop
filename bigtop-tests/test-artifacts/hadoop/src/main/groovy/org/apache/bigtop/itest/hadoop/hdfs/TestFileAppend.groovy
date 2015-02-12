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
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.conf.Configuration;

public class TestFileAppend {

  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");
  private static final String HADOOP_HOME = System.getenv('HADOOP_HOME');
  private static final String HADOOP_CONF_DIR = System.getenv('HADOOP_CONF_DIR');
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).replaceAll("\\s", "").replaceAll(":", "");
  private static String testAppendInput = "testAppendInput$date";
  private static String testAppendOutput = "testAppendOutput$date";
  private static String namenode;
  private static Configuration conf;

  @BeforeClass
  public static void setUp() {
    conf = new Configuration();
    namenode = conf.get("fs.defaultFS");
    if (namenode == null) {
      namenode = conf.get("fs.default.name");
    }
    assertTrue("Could not find namenode", namenode != null);

    // creating test directory and test files
    sh.exec("hadoop fs -mkdir $testAppendInput");
    sh.exec("echo \"-----TEST INPUT1-----\" > appendinput1.txt$date");
    sh.exec("echo \"-----TEST INPUT2-----\" > appendinput2.txt$date");
    sh.exec("echo \"-----TEST INPUT1-----\" > appendCorrect.txt$date");
    sh.exec("echo \"-----TEST INPUT2-----\" >> appendCorrect.txt$date");
    sh.exec("hadoop fs -put append* $testAppendInput");

    System.out.println("Running File Append Test:");

  }

  @AfterClass
  public static void tearDown() {
    // deletion of test folder
    sh.exec("hadoop fs -test -e $testAppendInput");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $testAppendInput");
      assertTrue("Deletion of previous testAppendInputs from HDFS failed",
        sh.getRet() == 0);
    }

  }

  @Test
  public void testAppendOnPreExistingFile() {
    FileSystem fs = FileSystem.get(conf);

    // setting paths for I/O stream creation
    String myInputPath = namenode + "/user/$USERNAME/$testAppendInput/appendinput2.txt$date";
    Path inFile = new Path(myInputPath);
    assertTrue("Input file not found", fs.exists(inFile));
    String myOutputPath = namenode + "/user/$USERNAME/$testAppendInput/appendinput1.txt$date";
    Path outFile = new Path(myOutputPath);
    assertTrue("Output file not found", fs.exists(outFile));

    FSDataInputStream input1 = fs.open(inFile);
    FSDataOutputStream output1 = fs.append(outFile);

    // append
    IOUtils.copyBytes(input1, output1, 4096, true);

    sh.exec("hadoop fs -cat $testAppendInput/appendinput1.txt$date > $testAppendOutput");
    sh.exec("diff $testAppendOutput appendCorrect.txt$date");
    assertTrue("Append did not work", sh.getRet() == 0);
    sh.exec("rm -rf appendinput1.txt$date", "rm -rf appendinput2.txt$date");

  }

  @Test
  public void testAppendOnCreatedFile() {
    FileSystem fs = FileSystem.get(conf);

    // setting paths for I/O stream creation
    String myOutputCreate = namenode + "/user/$USERNAME/$testAppendInput/appendinput3.txt$date";
    Path outCreate = new Path(myOutputCreate);
    FSDataOutputStream outputTemp = fs.create(outCreate);
    String myString = "-----TEST INPUT1-----\n";
    InputStream is = new ByteArrayInputStream(myString.getBytes());
    IOUtils.copyBytes(is, outputTemp, 4096, true);

    String myInputPath = namenode + "/user/$USERNAME/$testAppendInput/appendinput2.txt$date";
    Path inFile = new Path(myInputPath);
    assertTrue("Input file not found", fs.exists(inFile));
    String myOutputPath = namenode + "/user/$USERNAME/$testAppendInput/appendinput3.txt$date";
    Path outFile = new Path(myOutputPath);
    assertTrue("Output file not found", fs.exists(outFile));

    FSDataInputStream input1 = fs.open(inFile);
    FSDataOutputStream output1 = fs.append(outFile);

    //append
    IOUtils.copyBytes(input1, output1, 4096, true);

    sh.exec("hadoop fs -cat $testAppendInput/appendinput3.txt$date > $testAppendOutput");
    sh.exec("diff $testAppendOutput appendCorrect.txt$date");
    assertTrue("Append did not work", sh.getRet() == 0);
    sh.exec("rm -rf $testAppendOutput", "rm -rf appendinput1.txt$date", "rm -rf appendinput2.txt$date");
    sh.exec("rm -rf appendCorrect.txt$date");
    sh.exec("rm -rf appendinput3.txt$date");
  }


  @Test
  public void testAppendFilesGreaterThanBlockSize() {
    FileSystem fs = FileSystem.get(conf);

    // creating test files that exceed block size; putting them on hdfs
    sh.exec("dd if=/dev/urandom of=3mboutput.file$date count=3 bs=1048576");
    assertTrue("File creation error", sh.getRet() == 0);
    sh.exec("dd if=/dev/urandom of=3mbinput.file$date count=3 bs=1048576");
    assertTrue("File creation error", sh.getRet() == 0);
    sh.exec("hadoop fs -Ddfs.block.size=2097152 -put 3mb* $testAppendInput");
    assertTrue("Could not put test files onto hdfs", sh.getRet() == 0);
    sh.exec("cat 3mbinput.file$date >> 3mboutput.file$date");

    // setting paths for I/O stream creation    
    String myInputPath = namenode + "/user/$USERNAME/$testAppendInput/3mbinput.file$date";
    Path inFile = new Path(myInputPath);
    assertTrue("Input file not found", fs.exists(inFile));
    String myOutputPath = namenode + "/user/$USERNAME/$testAppendInput/3mboutput.file$date";
    Path outFile = new Path(myOutputPath);
    assertTrue("Output file not found", fs.exists(outFile));

    FSDataInputStream input1 = fs.open(inFile);
    FSDataOutputStream output1 = fs.append(outFile);

    // append
    IOUtils.copyBytes(input1, output1, 4096, true);

    sh.exec("hadoop fs -cat $testAppendInput/3mboutput.file$date > $testAppendOutput");
    sh.exec("diff $testAppendOutput 3mboutput.file$date");
    assertTrue("Append result is not what is expected", sh.getRet() == 0);
    sh.exec("rm -rf $testAppendOutput", "rm -rf 3mboutput.file$date", "rm -rf 3mbinput.file$date");
  }

  @Test
  public void testFsckSanity() {
    FileSystem fs = FileSystem.get(conf);

    // test file creation
    sh.exec("dd if=/dev/zero of=test1.file$date count=1 bs=1048576");
    assertTrue("File creation error", sh.getRet() == 0);
    sh.exec("dd if=/dev/zero of=test2.file$date count=1 bs=1048576");
    assertTrue("File creation error", sh.getRet() == 0);
    sh.exec("hadoop fs -put test1.file$date $testAppendInput", "hadoop fs -put test2.file$date $testAppendInput");
    assertTrue("Could not put test files onto hdfs", sh.getRet() == 0);

    // setting paths for I/O stream creation        
    String myInputPath = namenode + "/user/$USERNAME/$testAppendInput/test1.file$date";
    Path inFile = new Path(myInputPath);
    assertTrue("Input file not found", fs.exists(inFile));
    String myOutputPath = namenode + "/user/$USERNAME/$testAppendInput/test2.file$date";
    Path outFile = new Path(myOutputPath);
    assertTrue("Output file not found", fs.exists(outFile));

    FSDataInputStream input1 = fs.open(inFile);
    FSDataOutputStream output1 = fs.append(outFile);

    // append
    IOUtils.copyBytes(input1, output1, 4096, true);

    // running fsck
    shHDFS.exec("hadoop fsck /user/$USERNAME/$testAppendInput/test2.file$date");
    Boolean success = shHDFS.getOut().get(shHDFS.getOut().size() - 1).contains("is HEALTHY"); ;
    assertTrue("Append made file unhealthy", success == true);

    sh.exec("rm -rf test1.file$date", "rm -rf test2.file$date");
  }

  @Test
  public void testMultipleOutputStreamFailure() {
    // create 2 separate clients
    conf.setBoolean("fs.hdfs.impl.disable.cache", true);
    FileSystem fs = FileSystem.get(conf);
    FileSystem fs2 = FileSystem.get(conf);
    conf.setBoolean("fs.hdfs.impl.disable.cache", false);

    // test file creation
    sh.exec("dd if=/dev/zero of=test3.file$date count=1 bs=1048576");
    assertTrue("File creation error", sh.getRet() == 0);
    sh.exec("dd if=/dev/zero of=test4.file$date count=1 bs=1048576");
    assertTrue("File creation error", sh.getRet() == 0);
    sh.exec("hadoop fs -put test3.file$date $testAppendInput", "hadoop fs -put test4.file$date $testAppendInput");
    assertTrue("Could not put test files onto hdfs", sh.getRet() == 0);

    // setting paths for I/O stream creation        
    String myInputPath = namenode + "/user/$USERNAME/$testAppendInput/test3.file$date";
    Path inFile = new Path(myInputPath);
    assertTrue("Input file not found", fs.exists(inFile));
    String myOutputPath = namenode + "/user/$USERNAME/$testAppendInput/test4.file$date";
    Path outFile = new Path(myOutputPath);
    assertTrue("Output file not found", fs.exists(outFile));

    FSDataInputStream input1 = fs.open(inFile);
    FSDataOutputStream output1 = fs.append(outFile);

    // append
    IOUtils.copyBytes(input1, output1, 4096, false);

    // attempting second output stream
    try {
      FSDataOutputStream output2 = fs2.append(outFile);
      assertTrue("Should not have been able to open second output stream", false);
      IOUtils.closeStream(output2);
    }
    catch (Exception e) {
    }

    // attempting second output stream after first stream is closed
    IOUtils.closeStream(output1);
    FSDataOutputStream output3 = fs.append(outFile);

    IOUtils.closeStream(output3);
    IOUtils.closeStream(input1);
    sh.exec("rm -rf test3.file$date", "rm -rf test4.file$date");
    assertTrue("Could not remove test files", sh.getRet() == 0);

  }

}


