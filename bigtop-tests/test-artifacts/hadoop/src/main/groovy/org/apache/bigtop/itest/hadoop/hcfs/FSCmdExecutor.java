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
package org.apache.bigtop.itest.hadoop.hcfs;

import java.io.File;
import java.util.StringTokenizer;

import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.cli.CLITestHelper;
import org.apache.hadoop.cli.util.CommandExecutor;
import org.apache.hadoop.util.ToolRunner;

public class FSCmdExecutor extends CommandExecutor {
  protected String namenode = null;
  protected FsShell shell = null;

  public FSCmdExecutor(String namenode, FsShell shell) {
    this.namenode = namenode;
    this.shell = shell;
  }

  /**
   * Execute given hadoop FsShell command.
   */
  protected void execute(final String cmd) throws Exception{
    String[] args = getCommandAsArgs(cmd, "NAMENODE", this.namenode);
    ToolRunner.run(shell, args);
  }

  /**
   * Prapare given FsShell command for execution by ToolRunner.
   * This method also expands all variables used in the command.
   */
  @Override
  protected String[] getCommandAsArgs(final String cmd, final String masterKey,
                                      final String master) {
    StringTokenizer tokenizer = new StringTokenizer(cmd, " ");
    String[] args = new String[tokenizer.countTokens()];
    int i = 0;
    while (tokenizer.hasMoreTokens()) {
      args[i] = tokenizer.nextToken();
      // HCFS fs.default.name Hack (see TestCLI.java)
      args[i] = args[i].replaceAll("NAMENODETEST_DIR_ABSOLUTE", TestCLI.NAMENODE_TESTDIR_HACK);
      args[i] = args[i].replaceAll(masterKey, master);
      args[i] = args[i].replaceAll("CLITEST_DATA",
          new File(CLITestHelper.TEST_CACHE_DATA_DIR).
          toURI().toString().replace(' ', '+'));
      args[i] = args[i].replaceAll("USER_NAME", System.getProperty("user.name"));
      args[i] = args[i].replaceAll("TEST_DIR_ABSOLUTE", TestCLI.TEST_DIR_ABSOLUTE);
      args[i] = args[i].replaceAll("HCFS_SCHEME", TestCLI.HCFS_SCHEME);
      args[i] = args[i].replaceAll("HCFS_DIRSIZE", TestCLI.HCFS_DIRSIZE);
      args[i] = args[i].replaceAll("HCFS_NNMATCH", TestCLI.HCFS_NNMATCH);

      i++;
    }
    return args;
  }
}
