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
  private String runFailures = "false";
  private String serviceRestart = "false";
  private String serviceKill = "false";
  private String networkShutdown = "false";
  private String service = "crond";
  private String failureDelay = "0";
  private String startDelay = "0";
  private String killDuration = "0";

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
    if(propertyFile != null) {
      loadProps();
    }
  }

  public static FailureVars getInstance() {
    if(instance == null) {
      instance = new FailureVars();
    }
    return instance;
  }

  private void loadProps() {
    try {
      File pFile = new File(propertyFile);
      assert(pFile.exists()) : "Failure properties file cannot be read";
      BufferedReader is = new BufferedReader (new InputStreamReader(getClass(pFile)));
      System.out.println("Input Stream Location: " + is);
      Properties props = new Properties();
      props.load(is);
      is.close();
      testHost = props.getProperty("testhost");
      testRemoteHost = props.getProperty("testremotehost");
      runFailures = props.getProperty("runall");
      serviceRestart = props.getProperty("servicerestart");
      serviceKill = props.getProperty("servicekill");
      networkShutdown = props.getProperty("networkshutdown");
      service = props.getProperty("service");
      failureDelay = props.getProperty("failuredelay");
      startDelay = props.getProperty("startdelay");
      killDuration = props.getProperty("killduration");
    }
    catch(IOException ioe) {
      System.out.println(ioe.getMessage());
    }
  }

  public void setKillDuration(String killDuration) {
    this.killDuration = Long.toString(killDuration);
  }

  public void setTestHost(String testHost) {
    this.testHost = testHost;
  }

  public void setTestRemoteHost(String testRemoteHost) {
    this.testRemoteHost = testRemoteHost;
  }

  public void setRunAll(String runFailures) {
    this.runFailures = runFailures;
  }

  public void setRestart(String serviceRestart) {
    this.serviceRestart = serviceRestart;
  }

  public void setKill(String serviceKill) {
    this.serviceKill = serviceKill;
  }

  public void setShutdown(String networkShutdown) {
    this.networkShutdown = networkShutdown;
  }

  public void setFailureDelay(long failureDelay) {
    this.failureDelay = Long.toString(failureDelay);
  }

  public void setService(String service) {
    this.service = service;
  }

  public long getKillDuration() {
    return Long.parseLong(killDuration)*1000;
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
    return Long.parseLong(startDelay)*1000;
  }


  public String getRunFailures() {
    return runFailures;
  }

  public String getServiceRestart() {
    return serviceRestart;
  }

  public  String getServiceKill() {
    return serviceKill;
  }

  public String getNetworkShutdown() {
    return networkShutdown;
  }

  public long getFailureDelay() {
    return Long.parseLong(failureDelay)*1000;
  }

  int getSleepTime() {
    return SLEEP_TIME;
  }
}
