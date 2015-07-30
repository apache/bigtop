/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.bigtop.itest.failures

import org.apache.bigtop.itest.failures.FailureVars
import org.apache.bigtop.itest.failures.NetworkShutdownFailure
import org.apache.bigtop.itest.failures.ServiceKilledFailure
import org.apache.bigtop.itest.failures.ServiceRestartFailure
import org.apache.bigtop.itest.failures.FailureConstants
import org.apache.bigtop.itest.shell.OS
import org.junit.Test
import org.apache.bigtop.itest.shell.Shell

/**
 * A runnable that executes the cluster failure threads.
 * Used to run in parallel to hadoop jobs to test their completion.
 */
public class FailureExecutor implements Runnable {

  private boolean restart = FailureVars.instance.getServiceRestart();
  private boolean kill = FailureVars.instance.getServiceKill();
  private boolean shutdown = FailureVars.instance.getNetworkShutdown();
  private String testHost = FailureVars.instance.getTestHost();
  private String testRemoteHost = FailureVars.instance.getTestRemoteHost();
  private long failureDelay = FailureVars.instance.getFailureDelay();
  private long startDelay = FailureVars.instance.getStartDelay();

  Thread restartThread = null;
  Thread killThread = null;
  Thread shutdownThread = null;

  public void run() {
    if (startDelay > 0) {
      try {
        Thread.sleep(startDelay)
      } catch (InterruptedException e) {
      }
    }
    if (restart != null && restart.equals("true")) {
      serviceRestartExec();
    }
    if (kill != null && kill.equals("true")) {
      serviceKillExec();
    }
    if (shutdown != null && shutdown.equals("true")) {
      networkShutdownExec();
    }
  }

  public void serviceRestartExec() {
    System.out.println("Restarting services...")
    def srf = new ServiceRestartFailure([testHost],
      FailureVars.instance.CRON_SERVICE, failureDelay);
    restartThread = new Thread(srf, "restartThread");
    restartThread.start();
    restartThread.join();
    System.out.println("Finished restarting services.\n");
  }

  public void serviceKillExec() {
    System.out.println("Killing services....")
    def skf = new ServiceKilledFailure([testHost],
      FailureVars.instance.CRON_SERVICE, failureDelay);
    killThread = new Thread(skf, "killThread");
    killThread.start();
    killThread.join();
    System.out.println("Finished killing services.\n");
  }

  public void networkShutdownExec() {
    System.out.println("Shutting down network...")
    def nsf = new NetworkShutdownFailure(testHost,
      [testRemoteHost], failureDelay);
    shutdownThread = new Thread(nsf)
    shutdownThread.start();
    shutdownThread.join();
    System.out.println("Finished restarting network.\n");
  }
}
