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
package org.apache.bigtop.itest.hadooptests;

import java.io.File;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.cli.CLITestHelper;
import org.apache.hadoop.cli.util.CLICommand;
import org.apache.hadoop.cli.util.CLICommandFS;
import org.apache.hadoop.cli.util.CommandExecutor;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.HdfsConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the Command Line Interface (CLI)
 */
public class TestCLI extends CLITestHelper {
  public static final String TEST_DIR_ABSOLUTE = "/tmp/testcli";
  private String nn;
  private String sug;

  @Before
  @Override
  public void setUp() throws Exception {
    readTestConfigFile();
    conf = new HdfsConfiguration();
    conf.setBoolean(CommonConfigurationKeys.HADOOP_SECURITY_AUTHORIZATION, 
                    true);
    clitestDataDir =
      new File(TEST_CACHE_DATA_DIR).toURI().toString().replace(' ', '+');
    nn = conf.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY);
    sug = conf.get(DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_KEY);
    // Many of the tests expect a replication value of 1 in the output
    conf.setInt("dfs.replication", 1);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Override
  protected String getTestFile() {
    return "testConf.xml";
  }

  @Test
  @Override
  public void testAll() {
    super.testAll();
  }

  @Override
  protected String expandCommand(final String cmd) {
    String expCmd = super.expandCommand(cmd);
    String testcliDir = TEST_DIR_ABSOLUTE;
    expCmd = expCmd.replaceAll("TEST_DIR_ABSOLUTE", testcliDir);
    expCmd = expCmd.replaceAll("SUPERGROUP", sug);
    return expCmd;
  }

  @Override
  protected CommandExecutor.Result execute(CLICommand cmd) throws Exception {
    if (cmd.getType() instanceof CLICommandFS) {
      CommandExecutor cmdExecutor = new FSCmdExecutor(nn, new FsShell(conf));
      return cmdExecutor.executeCommand(cmd.getCmd());
    } else {
      throw new IllegalArgumentException("Unknown type of test command: " + cmd.getType());
    }
  }
}
