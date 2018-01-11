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
package org.apache.bigtop.itest.hbase.system;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.bigtop.itest.shell.Shell;

/**
 * This program unloads and reloads region servers and checks that
 * regions do not get stuck in transition for too long. The region
 * servers are specified by hostname.
 * <p>
 * Required arguments:
 * -Dregionservers=&lt;regionserver1&gt;,&lt;regionserver2&gt;,...
 * <p>
 * Optional arguments:
 * -Dload.iterations=&lt;number of times to unload and load the region servers&gt;
 * -Dtimeout.intervals=&lt;number of times to wait for no regions in transition&gt;
 * -Dtimeout.ms=&lt;milliseconds to wait before checking for regions in transition&gt;
 */
public class TestRegionMover {
  private static Shell sh = new Shell("/bin/bash -s");

  // Commands to execute the region mover and get the detailed HBase status.
  private static String load_regionserver =
      "$HBASE_HOME/bin/hbase org.jruby.Main $HBASE_HOME/bin/region_mover.rb load ";
  private static String unload_regionserver =
      "$HBASE_HOME/bin/hbase org.jruby.Main $HBASE_HOME/bin/region_mover.rb unload ";
  private static String hbase_status_detailed =
      "echo \"status \'detailed\'\" | $HBASE_HOME/bin/hbase shell";

  // Number of times we unload/load the region servers.
  private static int load_iterations;
  // Number of times we wait before we check the number of regions in transition.
  private static int timeout_intervals;
  // Number of milliseconds we wait during each interval.
  private static int timeout_ms;

  private static ArrayList<String> regionservers = new ArrayList<String>();

  private static final String HBASE_HOME = System.getenv("HBASE_HOME");

  static {
    assertNotNull("HBASE_HOME has to be set to run this test", HBASE_HOME);
  }

  @BeforeClass
  public static void setUp() throws InterruptedException {
    String region_servers = System.getProperty("regionservers", null);
    assertNotNull("Region server(s) must be specified to run this test",
        region_servers);
    StringTokenizer st = new StringTokenizer(region_servers, ",");
    while (st.hasMoreTokens()) {
      regionservers.add(st.nextToken());
    }
    System.out.println("Region servers to load/unload:\n" + regionservers);

    load_iterations = Integer.parseInt(System.getProperty("load.iterations", "10"));
    timeout_intervals = Integer.parseInt(System.getProperty("timeout.intervals", "20"));
    timeout_ms = Integer.parseInt(System.getProperty("timeout.ms", "20000"));
  }

  @AfterClass
  public static void tearDown() {
    // Cleanup
  }

  public static void waitForNoRegionsInTransition() throws InterruptedException {
    for (int i = 0; i < timeout_intervals; i++) {
      Thread.sleep(timeout_ms);
      System.out.println("Wait interval: " + i);
      sh.exec(hbase_status_detailed);
      String status = sh.getOut().toString();
      if (status.indexOf(" 0 regionsInTransition") != -1) {
        System.out.println(" 0 regionsInTransition.");
        return;
      } else {
        System.out.println("Regions still in transition");
      }
    }
    fail("Timed out waiting for regions to be out of transition");
  }

  @Test
  public void testRegionMover() throws InterruptedException {
    System.out.println("Beginning unloading and loading of region servers " +
        load_iterations + " times each");
    String cmd;
    for (int i = 0; i < load_iterations; i++) {
      for (String rs : regionservers) {
        // Unload
        cmd = unload_regionserver + rs;
        System.out.println(cmd);
        sh.exec(cmd);
        waitForNoRegionsInTransition();
        // Load
        cmd = load_regionserver + rs;
        System.out.println(cmd);
        sh.exec(cmd);
        waitForNoRegionsInTransition();
      }
    }
  }
}
