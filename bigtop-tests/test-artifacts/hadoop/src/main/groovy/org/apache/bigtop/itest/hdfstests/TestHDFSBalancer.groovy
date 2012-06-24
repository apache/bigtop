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
package org.apache.bigtop.itest.hdfstests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.JarContent;
import org.apache.bigtop.itest.shell.Shell;

public class TestHDFSBalancer {
 
  private static Shell shHDFS = new Shell("/bin/bash", "hdfs");
  // set with -Dthreshold
  private static String thresh = "10";

  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestHDFSBalancer.class, "." , null);
    if (System.getProperty("threshold") != null) {
      thresh = System.getProperty("threshold");
    }  
  }

  @AfterClass
  public static void tearDown() {
  }

  @Test
  public void testBalancer() { 
    System.out.println("Running Balancer:");
    System.out.println("Threshold is set to " + thresh +". Toggle by adding -Dthreshold=#");

    // must run balancer as hdfs user   
    shHDFS.exec("hdfs balancer -threshold $thresh");
  
    boolean success = false;
    // success_string message signifies balancing worked correctly
    String success_string1 = "The cluster is balanced. Exiting..."
    String success_string2 = "No block can be moved"
    String success_string3 = "No block has been moved for 3 iterations"
    List out_msgs = shHDFS.getOut();
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.equals(success_string1) || next_val.contains(success_string2) || next_val.contains(success_string3)) {
        success = true;
       }
    }

    String failure_string1 = "namenodes = []"
    List err_msgs = shHDFS.getErr();
    Iterator err = err_msgs.iterator();

    while (err.hasNext()) {
      String err_next = err.next()
      assertTrue("Balancer could not find namenode", !err_next.contains(failure_string1));
    }

    // could not just check if shHDFS.getRet() = 0 because balancer prints out INFO messages that the shell thinks are error messages 
    assertTrue("Balancer failed", success == true);
  }

}

