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

import org.apache.bigtop.itest.shell.OS
import org.apache.bigtop.itest.shell.Shell
import java.io.BufferedReader
import java.io.IOException;

/**
 * This class manages objects, variables, and command line parameter values for cluster failure testing.
 * By default, all parameters are off or set to false.
 *
 * The system property "failurePropertiesFile" represents the path to the file containing test parameters
 * and must be set in order to parametrize a test. Failure scenario parameters are:
 * testhost
 * testremotehost
 * runall
 * servicerestart
 * servicekill
 * networkshutdown
 * service
 * failuredelay
 * startdelay
 * killduration
 */
public class FailureVars {

  private final String CRON_SERVICE;
  private final int SLEEP_TIME = 100;
  private static FailureVars instance = null;
  private String propertyFile = System.getProperty("failurePropertiesFile");
  private String testHost = "localhost";
  private String testRemoteHost = "apache.org";
  private boolean runFailures = false;
  private boolean serviceRestart = false;
  private boolean serviceKill = false;
  private boolean networkShutdown = false;
  private String service = "crond";
  private long failureDelay = 0;
  private long startDelay = 0;
  private long killDuration = 0;

  {
    switch (OS.linux_flavor) {
      case ~/(?is).*(redhat|centos|rhel|fedora|enterpriseenterpriseserver).*/:
        CRON_SERVICE = "crond"
        break;
      default:
        CRON_SERVICE = "cron"
    }
  }

  protected FailureVars() {
    if (propertyFile != null) {
      loadProps();
    }
  }

  public static FailureVars getInstance() {
    if (instance == null) {
      instance = new FailureVars();
    }
    return instance;
  }

  private void loadProps() {
    try {
      File pFile = new File(propertyFile);
      assert (pFile.exists()): "Failure properties file cannot be read";
      BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(pFile)));
      System.out.println("Input Stream Location: " + is);
      Properties props = new Properties();
      props.load(is);
      is.close();
      testHost = props.getProperty("testhost");
      testRemoteHost = props.getProperty("testremotehost");
      runFailures = Boolean.parseBoolean(props.getProperty("runall"));
      serviceRestart = Boolean.parseBoolean(props.getProperty("servicerestart"));
      serviceKill = Boolean.parseBoolean(props.getPropery("servicekill"));
      networkShutdown = Boolean.parseBoolean(props.getProperty("networkshutdown"));
      service = props.getProperty("service");
      failureDelay = Long.parseLong(props.getProperty("failuredelay"));
      startDelay = Long.parseLong(props.getProperty("startdelay"));
      killDuration = Long.parseLong(props.getProperty("killduration"));
    }
    catch (IOException ioe) {
      System.out.println(ioe.getMessage());
    }
  }

  public void setKillDuration(long killDuration) {
    this.killDuration = killDuration;
  }

  public void setTestHost(String testHost) {
    this.testHost = testHost;
  }

  public void setTestRemoteHost(String testRemoteHost) {
    this.testRemoteHost = testRemoteHost;
  }

  public void setRunAll(boolean runFailures) {
    this.runFailures = runFailures;
  }

  public void setRestart(boolean serviceRestart) {
    this.serviceRestart = serviceRestart;
  }

  public void setKill(boolean serviceKill) {
    this.serviceKill = serviceKill;
  }

  public void setShutdown(boolean networkShutdown) {
    this.networkShutdown = networkShutdown;
  }

  public void setFailureDelay(long failureDelay) {
    this.failureDelay = failureDelay;
  }

  public void setService(String service) {
    this.service = service;
  }

  public long getKillDuration() {
    return killDuration * 1000;
  }

  public String getTestHost() {
    return testHost;
  }

  public String getService() {
    return service;
  }

  public String getTestRemoteHost() {
    return testRemoteHost;
  }

  public long getStartDelay() {
    return startDelay * 1000;
  }


  public boolean getRunFailures() {
    return runFailures;
  }

  public boolean getServiceRestart() {
    return serviceRestart;
  }

  public boolean getServiceKill() {
    return serviceKill;
  }

  public boolean getNetworkShutdown() {
    return networkShutdown;
  }

  public long getFailureDelay() {
    return failureDelay * 1000;
  }

  int getSleepTime() {
    return SLEEP_TIME;
  }
}
