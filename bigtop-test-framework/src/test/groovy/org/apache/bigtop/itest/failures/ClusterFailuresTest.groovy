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

import org.apache.bigtop.itest.shell.OS
import org.junit.Test
import org.apache.bigtop.itest.shell.Shell

public class ClusterFailuresTest {
  private Shell rootShell = new Shell("/bin/bash", "root")
  private final int SLEEP_TIME = 100
  private final String TEST_HOST = "localhost"
  private final String TEST_REMOTE_HOST = "apache.org"
  private final String CRON_SERVICE

  {
    switch (OS.linux_flavor) {
      case ~/(?is).*(redhat|centos|rhel|fedora|enterpriseenterpriseserver).*/:
        CRON_SERVICE = "crond"
        break
      default:
        CRON_SERVICE = "cron"
    }
  }

  @Test
  void testServiceRestart() {
    startCron()
    assert isCronRunning(), "$CRON_SERVICE service isn't running before the test:"

    def cronKilled = new ServiceRestartFailure([TEST_HOST], "$CRON_SERVICE")
    Thread t = new Thread(cronKilled)
    t.start()

    while (isCronRunning()) {
      println "$CRON_SERVICE it still running"
      Thread.sleep(SLEEP_TIME)
    }

    try{
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

    def cronKilled = new ServiceKilledFailure([TEST_HOST], "$CRON_SERVICE")
    Thread t = new Thread(cronKilled)
    t.start()

    while (isCronRunning()) {
      println "$CRON_SERVICE it still running.."
      Thread.sleep(SLEEP_TIME)
    }

    try{
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
    rootShell.exec("iptables -D INPUT -s $TEST_REMOTE_HOST -j DROP")
    rootShell.exec("iptables -D OUTPUT -d $TEST_REMOTE_HOST -j DROP")

    assert isRemoteHostReachable(), "No ping to $TEST_REMOTE_HOST, which is used for network failures test:"

    def networkShutdown = new NetworkShutdownFailure(TEST_HOST, [TEST_REMOTE_HOST])
    Thread t = new Thread(networkShutdown)
    t.start()

    while (isRemoteHostReachable()) {
      println "$TEST_REMOTE_HOST is still reachable..."
      Thread.sleep(SLEEP_TIME)
    }

    try{
      assert !isRemoteHostReachable(), "Connection to $TEST_REMOTE_HOST hasn't been killed as expected:"
      println "$TEST_REMOTE_HOST isn't reachable. Good."
    } finally {
      t.interrupt()
    }

    while (!isRemoteHostReachable()) {
      println "$TEST_REMOTE_HOST isn't reachable..."
      Thread.sleep(SLEEP_TIME)
    }

    assert isRemoteHostReachable(), "Connection to $TEST_REMOTE_HOST hasn't been restored after the test:"
    println "$TEST_REMOTE_HOST is reachable again. Good."
  }

  private boolean isCronRunning() {
    return rootShell.exec("pgrep $CRON_SERVICE").ret == 0 ? true : false
  }

  private void startCron() {
    rootShell.exec("service $CRON_SERVICE start")
  }

  private boolean isRemoteHostReachable() {
    return rootShell.exec("ping -qc 1 $TEST_REMOTE_HOST").ret == 0 ? true : false
  }
}
