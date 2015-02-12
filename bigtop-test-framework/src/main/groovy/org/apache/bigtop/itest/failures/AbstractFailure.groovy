/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.failures

import org.apache.bigtop.itest.shell.Shell
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import static org.apache.bigtop.itest.failures.FailureConstants.PRIVATE_KEY_PATH_ENV_VAR
import static org.apache.bigtop.itest.failures.FailureConstants.BIGTOP_SMOKES_USER

/**
 * Abstract class to be subclassed by cluster failures classes of various types:
 *  - service restart
 *  - service being killed (kill -9)
 *  - network shutdown (iptables-based drop).
 *
 * Provides means to:
 *  - run set of "failure" commands against the specified list of hosts
 *  - restore the correct state.
 *
 *  Please see examples of usage in test class ClusterFailuresTest.
 *
 *  WARNING:
 *   - password-less (PKI-based) SSH for user specified in env variable BIGTOP_SMOKES_USER
 *     to all nodes in cluster being tested is assumed
 *   - for local tests, like ClusterFailuresTest, this SSH should be setup for localhost
 *   - env variable BIGTOP_SMOKES_CLUSTER_IDENTITY_FILE should point to according private key file.
 */
public abstract class AbstractFailure implements Runnable {
  protected static Shell rootShell = new Shell("/bin/bash", "root")

  /**
   * Used to wrap actual command to be executed over SSH, if running in distributed setup.
   * First substitution param is path to SSH private key, second - remote server username,
   * third - remote server host address, forth - actual command being wrapped.
   */
  protected static String SSH_COMMAND_WRAPPER = "ssh -i %s -o StrictHostKeyChecking=no %s@%s '%s'"

  /**
   * List of hosts to run fail/restore commands against.
   */
  protected List<String> hosts = []

  /**
   * List of failing commands, defined by a subclass, execute in given sequence.
   */
  protected List<String> failCommands = []

  /**
   * List of restore commands, defined by a subclass, execute in given sequence.
   */
  protected List<String> restoreCommands = []

  /**
   * How long (in milliseconds) shall we wait before executing first failure.
   */
  protected long failureDelay = 0

  /**
   * How long failure thread waits before next check if failure is over and it should call restore commands.
   */
  private static final SLEEP_TIME = 100;

  /**
   * Simple constructor for failures, uses default values.
   * @param hosts list of hosts this failure will be executed on.
   */
  public AbstractFailure(List<String> hosts) {
    this.hosts = hosts
  }

  /**
   * Constructor allowing to set all params.
   *
   * @param hosts list of hosts the failure will be running against
   * @param failureDelay how long (in millisecs) failure will wait before starting
   */
  public AbstractFailure(List<String> hosts, long failureDelay) {
    this.hosts = hosts
    this.failureDelay = failureDelay
  }

  /**
   * Runs failure/restore commands in a separate thread.
   */
  @Override
  public void run() {
    try {
      if (failureDelay > 0) {
        try {
          Thread.sleep(failureDelay)
        } catch (InterruptedException e) {
          return
        }
      }
      if (FailureVars.instance.getServiceRestart().equals("true")
        || FailureVars.instance.getServiceKill().equals("true")
        || FailureVars.instance.getNetworkShutdown().equals("true")) {
        runFailCommands()
        Thread.sleep(FailureVars.instance.getKillDuration())
      } else {
        if (failureDelay > 0) {
          try {
            Thread.sleep(failureDelay)
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt()
            return
          }
        }
        runFailCommands()

        while (!Thread.currentThread().isInterrupted()) {
          try {
            Thread.sleep(SLEEP_TIME)
          } catch (InterruptedException e) {
            return
          }
        }
      }
    } finally {
      runRestoreCommands()
    }
  }

  private void runRestoreCommands() {
    restoreCommands.each {
      rootShell.exec(it)
      logError(rootShell)
      assert rootShell.getRet() == 0, "Restore command $it has returned non-0 error code:"
    }
  }

  private void runFailCommands() {
    failCommands.each {
      rootShell.exec(it)
      logError(rootShell)

      //some commands, like pkill over ssh, return 137. It's ok.
      //assertTrue(rootShell.getRet() == 0)
    }
  }

  /**
   * Reads the full path to private key file from env. variable PRIVATE_KEY_PATH_ENV_VAR.
   * @return full path to file with private key for SSH connections to cluster.
   */
  protected String getIdentityFile() {
    String identityFile = System.getenv(PRIVATE_KEY_PATH_ENV_VAR)
    assert identityFile, "Env variable $PRIVATE_KEY_PATH_ENV_VAR is not set:"
    return identityFile
  }

  /**
   * Reads the username used for ssh commands from env. variable BIGTOP_SMOKES_USER.
   * @return user which will be used to run SSH command on target hosts
   */
  protected String getSshUser() {
    String sshUser = System.getenv(BIGTOP_SMOKES_USER)
    assert sshUser, "Env variable $BIGTOP_SMOKES_USER is not set:"
    return sshUser
  }

  /**
   * If tests are running in distributed mode, i.e. not itest framework tests,
   * but real cluster smoke tests, wrapping failure command to go over SSH to node on the cluster.
   *
   * @param formattedCommand actual failure command to be executed on the remote node
   * @param host remote node to run command on
   * @return full command to be executed in the local shell
   */
  protected String getSshWrappedCommand(String formattedCommand, String host) {
    def identityFile = getIdentityFile()
    def sshUser = getSshUser()

    return String.format(SSH_COMMAND_WRAPPER, identityFile, sshUser, host, formattedCommand);
  }
}
