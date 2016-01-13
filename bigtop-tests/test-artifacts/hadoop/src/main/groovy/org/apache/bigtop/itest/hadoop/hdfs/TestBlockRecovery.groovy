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
 Block replication must be set to a minimum value of 2
 for this test to work properly.

 If passwordless setup isn't configured for user HDFS the test
 will be skipped as well.
 */
public class TestBlockRecovery {

  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");

  private static Configuration conf;

  private static final String SSH_HDFS_ID = "~/.ssh/id_hdfsuser"
  private static final String corruptContent = "0123456789";
  private static final String HDFS_TEST_DIR = "/tmp/TestBlockRecovery$corruptContent";
  private static final String fsFilePath = HDFS_TEST_DIR + "/file0";
  private static final String grepIP = "grep -o '\\[[^]]*\\]' | " +
    "grep -o '[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*'";
  private static final String localTestDir = "/tmp/test";
  private static final String outputFile = localTestDir + "/fsckOutput.txt";

  private static final int sleepTime = 60 * 1000;
  private static final int TIMEOUT = 5000;

  private static String blockToTest;
  private static String blockLocation;
  private static String blockRecoveryNode;
  private static String cksumError;
  private static String initialBlockChecksum;
  private static String fileContent;

  private static String [] dataDirs;
  private static def nodesBeforeRecovery = [];
  private static def nodesAfterRecovery = [];

  @BeforeClass
  public static void setUp() {
    shHDFS.exec("rm -rf $localTestDir");
    shHDFS.exec("mkdir $localTestDir");
    shHDFS.exec("hadoop fs -rm -r $fsFilePath");
    Thread.sleep(TIMEOUT);
    shHDFS.exec("hadoop fs -mkdir -p $HDFS_TEST_DIR && hadoop fs -chmod 777 $HDFS_TEST_DIR");
    assertTrue("Failed to create input directory", shHDFS.getRet() == 0);

  }

  @AfterClass
  public static void tearDown() {
    // deletion of test files
    shHDFS.exec("rm -rf $localTestDir");
    assertTrue("Could not delete test directory $localTestDir", shHDFS.getRet() == 0);
  }

