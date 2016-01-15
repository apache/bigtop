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
package org.apache.bigtop.itest.hadoop.hdfs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.commons.lang.StringUtils;

public class TestHDFSBalancer {

  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");
  // set with -Dthreshold
  private static String thresh = System.getProperty("threshold") ?: "10";

  /*
   * This function executes the hdfs balancer -threshold command with
   * the specified thresholdValue. If the command is expected to fail
   * then it will gracefully handle it according to the flag
   * isExpectedFail and will check the error output for the expectedMsg
   * specfied in the input.
   */

  public void testBalancerWithThreshold (String thresholdValue,
          boolean isExpectedFail, String expectedMsg) {
    println("Running Balancer with threshold value:" + thresholdValue);

    String success_string1 = "The cluster is balanced. Exiting...";
    String success_string2 = "No block can be moved";
    String success_string3 = "No block has been moved for 3 iterations";
    String success_string4 = "No block has been moved for 5 iterations";
    String failure_string1 = "Expecting a number in the range of [1.0, 100.0]:";
    String failure_string2 = "ERROR balancer.Balancer: Exiting balancer due an exception";
    String balancerRunningString = "Another balancer is running.";
    boolean success_balancer = false;
    int attemptsForBalancing = 10;
    List out_msgs_balancer = null;
    String out_msgs = "";
    boolean another_balancer_running = false;

    /*
     * If another balancer is already running when we submit the command
     * then it might return without balancing it. So we have to gracefully
     * wait for some time to finish the previous balancing to complete.
     */
    while(attemptsForBalancing>0) {
      attemptsForBalancing--;
      // set the threshold to the specified threshold value */
      shHDFS.exec("hdfs balancer -threshold " + thresholdValue);
      out_msgs_balancer = shHDFS.getOut();
      out_msgs = StringUtils.join(out_msgs_balancer.iterator(), ',');
      if (out_msgs.toLowerCase().contains(balancerRunningString.toLowerCase())) {
        another_balancer_running = true;
        println("another balancer is running so waiting for 1 min to retry..");
        sleep(60000);
        continue;
      } else {
        another_balancer_running=false;
	break;
      }
    }

    /*
     * if previous balancing task is still active even after 10 minutes
     * then fail the test
     */
    if (another_balancer_running) {
	assertTrue("When Run with threshold value:" + thresholdValue +
                   ", It seems another balancer is still running even after waiting for 10 mins",
                   false);
    }

    // check if the command outout is containing expected messages
    if (out_msgs.contains(success_string1) || out_msgs.contains(success_string2) ||
        out_msgs.contains(success_string3) || out_msgs.contains(success_string4)) {
      success_balancer = true;
      println("command output is having expected messages");
    } else {
      println("command output is not having expected messages");
    }

    // Check if the err outpur of command is containing unexpected messages
    List err_msgs_balancer = shHDFS.getErr();
    String err_msgs = StringUtils.join(err_msgs_balancer.iterator(), ',');
    if (isExpectedFail) {
      if (err_msgs.contains(expectedMsg)) {
        success_balancer = false;
      } else {
        success_balancer = true;
      }
    } else {
      if (err_msgs.contains(failure_string1) || err_msgs.contains(failure_string2)) {
	assertTrue("When Run with threshold value:" + thresholdValue +
                   ", Found some problem with the submitted command. " +
                   "Command did not get completed successfully", false);
      }
    }


    if (isExpectedFail == false) {
      assertTrue("When Run with threshold value:" + thresholdValue +
                 ", Balancer failed", success_balancer == true);
    } else {
      println("balancer is expected to fail so it got failed");
      assertTrue("When Run with threshold value:" + thresholdValue +
                 ", Balancer failed", success_balancer == false);
    }
  }

  @Test
  public void testBalancerWithFloatValue() {
    testBalancerWithThreshold("1.234", false, "");
  }

  @Test
  public void testBalancerWithMaximumThresholdValue() {
    testBalancerWithThreshold("100.0", false, "");
  }

  @Test
  public void testBalancerWithMinimumThresholdValue() {
    testBalancerWithThreshold("1.0", false, "");
  }

  @Test
  public void testBalancerWithZeroThresholdValue() {
    testBalancerWithThreshold("0.0", true,
            "Expecting a number in the range of [1.0, 100.0]");
  }

  @Test
  public void testBalancerWithProperThresholdValue() {
    testBalancerWithThreshold(thresh, false, "");
  }

  @Test
  public void testBalancerWithInvalidThresholdValue() {
    testBalancerWithThreshold("1000.0", true,
            "Expecting a number in the range of [1.0, 100.0]");
  }

}
