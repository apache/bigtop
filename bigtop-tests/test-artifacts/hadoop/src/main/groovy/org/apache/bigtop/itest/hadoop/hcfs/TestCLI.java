/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hadoop.hcfs;

import java.io.File;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.cli.CLITestHelper;
import org.apache.hadoop.cli.util.CLICommand;
import org.apache.hadoop.cli.util.CLICommandFS;
import org.apache.hadoop.cli.util.CommandExecutor;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.bigtop.itest.shell.Shell;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests for the Command Line Interface (CLI)
 */
public class TestCLI extends CLITestHelper {
  public static final String TEST_DIR_ABSOLUTE = "/tmp/testcli_" + Long.valueOf(System.currentTimeMillis());
  public static String HCFS_SCHEME;
  public static String HCFS_DIRSIZE;
  public static String HCFS_NNMATCH;
  public static String NAMENODE_TESTDIR_HACK;
  private String supergroup;
  private String namenode;
  private static Shell shHDFS = new Shell("/bin/bash");
  private static Log LOG = LogFactory.getLog(Shell.class);

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
    // We can't just use conf.setInt(fs.trash.interval",0) because if trash is
    // enabled on the server, client configuration value is ignored.
    /*Assert.assertEquals("HDFS trash should be disabled via fs.trash.interval",
        0, conf.getInt("fs.trash.interval", 0));*/
    LOG.info("HDFS fs.trash.interval is set to: "+conf.getInt("fs.trash.interval", 0));
    Assert.assertEquals("This test needs to be run under root user of hcfs",
        System.getProperty("hcfs.root.username", "hdfs"),
        System.getProperty("user.name"));

    // Initialize variables from test config file
    HCFS_SCHEME = System.getProperty("hcfs.scheme", "hdfs:");
    HCFS_DIRSIZE = System.getProperty("hcfs.dirsize.pattern", "0");
    HCFS_NNMATCH = System.getProperty("hcfs.namenode.pattern", "\\\\w+[-.a-z0-9]*(:[0-9]+)?");

    // HCFS fs.default.name Hack
    // Hadoop property 'fs.default.name' usually has value like this one:
    // "hdfs://namenode_hostname:port". But for other hadoop filesystems, the
    // value may just end with 3 slashes in a row (eg. 'glusterfs:///' or
    // 'maprfs:///'). This leads to file paths with 4 slashes in it (eg.
    // 'glusterfs:////tmp/testcli_sth') which are shortened back to
    // 'glusterfs:///tmp/...' if the file actually exists. To fix this we just
    // replace 4 slashes with 3 to prevent this from happening.
    String namenode_testdir = namenode + TEST_DIR_ABSOLUTE;
    NAMENODE_TESTDIR_HACK = namenode_testdir.replace(":////", ":///");
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
    return "testHCFSConf.xml";
  }

  @Test
  @Override
  public void testAll() {
    super.testAll();
  }

  /**
   * Expand commands from the test config file.
   * This method is used in displayResults() and compareTestOutput() only,
   * so it doesn't have any effect on the test execution itself.
   *
   * @param cmd
   * @return String expanded command
   */
  @Override
  protected String expandCommand(final String cmd) {
    String expCmd = super.expandCommand(cmd);
    // note: super.expandCommand() expands CLITEST_DATA and USERNAME
    expCmd = expCmd.replaceAll("NAMENODETEST_DIR_ABSOLUTE", NAMENODE_TESTDIR_HACK);
    expCmd = expCmd.replaceAll("TEST_DIR_ABSOLUTE", TEST_DIR_ABSOLUTE);
    expCmd = expCmd.replaceAll("supergroup", supergroup);
    expCmd = expCmd.replaceAll("NAMENODE", namenode);
    expCmd = expCmd.replaceAll("USER_NAME", username);
    expCmd = expCmd.replaceAll("HCFS_SCHEME", HCFS_SCHEME);
    expCmd = expCmd.replaceAll("HCFS_DIRSIZE", HCFS_DIRSIZE);
    expCmd = expCmd.replaceAll("HCFS_NNMATCH", HCFS_NNMATCH);
    return expCmd;
  }

  /**
   * Execute given hadoop FsShell command (via Toolrunner).
   */
  @Override
  protected CommandExecutor.Result execute(CLICommand cmd) throws Exception {
    if (cmd.getType() instanceof CLICommandFS) {
      CommandExecutor cmdExecutor = new FSCmdExecutor(namenode, new FsShell(conf));
      return cmdExecutor.executeCommand(cmd.getCmd());
    } else {
      throw new IllegalArgumentException("Unknown type of test command: " + cmd.getType());
    }
  }
}
