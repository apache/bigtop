/**
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

import org.apache.bigtop.itest.shell.Shell;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.cli.TestHDFSCLI;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;

public class TestDFSCLI extends TestHDFSCLI {
  public static final String TEST_DIR_ABSOLUTE = "/tmp/testcli_" + Long.valueOf(System.currentTimeMillis());
  public static String NAMENODE_TESTDIR_HACK;
  private String supergroup;
  private static Shell shHDFS = new Shell("/bin/bash");

  @Before
  @Override
  public void setUp() throws Exception {
    readTestConfigFile();

    // Configuration of real Hadoop cluster
    conf = new HdfsConfiguration();
    supergroup = System.getProperty("hcfs.root.groupname",
        conf.get(DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_KEY));
    namenode = conf.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY);
    username = System.getProperty("user.name");

    conf.setBoolean(CommonConfigurationKeys.HADOOP_SECURITY_AUTHORIZATION, true);
    // Many of the tests expect a replication value of 1 in the output
    conf.setInt("dfs.replication", 1);

    clitestDataDir = new File(TEST_CACHE_DATA_DIR).toURI().toString().replace(' ', '+');

    String[] createTestcliDirCmds = {
        "hadoop fs -mkdir -p " + TEST_DIR_ABSOLUTE,
        "hadoop fs -chmod 777 " + TEST_DIR_ABSOLUTE
    };
    shHDFS.exec((Object[])createTestcliDirCmds);

    // Check assumptions which would make some cases fail if not met
    Assert.assertEquals("Creation of testcli dir should succeed and return 0"
        + " (but it failed with the following error message: "
        + StringUtils.join(shHDFS.getErr().toArray(), "\\n") + ")",
        0, shHDFS.getRet());
    // We can't just use conf.setInt(fs.trash.interval", 0) because if trash is
    // enabled on the server, client configuration value is ignored.
    Assert.assertEquals("HDFS trash should be disabled via fs.trash.interval",
        0, conf.getInt("fs.trash.interval", 0));
    Assert.assertEquals("This test needs to be run under root user of hcfs",
        System.getProperty("hcfs.root.username", "hdfs"), username);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();

    String removeTestcliDirCmd = "hadoop fs -rm -r " + TEST_DIR_ABSOLUTE;
    shHDFS.exec(removeTestcliDirCmd);
  }

  @Override
  protected String getTestFile() {
    return "testDFSConf.xml";
  }

  /**
   * Expand commands from the test config file.
   * This method is used in displayResults() and compareTestOutput() only,
   * so it doesn't have any effect on the test execution itself.
   *
   * @param cmd executed command
   * @return String expanded command
   */
  @Override
  protected String expandCommand(String cmd) {
    String expCmd = super.expandCommand(cmd);
    // note: super.expandCommand() expands CLITEST_DATA and USERNAME
    expCmd = expCmd.replaceAll("NAMENODETEST_DIR_ABSOLUTE", NAMENODE_TESTDIR_HACK);
    expCmd = expCmd.replaceAll("TEST_DIR_ABSOLUTE", TEST_DIR_ABSOLUTE);
    expCmd = expCmd.replaceAll("supergroup", supergroup);
    expCmd = expCmd.replaceAll("NAMENODE", namenode);
    expCmd = expCmd.replaceAll("USER_NAME", System.getProperty("user.name"));
    return expCmd;
  }
}
