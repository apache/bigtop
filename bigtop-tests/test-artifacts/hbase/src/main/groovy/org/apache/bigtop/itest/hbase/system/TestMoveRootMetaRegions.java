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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import org.apache.bigtop.itest.shell.Shell;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.catalog.CatalogTracker;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test contains 3 tests:
 * <p/>
 * 1) Unload the region server hosting -ROOT-. Check that -ROOT- region
 * is accessible after a configurable period of time.
 * <p/>
 * 2) The test above for the .META. region.
 * <p/>
 * 3) Stop the region server(s) hosting the -ROOT- and .META. tables
 * and verify the regions are moved to other region server(s).
 * <p/>
 * This test does not restart the stopped region server, so users will
 * have to manually restart the region server.
 * <p/>
 * The third test is designed for clusters with more than two region servers.
 * <p/>
 * Optional arguments:
 * -Droot.timeout.ms=<milliseconds to wait while trying to find -ROOT->
 * -Dmeta.timeout.ms=<milliseconds to wait while trying to find .META.>
 * -Dwait.after.move.ms=<milliseconds to wait after moving -ROOT- or .META.>
 */

public class TestMoveRootMetaRegions {
  private static final String HBASE_HOME = System.getenv("HBASE_HOME");

  static {
    Assert.assertNotNull("HBASE_HOME has to be set to run this test", HBASE_HOME);
  }

  private static Shell sh = new Shell("/bin/bash -s");

  private static String load_regionserver =
      "$HBASE_HOME/bin/hbase org.jruby.Main $HBASE_HOME/bin/region_mover.rb load ";
  private static String unload_regionserver =
      "$HBASE_HOME/bin/hbase org.jruby.Main $HBASE_HOME/bin/region_mover.rb unload ";

  private static long meta_timeout_ms;
  private static long root_timeout_ms;
  private static long wait_after_move_ms;

  private static Configuration conf;
  private static HBaseAdmin admin;
  private static CatalogTracker ct;

  private static String meta_table =
      Bytes.toStringBinary(TableName.META_TABLE_NAME.getName());

  @BeforeClass
  public static void setUp() throws Exception {
    // Default timeout is 3 minutes.
    root_timeout_ms =
        Integer.parseInt(System.getProperty("root.timeout.ms", "180000"));
    meta_timeout_ms =
        Integer.parseInt(System.getProperty("meta.timeout.ms", "180000"));
    // Default to 20 seconds.
    wait_after_move_ms =
        Integer.parseInt(System.getProperty("wait.after.move.ms", "20000"));

    conf = HBaseConfiguration.create();
    admin = new HBaseAdmin(conf);

    ct = new CatalogTracker(admin.getConfiguration());
    ct.start();

    Assert.assertTrue(admin.tableExists(HConstants.META_TABLE_NAME));
    Assert.assertTrue(admin.isTableEnabled(HConstants.META_TABLE_NAME));
  }

  @AfterClass
  public static void tearDown() {
    // Cleanup
    ct.stop();
  }

  public static ServerName getMetaAddress() throws Exception {
    return ct.waitForMeta(meta_timeout_ms);
  }

  @Test
  public void unloadMetaRegionServer() throws Exception {
    ServerName meta_address = getMetaAddress();
    String cmd = unload_regionserver + meta_address.getHostname();
    System.out.println("Unloading the region server hosting " + meta_table);
    System.out.println(cmd);
    sh.exec(cmd);

    Thread.sleep(wait_after_move_ms);
    getMetaAddress();

    cmd = load_regionserver + meta_address.getHostname();
    System.out.println("Reloading the region server");
    sh.exec(cmd);
    Thread.sleep(wait_after_move_ms);
  }

  @Test
  public void testStopRootMetaRegionServers() throws Exception {
    ServerName meta_address = getMetaAddress();

    boolean same_server = false;

    System.out.println(meta_table + " server address: " + meta_address);

    System.out.println("Stopping region server(s)");
    admin.stopRegionServer(meta_address.getHostAndPort());

    Thread.sleep(wait_after_move_ms);

    ServerName new_meta_address = getMetaAddress();

    System.out.println(meta_table + " server address: " + new_meta_address);
    Assert.assertThat(meta_address, not(equalTo(new_meta_address)));
  }
}
