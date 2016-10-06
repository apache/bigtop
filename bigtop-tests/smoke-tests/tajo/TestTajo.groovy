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

package org.apache.bigtop.itest.tajo

import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.Test

import org.apache.bigtop.itest.shell.Shell
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import java.sql.*

import static org.junit.Assert.assertEquals

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestTajo {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")
  static String pwd = ""

  static Connection conn = null
  static Statement stmt = null
  static ResultSet rs = null

  @BeforeClass
  static void setUp() {
    try {
      Class.forName("org.apache.tajo.jdbc.TajoDriver")
    } catch (ClassNotFoundException e) {
      logError(e)
    }

    String url = "jdbc:tajo://localhost/default"
    conn = DriverManager.getConnection(url)
    stmt = conn.createStatement()
  }

  @AfterClass
  public static void tearDown() {
    dropTable()
    if (stmt != null) stmt.close()
    if (conn != null) conn.close()
  }

  static void dropTable() {
    stmt.executeQuery("drop table if exists table1")
  }

  @Test
  void test1CreateSQL() {
    try {
      sh.exec("pwd")
      pwd = sh.out
      int lastIndex = pwd.length() - 1
      pwd = pwd.substring(1, lastIndex)

      String query = "create external table if not exists table1 (" +
              "      id int," +
              "      name text," +
              "      score float," +
              "      type text)" +
              "      using text with ('text.delimiter'='|') location 'file://"+ pwd +"/table1'"
      LOG.info(query)
      rs = stmt.executeQuery(query)
      while (rs.next()) {
        LOG.info(rs.getString(1) + "," + rs.getString(3))
      }
    } finally {
      if (rs != null) rs.close()
    }
  }

  @Test
  void test2SelectSQL() {
    StringBuffer answer = new StringBuffer()
    StringBuffer correct = new StringBuffer()
    correct.append("1,1.1")
    correct.append("2,2.3")
    correct.append("3,3.4")
    correct.append("4,4.5")
    correct.append("5,5.6")
    try {
      rs = stmt.executeQuery("select * from table1")
      while (rs.next()) {
        String str = rs.getString(1) + "," + rs.getString(3)
        LOG.info(str)
        answer.append(str)
      }
    } finally {
      if (rs != null) rs.close()
    }
    assertEquals(correct.toString(), answer.toString())
  }
}
