/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.odpi.specs.runtime.hive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;

public class TestSql extends JdbcConnector {
  private static final Log LOG = LogFactory.getLog(TestSql.class.getName());

  @Test
  public void db() throws SQLException {
    final String db1 = "odpi_sql_db1";
    final String db2 = "odpi_sql_db2";
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("drop database if exists " + db1 + " cascade");

      // Simple create database
      stmt.execute("create database " + db1);
      stmt.execute("drop database " + db1);

      stmt.execute("drop schema if exists " + db2 + " cascade");

      String location = getProperty(LOCATION, "a writable directory in HDFS");

      // All the bells and whistles
      stmt.execute("create schema if not exists " + db2 + " comment 'a db' location '" + location +
          "' with dbproperties ('a' = 'b')");

      stmt.execute("alter database " + db2 + " set dbproperties ('c' = 'd')");

      stmt.execute("drop database " + db2 + " restrict");
    }
  }

  @Test
  public void table() throws SQLException {
    final String table1 = "odpi_sql_table1";
    final String table2 = "odpi_sql_table2";
    final String table3 = "odpi_sql_table3";
    final String table4 = "odpi_sql_table4";
    final String table5 = "odpi_sql_table5";

    try (Statement stmt = conn.createStatement()) {
      stmt.execute("drop table if exists " + table1);
      stmt.execute("drop table if exists " + table2);
      stmt.execute("drop table if exists " + table3);
      stmt.execute("drop table if exists " + table4);
      stmt.execute("drop table if exists " + table5);

      String location = getProperty(LOCATION, "a writable directory in HDFS");
      stmt.execute("create external table " + table1 + "(a int, b varchar(32)) location '" +
          location + "'");

      // With a little bit of everything, except partitions, we'll do those below
      stmt.execute("create table if not exists " + table2 +
          "(c1 tinyint," +
          " c2 smallint," +
          " c3 int comment 'a column comment'," +
          " c4 bigint," +
          " c5 float," +
          " c6 double," +
          " c7 decimal," +
          " c8 decimal(12)," +
          " c9 decimal(8,2)," +
          " c10 timestamp," +
          " c11 date," +
          " c12 string," +
          " c13 varchar(120)," +
          " c14 char(10)," +
          " c15 boolean," +
          " c16 binary," +
          " c17 array<string>," +
          " c18 map <string, string>," +
          " c19 struct<s1:int, s2:bigint>," +
          " c20 uniontype<int, string>) " +
          "comment 'table comment'" +
          "clustered by (c1) sorted by (c2) into 10 buckets " +
          "stored as orc " +
          "tblproperties ('a' = 'b')");

      // NOTES: Not testing SKEWED BY, ROW FORMAT, STORED BY (storage handler

      stmt.execute("create temporary table " + table3 + " like " + table2);

      stmt.execute("insert into " + table1 + " values (3, 'abc'), (4, 'def')");

      stmt.execute("create table " + table4 + " as select a, b from " + table1);

      stmt.execute("truncate table " + table4);

      stmt.execute("alter table " + table4 + " rename to " + table5);
      stmt.execute("alter table " + table2 + " set tblproperties ('c' = 'd')");

      // NOTE: Not testing alter of clustered or sorted by, because that's suicidal
      // NOTE: Not testing alter of skewed or serde properties since we didn't test it for create
      // above.

      stmt.execute("drop table " + table1 + " purge");
      stmt.execute("drop table " + table2);
      stmt.execute("drop table " + table3);
      stmt.execute("drop table " + table5);
    }
  }

  @Test
  public void partitionedTable() throws SQLException {
    final String table1 = "odpi_sql_ptable1";
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("drop table if exists " + table1);

      stmt.execute("create table " + table1 +
          "(c1 int," +
          " c2 varchar(32))" +
          "partitioned by (p1 string comment 'a partition column')" +
          "stored as orc");

      stmt.execute("alter table " + table1 + " add partition (p1 = 'a')");
      stmt.execute("insert into " + table1 + " partition (p1 = 'a') values (1, 'abc')");
      stmt.execute("insert into " + table1 + " partition (p1 = 'a') values (2, 'def')");
      stmt.execute("insert into " + table1 + " partition (p1 = 'a') values (3, 'ghi')");
      stmt.execute("alter table " + table1 + " partition (p1 = 'a') concatenate");
      stmt.execute("alter table " + table1 + " touch partition (p1 = 'a')");

      stmt.execute("alter table " + table1 + " add columns (c3 float)");
      stmt.execute("alter table " + table1 + " drop partition (p1 = 'a')");

      // NOTE: Not testing rename partition, exchange partition, msck repair, archive/unarchive,
      // set location, enable/disable no_drop/offline, compact (because not everyone may have
      // ACID on), change column

      stmt.execute("drop table " + table1);

    }
  }

  @Test
  public void view() throws SQLException {
    final String table1 = "odpi_sql_vtable1";
    final String view1 = "odpi_sql_view1";
    final String view2 = "odpi_sql_view2";
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("drop table if exists " + table1);
      stmt.execute("drop view if exists " + view1);
      stmt.execute("drop view if exists " + view2);
      stmt.execute("create table " + table1 + "(a int, b varchar(32))");
      stmt.execute("create view " + view1 + " as select a from " + table1);

      stmt.execute("create view if not exists " + view2 +
          " comment 'a view comment' " +
          "tblproperties ('a' = 'b') " +
          "as select b from " + table1);

      stmt.execute("alter view " + view1 + " as select a, b from " + table1);
      stmt.execute("alter view " + view2 + " set tblproperties('c' = 'd')");

      stmt.execute("drop view " + view1);
      stmt.execute("drop view " + view2);
    }
  }

  // NOTE: Not testing indices because they are currently useless in Hive
  // NOTE: Not testing macros because as far as I know no one uses them

  @Test
  public void function() throws SQLException {
    final String func1 = "odpi_sql_func1";
    final String func2 = "odpi_sql_func2";
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("create temporary function " + func1 +
          " as 'org.apache.hadoop.hive.ql.udf.UDFToInteger'");
      stmt.execute("drop temporary function " + func1);

      stmt.execute("drop function if exists " + func2);

      stmt.execute("create function " + func2 +
          " as 'org.apache.hadoop.hive.ql.udf.UDFToInteger'");
      stmt.execute("drop function " + func2);
    }
  }

  // NOTE: Not testing grant/revoke/roles as different vendors use different security solutions
  // and hence different things will work here.


}





