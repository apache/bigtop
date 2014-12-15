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
package org.apache.bigtop.itest.hadoop.hdfs

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.hadoop.hdfs.HdfsConfiguration
import org.junit.Assume

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.hadoop.hdfs.DFSTestUtil;


/**
 This test checks block recovery after a block is corrupted.
 The test must be performed on a cluster with at least
 three datanodes to allow block recovery.
 The test must be run under user hdfs.
 Block replication must be set to a minimum value of 2
 for this test to work properly.
 */
public class TestBlockRecovery {

  private static Shell sh = new Shell("/bin/bash");

  private static Configuration conf;

  private static final String corruptContent = "0123456789";
  private static final String fsFilePath = USER_DIR + "/file0";
  private static final String grepIP = "grep -o '\\[[^]]*\\]' | " +
    "grep -o '[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*'";
  private static final String localTestDir = "/tmp/test";
  private static final String outputFile = localTestDir + "/fsckOutput.txt";
  private static final String USER_DIR = "/user/hdfs";

  private static final int sleepTime = 60 * 1000;
  private static final int TIMEOUT = 5000;

  private static String blockToTest;
  private static String blockLocation;
  private static String blockRecoveryNode;
  private static String cksumError;
  private static String initialBlockChecksum;
  private static String fileContent;
  private static String USERNAME;

  private static def dataDirs = [];
  private static def nodesBeforeRecovery = [];
  private static def nodesAfterRecovery = [];

  private static short numberOfDataNodes;
  private static short repFactor;

  private static final long fileLen = 10;
  private static final long SEED = 0;

  @BeforeClass
  public static void setUp() {
    /* Find datanode data directory, make file, add content, ensure replication
     * is set to guarantee any chosen datanode will have block,
     * get block and its location, perform checksum before corrupting block
     * -- all on client side
     */
    conf = new HdfsConfiguration();
    FileSystem fileSys = DistributedFileSystem.get(conf);
    conf.addResource("hdfs-site.xml");
    dataDirs = conf.get("dfs.data.dir").split(",");
    if (dataDirs == null)
      dataDirs = conf.get("dfs.datanode.data.dir").split(",");

    USERNAME = System.getProperty("user.name");
    Assume.assumeTrue(USERNAME == "hdfs");

    numberOfDataNodes = sh.exec("hdfs dfsadmin -report | grep ^Name | wc -l").getOut()[0] as short;
    Assume.assumeTrue(numberOfDataNodes >= 3);

    sh.exec("rm -rf $localTestDir");
    sh.exec("mkdir $localTestDir");
    sh.exec("hadoop fs -rm -r $fsFilePath");
    Thread.sleep(TIMEOUT);
    sh.exec("hadoop fs -mkdir -p $USER_DIR");
    assertTrue("Failed to create input directory", sh.getRet() == 0);

    repFactor = (numberOfDataNodes - 1);

    DFSTestUtil.createFile(fileSys, new Path(fsFilePath), fileLen, repFactor, SEED);

    fileContent = sh.exec("hadoop fs -cat $fsFilePath").getOut()[0];

    sh.exec("hdfs fsck $fsFilePath -blocks -locations -files > $outputFile");
    assertTrue("Could not write output to file", sh.getRet() == 0);

    nodesBeforeRecovery = sh.exec("grep -o '\\[[^]]*\\]' $outputFile | " +
      "grep -o '[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*'").getOut();
    assertTrue("Could not obtain datanode addresses", sh.getRet() == 0);

    blockToTest = sh.exec("grep -o 'blk_[0-9]*' $outputFile").getOut()[0];
    assertTrue("Could not obtain block number", sh.getRet() == 0);

    for (int i=0; i < dataDirs.length; i++) {
      def dataDir = dataDirs[i]
      blockLocation = sh.exec("find $dataDir -name $blockToTest | grep $dataDir").getOut()[0];
      if (blockLocation != null) break;
    }
    assertNotNull("Could not find specified block", blockLocation);

    initialBlockChecksum = sh.exec("cksum $blockLocation").getOut()[0].split(" ")[0];
    assertTrue("Could not obtain checksum for block $blockToTest", sh.getRet() == 0);
  }

  @AfterClass
  public static void tearDown() {
    // deletion of test files
    sh.exec("hadoop fs -rm -r -skipTrash $fsFilePath");
    assertTrue("Could not delete file $fsFilePath", sh.getRet() == 0);
    sh.exec("rm -rf $localTestDir");
    assertTrue("Could not delete test directory $localTestDir", sh.getRet() == 0);
    }

  @Test
  public void testBlockRecovery() {
    // corrupt block
    sh.exec("echo $corruptContent > $blockLocation");
    assertTrue("Could not write to file", sh.getRet() == 0);

    // perform checksum after block corruption
    String corruptBlockChecksum = sh.exec("cksum $blockLocation").getOut()[0].split(" ")[0];
    assertTrue("Could not obtain checksum for block $blockToTest", sh.getRet() == 0);

    // trigger block recovery by trying to access the file
    sh.exec("hadoop fs -cat $fsFilePath");

    // make sure checksum changes back to original, indicating block recovery
    for (int j=0; j<3; j++) {
      // wait a bit to let the block recover
      sleep(sleepTime);
      // see if checksum has changed
      cksumError = sh.exec("hadoop fs -cat $fsFilePath | grep -o 'Checksum error'").getErr();
      if (cksumError != "Checksum error") break;
    }
    assertNotNull ("Block has not been successfully triggered for recovery.", cksumError);

    nodesAfterRecovery = sh.exec("hdfs fsck $fsFilePath -blocks -locations -files | $grepIP").getOut();
    assertTrue("Could not obtain datanode addresses", sh.getRet() == 0);

    blockRecoveryNode = (nodesBeforeRecovery.intersect(nodesAfterRecovery))[0];

    if (blockRecoveryNode == null) {
      sleep(sleepTime);

      nodesAfterRecovery = sh.exec("hdfs fsck $fsFilePath -blocks -locations -files | $grepIP").getOut();
      assertTrue("Could not obtain datanode addresses", sh.getRet() == 0);

      blockRecoveryNode = (nodesBeforeRecovery.intersect(nodesAfterRecovery))[0];
      assert (blockRecoveryNode.size() != 0) : "Block has not been successfully triggered for recovery."
    }

    int cksumAttempt;

    boolean success = false;

    // verify block has recovered. If not, give it a few more tries
    while (cksumAttempt < 3) {
      if (corruptBlockChecksum != initialBlockChecksum) {
        sleep(sleepTime);
        corruptBlockChecksum = sh.exec("ssh -o StrictHostKeyChecking=no -i ~/.ssh/id_hdfsuser " +
          "$blockRecoveryNode 'cksum `find ${dataDirs.join(' ')}" +
          " -name $blockToTest 2>/dev/null | grep $blockToTest` '").getOut()[0].split(" ")[0];
        ++cksumAttempt;
      } else {
        // If block recovers, verify file content is the same as before corruption
        if (sh.exec("hadoop fs -cat $fsFilePath").getOut()[0] == fileContent) {
          assertTrue("Could not read file $fsFilePath", sh.getRet() == 0);
          success = true;
          break;
        }
      }
    }
    assertTrue("Block has not recovered", success);
  }

}