  @Test
  public void testBlockRecovery() {

    short numberOfDataNodes;
    short repFactor;

    final long fileLen = 10;
    final long SEED = 0;
    /* Find datanode data directory, make file, add content, ensure replication
     * is set to guarantee any chosen datanode will have block,
     * get block and its location, perform checksum before corrupting block
     * -- all on client side
     */
    conf = new HdfsConfiguration();
    FileSystem fileSys = DistributedFileSystem.get(conf);
    conf.addResource("hdfs-site.xml");
    def confDataDirs = conf.get("dfs.data.dir")
    if (confDataDirs == null)
      confDataDirs = conf.get("dfs.datanode.data.dir")
    // data.dirs might be configured w/ explicit file:// auth.
    // Should be stripped-off
    dataDirs = confDataDirs.split(",")*.replaceFirst(~/file:\/\//,'');

    numberOfDataNodes = shHDFS.exec("hdfs dfsadmin -report | grep ^Name | wc -l").getOut()[0] as short;
    // to recover a block at least two non-corrupted replicas should exist
    Assume.assumeTrue(numberOfDataNodes >= 3);
    // If passwordless setup isn't configured for user HDFS the test needs to
    // be skipped
    Assume.assumeTrue(shHDFS.exec("[ -f ${SSH_HDFS_ID} ]").getRet() == 0);

    // snapshot of everything before the corruption happens
    repFactor = (numberOfDataNodes - 1);
    try {
    	DFSTestUtil.createFile(fileSys, new Path(fsFilePath), fileLen, repFactor, SEED);
    } catch (Exception e) {
	assert "Exception should not be thrown"
    }
    fileContent = shHDFS.exec("hadoop fs -cat $fsFilePath").getOut()[0];
    assertTrue("File $fsFilePath doesn't exist", shHDFS.getRet() == 0);

    shHDFS.exec("hdfs fsck $fsFilePath -blocks -locations -files > $outputFile");
    assertTrue("Could not write output to file", shHDFS.getRet() == 0);

    nodesBeforeRecovery = shHDFS.exec("grep -o '\\[[^]]*\\]' $outputFile | " +
      "grep -o '[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*'").getOut();
    assertTrue("Could not obtain datanode addresses", shHDFS.getRet() == 0);

    blockToTest = shHDFS.exec("grep -o 'blk_[0-9]*' $outputFile").getOut()[0];
    assertTrue("Could not obtain block number", shHDFS.getRet() == 0);

    for (int i = 0; i < dataDirs.length; i++) {
      def dataDir = dataDirs[i]
      blockLocation = shHDFS.exec("find $dataDir -name $blockToTest | grep $dataDir").getOut()[0];
      if (blockLocation != null) break;
    }
    assertNotNull("Could not find specified block", blockLocation);

    initialBlockChecksum = shHDFS.exec("cksum $blockLocation").getOut()[0].split(" ")[0];
    assertTrue("Could not obtain checksum for block $blockToTest", shHDFS.getRet() == 0);
    // corrupt block
    shHDFS.exec("echo $corruptContent > $blockLocation");
    assertTrue("Could not write to file", shHDFS.getRet() == 0);

    // perform checksum after block corruption
    String corruptBlockChecksum = shHDFS.exec("cksum $blockLocation").getOut()[0].split(" ")[0];
    assertTrue("Could not obtain checksum for block $blockToTest", shHDFS.getRet() == 0);

    // trigger block recovery by trying to access the file
    shHDFS.exec("hadoop fs -cat $fsFilePath");

    // make sure checksum changes back to original, indicating block recovery
    for (int j = 0; j < 3; j++) {
      // wait a bit to let the block recover
      sleep(sleepTime);
      // see if checksum has changed
      cksumError = shHDFS.exec("hadoop fs -cat $fsFilePath | grep -o 'Checksum error'").getErr();
      if (cksumError != "Checksum error") break;
    }
    assertNotNull("Block has not been successfully triggered for recovery.", cksumError);

    nodesAfterRecovery = shHDFS.exec("hdfs fsck $fsFilePath -blocks -locations -files | $grepIP").getOut();
    assertTrue("Could not obtain datanode addresses", shHDFS.getRet() == 0);

    blockRecoveryNode = (nodesBeforeRecovery.intersect(nodesAfterRecovery))[0];

    if (blockRecoveryNode == null) {
      sleep(sleepTime);

      nodesAfterRecovery = shHDFS.exec("hdfs fsck $fsFilePath -blocks -locations -files | $grepIP").getOut();
      assertTrue("Could not obtain datanode addresses", shHDFS.getRet() == 0);

      blockRecoveryNode = (nodesBeforeRecovery.intersect(nodesAfterRecovery))[0];
      assert (blockRecoveryNode.size() != 0): "Block has not been successfully triggered for recovery."
    }

    int cksumAttempt;

    boolean success = false;

    // verify block has recovered. If not, give it a few more tries
    while (cksumAttempt < 3) {
      if (corruptBlockChecksum != initialBlockChecksum) {
        sleep(sleepTime);
        corruptBlockChecksum = shHDFS.exec("ssh -o StrictHostKeyChecking=no -i ${SSH_HDFS_ID} " +
          "$blockRecoveryNode 'cksum `find ${dataDirs.join(' ')}" +
          " -name $blockToTest 2>/dev/null | grep $blockToTest` '").getOut()[0].split(" ")[0];
        ++cksumAttempt;
      } else {
        // If block recovers, verify file content is the same as before corruption
        if (shHDFS.exec("hadoop fs -cat $fsFilePath").getOut()[0] == fileContent) {
          assertTrue("Could not read file $fsFilePath", shHDFS.getRet() == 0);
          success = true;
          break;
        }
      }
    }
    assertTrue("Block has not recovered", success);
    // Let's remove the garbage after the test
    shHDFS.exec("hadoop fs -rm -r -skipTrash $fsFilePath");
    assertTrue("Could not delete file $fsFilePath", shHDFS.getRet() == 0);
  }

}

