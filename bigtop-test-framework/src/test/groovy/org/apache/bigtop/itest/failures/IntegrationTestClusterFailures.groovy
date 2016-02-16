/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.failures

import org.apache.bigtop.itest.TestUtils
import org.apache.bigtop.itest.shell.OS
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.apache.bigtop.itest.shell.Shell

public class IntegrationTestClusterFailures {
  private Shell rootShell = new Shell("/bin/bash", "root")
  private final int SLEEP_TIME = 100
  private final String CRON_SERVICE
  private String testHost;
  private String testRemoteHost;
  private String serviceRestart;
  private String serviceKill;
  private String networkShutdown;

  {
    switch (OS.linux_flavor) {
      case ~/(?is).*(redhat|centos|rhel|fedora|enterpriseenterpriseserver).*/:
        CRON_SERVICE = "crond"
        break
      default:
        CRON_SERVICE = "cron"
    }
  }

  @Before
  void configureVars() {
    Assume.assumeTrue("Password-less sudo should be enabled", TestUtils.noPassSudo())
    def f = FailureVars.getInstance();
    testHost = f.getTestHost();
    testRemoteHost = f.getTestRemoteHost();
    serviceRestart = f.getServiceRestart();
    serviceKill = f.getServiceKill();
    networkShutdown = f.getNetworkShutdown();
  }

  @Test
  void testServiceRestart() {
    startCron()
    assert isCronRunning(), "$CRON_SERVICE service isn't running before the test:"

    def cronKilled = new ServiceRestartFailure([testHost], "$CRON_SERVICE")
    Thread t = new Thread(cronKilled)
    t.start()

    while (isCronRunning()) {
      println "$CRON_SERVICE it still running"
      Thread.sleep(SLEEP_TIME)
    }

    try {
      assert !isCronRunning(), "$CRON_SERVICE hasn't been stopped as expected:"
      println "$CRON_SERVICE stopped. Good."
    } finally {
      t.interrupt()
    }

    while (!isCronRunning()) {
      println "$CRON_SERVICE it still stopped.."
      Thread.sleep(SLEEP_TIME)
    }

    assert isCronRunning(), "$CRON_SERVICE hasn't been restarted after the test:"
    println "$CRON_SERVICE is up. Good"
  }

  @Test
  void testServiceKilled() {
    // On Ubuntu services like cron or ssh get restarted automatically if killed,
    // so for now disabling this test for Ubuntu users.
    if (OS.linux_flavor ==~ /(?is).*(ubuntu|debian).*/) {
      println "As you're running on $OS.linux_flavor, testServiceKilled() doesn't run for you."
      return
    }

    startCron()
    assert isCronRunning(), "$CRON_SERVICE service isn't running before the test:"

    def cronKilled = new ServiceKilledFailure([testHost], "$CRON_SERVICE")
    Thread t = new Thread(cronKilled)
    t.start()

    while (isCronRunning()) {
      println "$CRON_SERVICE it still running.."
      Thread.sleep(SLEEP_TIME)
    }

    try {
      assert !isCronRunning(), "$CRON_SERVICE hasn't been killed as expected:"
      println "$CRON_SERVICE killed. Good."
    } finally {
      t.interrupt()
    }

    while (!isCronRunning()) {
      println "$CRON_SERVICE it still killed..."
      Thread.sleep(SLEEP_TIME)
    }

    assert isCronRunning(), "$CRON_SERVICE hasn't been restarted after the test:"
    println "$CRON_SERVICE is up. Good."
  }

  @Test
  void testNetworkShutdown() {
    //make sure there are no blocking rules
    rootShell.exec("iptables -D INPUT -s $testRemoteHost -j DROP")
    rootShell.exec("iptables -D OUTPUT -d $testRemoteHost -j DROP")

    assert isRemoteHostReachable(), "No ping to $testRemoteHost, which is used for network failures test:"

    def networkShutdown = new NetworkShutdownFailure(testHost, [testRemoteHost])
    Thread t = new Thread(networkShutdown)
    t.start()

    while (isRemoteHostReachable()) {
      println "$testRemoteHost is still reachable..."
      Thread.sleep(SLEEP_TIME)
    }

    try {
      assert !isRemoteHostReachable(), "Connection to $testRemoteHost hasn't been killed as expected:"
      println "$testRemoteHost isn't reachable. Good."
    } finally {
      t.interrupt()
    }

    while (!isRemoteHostReachable()) {
      println "$testRemoteHost isn't reachable..."
      Thread.sleep(SLEEP_TIME)
    }

    assert isRemoteHostReachable(), "Connection to $testRemoteHost hasn't been restored after the test:"
    println "$testRemoteHost is reachable again. Good."
  }

  private boolean isCronRunning() {
    return rootShell.exec("pgrep $CRON_SERVICE").ret == 0 ? true : false
  }

  private void startCron() {
    rootShell.exec("service $CRON_SERVICE start")
  }

  private boolean isRemoteHostReachable() {
    return rootShell.exec("ping -qc 1 $testRemoteHost").ret == 0 ? true : false
  }
}
