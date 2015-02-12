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

package org.apache.bigtop.itest.shell

import org.junit.Assert;

/**
 * Extension of {@link Shell} with return code checking.
 */
class JUnitShell extends Shell {

  /**
   * Instantiate with the given shell
   * @param shell shell to use
   */
  JUnitShell(String shell) {
    super(shell)
  }

  /**
   * Instantiate with the given shell and user name
   * @param shell shell to use
   * @param user user
   */
  JUnitShell(String shell, String user) {
    super(shell, user)
  }

  /**
   * Instantiate with the default shell, {@link Shell#DEFAULT_SHELL}
   */
  JUnitShell() {
  }

  /**
   * Execute a shell script expecting a specific exit code.
   * The exit code is only checked at the end of the sequence
   * @param expectedExitCode the expected exit code
   * @param args shell script split into multiple Strings -one per line.
   */
  void expectExitCode(int expectedExitCode, Object... args) {
    exec(args)
    assertExitCode(expectedExitCode)
  }

  /**
   * Execute expecting an exit code of "0", "success"
   * @param args shell script split into multiple Strings
   */
  void expectSuccess(Object... args) {
    expectExitCode(0, args)
  }

  /**
   * Assert the shell exited with a given error code
   * if not the output is printed and an assertion is raised
   * @param expectedExitCode expected error code
   * @throws AssertionError if the return code is wrong
   */
  void assertExitCode(int expectedExitCode) {
    int result = signCorrectedReturnCode()
    if (result != expectedExitCode) {
      dumpOutput()
      Assert.assertEquals(
        "Wrong exit code of script ${script}" as String,
        expectedExitCode, result)
    }
  }

}
