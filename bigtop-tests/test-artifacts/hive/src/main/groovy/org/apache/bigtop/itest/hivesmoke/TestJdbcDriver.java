/**
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

package org.apache.bigtop.itest.hivesmoke;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.apache.bigtop.itest.Contract;
import org.apache.bigtop.itest.ParameterSetter;
import org.apache.bigtop.itest.Property;
import org.apache.bigtop.itest.shell.Shell;

@Contract(
  properties = {
    @Property(name="hiveserver.startup.wait", type=Property.Type.INT, longValue=3000, intValue=3000, defaultValue="3000")
  },
  env = {})
public class TestJdbcDriver {

  public static String driverName = "org.apache.hadoop.hive.jdbc.HiveDriver";
  public static String hiveserver_url = "jdbc:hive://localhost:10000/default";
  public static Shell sh = new Shell("/bin/bash -s");
  public static String testDir = "/tmp/hive-jdbc." + (new Date().getTime());
  public static String hiveserver_pid;
  public static int hiveserver_startup_wait;
  private Connection con;

  @BeforeClass
  public static void setUp() throws ClassNotFoundException, InterruptedException, NoSuchFieldException, IllegalAccessException {
    ParameterSetter.setProperties(TestJdbcDriver.class, new String[] {"hiveserver_startup_wait"});
    System.out.println("hiveserver_startup_wait: " + hiveserver_startup_wait);
    Class.forName(driverName);
    sh.exec("hadoop fs -mkdir " + testDir);
    assertTrue("Could not create test directory", sh.getRet() == 0);
    sh.exec("hadoop fs -copyFromLocal a.txt " + testDir + "/a.txt");
    assertTrue("Could not copy local file to test directory", sh.getRet() == 0);
    // start hiveserver in background and remember the pid
    sh.exec("(HIVE_PORT=10000 hive --service hiveserver > /dev/null 2>&1 & echo $! ) 2> /dev/null");
    hiveserver_pid = sh.getOut().get(0);
    Thread.sleep(hiveserver_startup_wait); // allow time for hiveserver to be up
  }

  @Before
  public void getConnection() throws SQLException {
    //System.out.println(hiveserver_url);
    con = DriverManager.getConnection(hiveserver_url, "", "");
    //System.out.println("JDBC connection is " +
    //    (con == null ? "not instantiated." : "instantiated."));
  }

  @After
  public void closeConnection() throws SQLException {
    if (con != null)
      con.close();
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hadoop fs -rmr -skipTrash " + testDir);
    sh.exec("kill -9 " + hiveserver_pid);
  }

  @Test(timeout=120000L)
  public void testCreate() throws SQLException {
    Statement stmt = con.createStatement();
    String tableName = "hive_jdbc_driver_test";
    stmt.executeQuery("drop table if exists " + tableName);
    ResultSet res = stmt.executeQuery("create table " + tableName +
        " (key int, value string)");
    // show tables
    String sql = "show tables";
    //System.out.println("executing: " + sql);
    res = stmt.executeQuery(sql);
    boolean tableCreated = false;
    while (res.next()) {
      String tab_name = res.getString(1);
      //System.out.println(tab_name);
      if (tab_name.equals(tableName))
        tableCreated = true;
    }
    assertTrue("table " + tableName + " does not appear to be created",
        tableCreated);
    // describe table
    sql = "describe " + tableName;
    //System.out.println("executing: " + sql);
    res = stmt.executeQuery(sql);
    List<String> colNames = new ArrayList<String>();
    List<String> dataTypes = new ArrayList<String>();
    while (res.next()) {
      String col_name = res.getString(1);
      String data_type = res.getString(2);
      colNames.add(col_name);
      dataTypes.add(data_type);
      //System.out.println(col_name + "\t" + data_type);
    }
    assertEquals("table should have two columns", 2, colNames.size());
    assertEquals("key", colNames.get(0));
    assertEquals("value", colNames.get(1));
    assertEquals("int", dataTypes.get(0));
    assertEquals("string", dataTypes.get(1));

    // load data into table
    String filepath = testDir + "/a.txt"; // this is an hdfs filepath
    sql = "load data inpath '" + filepath + "' into table " + tableName;
    //System.out.println("executing: " + sql);
    res = stmt.executeQuery(sql);

    // select
    sql = "select * from " + tableName;
    //System.out.println("executing: " + sql);
    res = stmt.executeQuery(sql);
    List<Integer> keys = new ArrayList<Integer>();
    List<String> values = new ArrayList<String>();
    while (res.next()) {
      int key = res.getInt(1);
      String value = res.getString(2);
      keys.add(new Integer(key));
      values.add(value);
      //System.out.println("" + key + "\t" + value);
    }
    assertEquals("table should have two rows", 2, keys.size());
    assertEquals(new Integer(1), keys.get(0));
    assertEquals(new Integer(2), keys.get(1));
    assertEquals("foo", values.get(0));
    assertEquals("bar", values.get(1));
  }

}
