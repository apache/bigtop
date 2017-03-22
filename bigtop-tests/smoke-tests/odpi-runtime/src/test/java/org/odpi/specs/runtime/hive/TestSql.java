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
package org.apache.bigtop.itest.hive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;

// This does not test every option that Hive supports, but does try to touch the major
// options, especially anything unique to Hive.  See each test for areas tested and not tested.
public class TestSql extends JdbcConnector {
    private static final Log LOG = LogFactory.getLog(TestSql.class.getName());

    @Test
    public void db() throws SQLException {
        final String db1 = "bigtop_sql_db1";
        final String db2 = "bigtop_sql_db2";
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
        final String table1 = "bigtop_sql_table1";
        final String table2 = "bigtop_sql_table2";
        final String table3 = "bigtop_sql_table3";
        final String table4 = "bigtop_sql_table4";
        final String table5 = "bigtop_sql_table5";

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

            // Not testing SKEWED BY, ROW FORMAT, STORED BY (storage handler

            stmt.execute("create temporary table " + table3 + " like " + table2);

            stmt.execute("insert into " + table1 + " values (3, 'abc'), (4, 'def')");

            stmt.execute("create table " + table4 + " as select a, b from " + table1);

            stmt.execute("truncate table " + table4);

            stmt.execute("alter table " + table4 + " rename to " + table5);
            stmt.execute("alter table " + table2 + " set tblproperties ('c' = 'd')");

            // Not testing alter of clustered or sorted by, because that's suicidal
            // Not testing alter of skewed or serde properties since we didn't test it for create
            // above.

            stmt.execute("drop table " + table1 + " purge");
            stmt.execute("drop table " + table2);
            stmt.execute("drop table " + table3);
            stmt.execute("drop table " + table5);
        }
    }

    @Test
    public void partitionedTable() throws SQLException {
        final String table1 = "bigtop_sql_ptable1";
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

            // Not testing rename partition, exchange partition, msck repair, archive/unarchive,
            // set location, enable/disable no_drop/offline, compact (because not everyone may have
            // ACID on), change column

            stmt.execute("drop table " + table1);

        }
    }

    @Test
    public void view() throws SQLException {
        final String table1 = "bigtop_sql_vtable1";
        final String view1 = "bigtop_sql_view1";
        final String view2 = "bigtop_sql_view2";
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

    // Not testing indices because they are currently useless in Hive
    // Not testing macros because as far as I know no one uses them

    @Test
    public void function() throws SQLException {
        final String func1 = "bigtop_sql_func1";
        final String func2 = "bigtop_sql_func2";
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

    // Not testing grant/revoke/roles as different vendors use different security solutions
    // and hence different things will work here.

    // This covers insert (non-partitioned, partitioned, dynamic partitions, overwrite, with
    // values and select), and multi-insert.  Load is not tested as there's no guarantee that the
    // test machine has access to HDFS and thus the ability to upload a file.
    @Test
    public void insert() throws SQLException {
        final String table1 = "bigtop_insert_table1";
        final String table2 = "bigtop_insert_table2";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("drop table if exists " + table1);
            stmt.execute("create table " + table1 +
                    "(c1 tinyint," +
                    " c2 smallint," +
                    " c3 int," +
                    " c4 bigint," +
                    " c5 float," +
                    " c6 double," +
                    " c7 decimal(8,2)," +
                    " c8 varchar(120)," +
                    " c9 char(10)," +
                    " c10 boolean)" +
                    " partitioned by (p1 string)");

            // insert with partition
            stmt.execute("explain insert into " + table1 + " partition (p1 = 'a') values " +
                    "(1, 2, 3, 4, 1.1, 2.2, 3.3, 'abcdef', 'ghi', true)," +
                    "(5, 6, 7, 8, 9.9, 8.8, 7.7, 'jklmno', 'pqr', true)");

            stmt.execute("set hive.exec.dynamic.partition.mode=nonstrict");

            // dynamic partition
            stmt.execute("explain insert into " + table1 + " partition (p1) values " +
                    "(1, 2, 3, 4, 1.1, 2.2, 3.3, 'abcdef', 'ghi', true, 'b')," +
                    "(5, 6, 7, 8, 9.9, 8.8, 7.7, 'jklmno', 'pqr', true, 'b')");

            stmt.execute("drop table if exists " + table2);

            stmt.execute("create table " + table2 +
                    "(c1 tinyint," +
                    " c2 smallint," +
                    " c3 int," +
                    " c4 bigint," +
                    " c5 float," +
                    " c6 double," +
                    " c7 decimal(8,2)," +
                    " c8 varchar(120)," +
                    " c9 char(10)," +
                    " c10 boolean)");

            stmt.execute("explain insert into " + table2 + " values " +
                    "(1, 2, 3, 4, 1.1, 2.2, 3.3, 'abcdef', 'ghi', true)," +
                    "(5, 6, 7, 8, 9.9, 8.8, 7.7, 'jklmno', 'pqr', true)");

            stmt.execute("explain insert overwrite table " + table2 + " select c1, c2, c3, c4, c5, c6, " +
                    "c7, c8, c9, c10 from " + table1);

            // multi-insert
            stmt.execute("from " + table1 +
                    " insert into table " + table1 + " partition (p1 = 'c') " +
                    " select c1, c2, c3, c4, c5, c6, c7, c8, c9, c10" +
                    " insert into table " + table2 + " select c1, c2, c3, c4, c5, c6, c7, c8, c9, c10");
        }
    }

    // This tests CTEs
    @Test
    public void cte() throws SQLException {
        final String table1 = "bigtop_cte_table1";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("drop table if exists " + table1);
            stmt.execute("create table " + table1 + "(c1 int, c2 varchar(32))");
            stmt.execute("with cte1 as (select c1 from " + table1 + " where c1 < 10) " +
                    " select c1 from cte1");
        }
    }

    // This tests select, including CTEs, all/distinct, single tables, joins (inner & outer),
    // group by (w/ and w/o having), order by, cluster by/distribute by/sort by, limit, union,
    // subqueries, and over.

    @Test
    public void select() throws SQLException {
        final String[] tables = {"bigtop_select_table1", "bigtop_select_table2"};
        try (Statement stmt = conn.createStatement()) {
            for (int i = 0; i < tables.length; i++) {
                stmt.execute("drop table if exists " + tables[i]);
                stmt.execute("create table " + tables[i] + "(c1 int, c2 varchar(32))");
            }

            // single table queries tested above in several places

            stmt.execute("explain select all a.c2, SUM(a.c1), SUM(b.c1) " +
                    "from " + tables[0] + " a join " + tables[1] + " b on (a.c2 = b.c2) " +
                    "group by a.c2 " +
                    "order by a.c2 asc " +
                    "limit 10");

            stmt.execute("explain select distinct a.c2 " +
                    "from " + tables[0] + " a left outer join " + tables[1] + " b on (a.c2 = b.c2) " +
                    "order by a.c2 desc ");

            stmt.execute("explain select a.c2, SUM(a.c1) " +
                    "from " + tables[0] + " a right outer join " + tables[1] + " b on (a.c2 = b.c2) " +
                    "group by a.c2 " +
                    "having SUM(b.c1) > 0 " +
                    "order by a.c2 ");

            stmt.execute("explain select a.c2, rank() over (partition by a.c1) " +
                    "from " + tables[0] + " a full outer join " + tables[1] + " b on (a.c2 = b.c2) ");

            stmt.execute("explain select c2 from " + tables[0] + " union all select c2 from " + tables[1]);

            stmt.execute("explain select * from " + tables[0] + " distribute by c1 sort by c2");
            stmt.execute("explain select * from " + tables[0] + " cluster by c1");

            stmt.execute("explain select * from (select c1 from " + tables[0] + ") t");
            stmt.execute("explain select * from " + tables[0] + " where c1 in (select c1 from " + tables[1] +
                    ")");

        }

    }

    // Update and delete are not tested because not everyone configures their system to run
    // with ACID.


}





