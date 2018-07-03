
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.hive.service.cli.HiveSQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * THIS CLASS TESTS THE FOLLOWING ASPECTS OF HIVE:
 *
 * Connect to hive; Drop the tables if already there; Test Show Tables; Create
 * blank tables, both transactional and non, along with ORC format and
 * partitions; Reprint the list of tables; Load File into HDFS; Load data into
 * tables; describe tables; Delete Uploaded File; Print table contents with
 * various queries; Test Prepared and Callable Statements; Test Fetch Sizes;
 * Test ACID (UPDATE/INSERT/DELETE); View Partitions;
 */
public class HiveJdbcGeneralTest extends TestMethods {

  static String hivePort = System.getenv("HIVE_PORT");
  static String jdbcConnection = System.getenv("HIVE_JDBC_URL");
  static Connection con;
  String newTableName = "btest";

  @BeforeClass
  public static void onTimeSetup() throws Exception {
    String username = System.getenv("HIVE_USER");
    String password = System.getenv("HIVE_PASSWORD");
    Properties connectionProps = new Properties();
    connectionProps.put("user", username);
    connectionProps.put("password", password);
    Class.forName("org.apache.hive.jdbc.HiveDriver");
    con = DriverManager.getConnection(
        jdbcConnection + ":" + hivePort + "/default;", connectionProps);
  }

  @AfterClass
  public static void oneTimeTearDown() throws Exception {
    con.close();
  }

  @Test // (expected=java.sql.SQLDataException.class)
  public void testHive() throws Exception {
    final File f = new File(HiveJdbcGeneralTest.class.getProtectionDomain()
        .getCodeSource().getLocation().getPath());
    // String jdbcDriver = propertyValue("hive-site.xml",
    // "javax.jdo.option.ConnectionDriverName");

    // String qualifiedName = propertyValue("hdfs-site.xml",
    // "dfs.internal.nameservices");
    // String[] haNodes = propertyValue("hdfs-site.xml",
    // "dfs.ha.namenodes."+qualifiedName).split(",");
    // String primaryNode = haNodes[0];
    String hdfsConnection =
        propertyValue("hdfs-site.xml", "dfs.namenode.rpc-address");
    try (Statement stmt = con.createStatement()) {
      String columnNames =
          "(Flight int, Dates varchar(255), Depart varchar(10), Orig varchar(5), Dest varchar(5), Miles int, Boarded int, Capacity double)";
      String partitionedColumns =
          "(Flight int, Dates varchar(255), Depart varchar(10), Orig varchar(5), Dest varchar(5), Miles int, Boarded int)";
      String localFilepath = f + "/samdat1.csv";
      String HdfsURI = "hdfs://" + hdfsConnection;
      String filePath = "/tmp/htest/00000_";
      String fileDestination = HdfsURI + filePath;
      assertFalse(con.getMetaData().supportsRefCursors());
      assertTrue(con.getMetaData().allTablesAreSelectable());
      assertEquals("Apache Hive", con.getMetaData().getDatabaseProductName());
      System.out.println(
          "Hive Version: " + con.getMetaData().getDatabaseMajorVersion() + "."
              + con.getMetaData().getDatabaseMinorVersion());
      getTables(con, newTableName);
      dropTable(stmt, newTableName);
      dropTable(stmt, newTableName + "NT");
      dropTable(stmt, newTableName + "T");
      dropTable(stmt, newTableName + "P");
      dropTable(stmt, newTableName + "V");
      showTables(stmt, "show tables like 'b*'");
      createTable(stmt, newTableName, columnNames, ",", "");
      try {
        createTable(stmt, newTableName + "NT", columnNames, ",",
            "TBLPROPERTIES(\"transactional\"=\"true\")");
        fail("shouldn't get here");
      } catch (SQLException e) {

      }
      createTable(stmt, newTableName + "T", columnNames, ",",
          "STORED AS ORC TBLPROPERTIES(\"transactional\"=\"true\")");
      createPartitionedTable(stmt, newTableName + "P", partitionedColumns,
          "(Capacity double)", ",", "STORED AS ORC");
      createTable(stmt, newTableName + "V", "(Dest varchar(5))", ",",
          "STORED AS ORC");
      showTables(stmt, "show tables like 'b*'");
      loadFile(localFilepath, HdfsURI, fileDestination + ".txt");
      loadData(stmt, filePath + ".txt", newTableName);
      describeTable(stmt, newTableName);
      updateTable(stmt, "Insert into table btestt SELECT * from btest");
      updateTable(stmt, "Insert into table btestv SELECT Dest from btest");
      updateTable(stmt,
          "Insert into table btestp PARTITION (Capacity) SELECT * from btest");
      deleteFile(stmt, filePath + ".txt", HdfsURI);
      deleteFile(stmt, filePath + "0.orc", HdfsURI);
      assertEquals("302", printResults(stmt, "Select * from btest"));
      assertEquals("114", printResults(stmt,
          "Select * from btest where Dest = 'LAX' order by boarded desc"));
      assertEquals("622", printResults(stmt,
          "Select * from btest where boarded between 160 and 180 order by boarded asc"));
      assertEquals("202", printResults(stmt,
          "Select * from btest where Dest='LAX' or Dest='ORD'"));
      assertEquals("114", printResults(stmt,
          "Select * from btest where Dest= 'LAX' and boarded >= 180"));
      assertEquals("114", printResults(stmt,
          "Select * from btest where Dest ='LAX' and boarded = 197"));
      assertEquals("219",
          printResults(stmt, "Select * from btest LIMIT 10 --"));
      assertEquals("114", preparedStatement(con,
          "Select * from btest where Dest ='LAX' and boarded = 197"));
      assertEquals(15, setFetchSizeStatement(stmt));
      assertEquals(15, setFetchSizePreparedStatement(con));
      // assertEquals(callableStatement(con, -20), -1);
      assertEquals("95487", printResults(stmt, "Select SUM(Miles) from btest"));
      assertEquals("302", printResults(stmt, "Select * from btest"));
      assertEquals(0, updateTable(stmt,
          "Update btestt set Orig= 'test' where Dest= 'LAX'"));
      try {
        loadData(stmt, filePath + ".txt", newTableName + "T");
        fail("shouldn't get here");
      } catch (HiveSQLException e) {
        System.out.println("File does not exist in specified location");
      }
      assertEquals("132",
          printResults(stmt, "Select * from btestt order by Dest"));
      assertEquals("capacity=250.0",
          printResults(stmt, "show partitions btestp"));
      assertEquals("LAX", printResults(stmt,
          "select MIN(Dest), boarded from btest where Dest ='LAX' group by Dest, boarded"));
      assertEquals("19", printResults(stmt,
          "select count(*) from btest where capacity = 250.0 group by capacity"));
      executeStatement(stmt, "Drop view testview");
      executeStatement(stmt,
          "Create view testview AS select SUM(btest.boarded) BOARDED from btest, btestv where btest.Dest=btestv.Dest");
      assertEquals("45072", printResults(stmt, "Select * from testview"));
      executeStatement(stmt, "Drop view testview");
      printResults(stmt, "Describe formatted btest");
      dropTable(stmt, newTableName);
      dropTable(stmt, newTableName + "NT");
      dropTable(stmt, newTableName + "T");
      dropTable(stmt, newTableName + "P");
      dropTable(stmt, newTableName + "V");
      setNegativeFetchSize(stmt);
    }
  }

  @Test
  public void testFunctionsandMetaData() throws Exception {

    String queryValues = "insert into table btestm values"
        + " ('Alfred', 'M', 69.0, 122.5, 'AJH', 1),"
        + " ('Alfred', 'M', 71.0, 130.5, 'AJH', 2),"
        + " ('Alicia', 'F', 56.5, 84.0, 'BJH', 1),"
        + " ('Alicia', 'F', 60.5, 86.9, 'BJH', 2),"
        + " ('Benicia', 'F', 65.3, 98.0, 'BJH', 1),"
        + " ('Benicia', 'F', 69.3, 99.1, 'BJH', 2),"
        + " ('Bennett', 'F', 63.2, 96.2, 'AJH', 1),"
        + " ('Bennett', 'F', 69.2, 98.2, 'AJH', 2),"
        + " ('Carol', 'F', 62.8, 102.5, 'BJH', 1),"
        + " ('Carol', 'F', 65.3, 105.4, 'BJH', 2),"
        + " ('Carlos', 'M', 63.7, 102.9, 'AJH', 1),"
        + " ('Carlos', 'M', 70.3, 106.9, 'AJH', 2),"
        + " ('Henry', 'M', 63.5, 102.5, 'AJH', 1),"
        + " ('Henry', 'M', 68.9, 108.6, 'AJH', 2),"
        + " ('Jaime', 'M', 57.3, 86.0, 'BJH', 1),"
        + " ('Jaime', 'M', 62.9, 90.0, 'BJH', 2),"
        + " ('Janet', 'F', 59.8, 84.5, 'AJH', 1),"
        + " ('Janet', 'F', 62.5, 86.5, 'AJH', 2),"
        + " ('Jean', 'M', 68.2, 113.4, 'AJH', 1),"
        + " ('Jean', 'M', 70.3, 116.0, 'AJH', 2),"
        + " ('Joyce', 'M', 51.3, 50.5, 'BJH', 1),"
        + " ('Joyce', 'M', 55.5, 53.5, 'BJH', 2),"
        + " ('Luc', 'M', 66.3, 77.0, 'AJH', 1),"
        + " ('Luc', 'M', 69.3, 82.9, 'AJH', 2),"
        + " ('Marie', 'F', 66.5, 112.0, 'BJH', 1),"
        + " ('Marie', 'F', 69.5, 114.9, 'BJH', 2),"
        + " ('Medford', 'M', 64.9, 114.0, 'AJH', 1),"
        + " ('Medford', 'M', NULL, NULL, NULL, NULL),"
        + " ('Philip', 'M', 69.0, 115.0, 'AJH', 1),"
        + " ('Philip', 'M', 70.0, 118.0, 'AJH', 2),"
        + " ('Robert', 'M', 64.8, 128.0, 'BJH', 1),"
        + " ('Robert', 'M', 68.3, NULL, 'BJH', 2),"
        + " ('Thomas', 'M', 57.5, 85.0, 'AJH', 1),"
        + " ('Thomas', 'M', 59.1, 92.3, 'AJH', 2),"
        + " ('Wakana', 'F', 61.3, 99.0, 'AJH', 1),"
        + " ('Wakana', 'F', 63.8, 102.9, 'AJH', 2),"
        + " ('William', 'M', 66.5, 112.0, 'BJH', 1),"
        + " ('William', 'M', 68.3, 118.2, 'BJH', 2)";

    // failing query from SAS Institute, code generated by PROC MEANS
    String failingQuery =
        " select COUNT(*) as ZSQL1, MIN(TXT_1.`school`) as ZSQL2, MIN(TXT_1.`time`) as"
            + " ZSQL3, COUNT(*) as ZSQL4, COUNT(TXT_1.`height`) as ZSQL5, MIN(TXT_1.`height`)"
            + " as ZSQL6, MAX(TXT_1.`height`) as ZSQL7, SUM(TXT_1.`height`) as ZSQL8,"
            + " COALESCE(VAR_SAMP(TXT_1.`height`)*(COUNT(TXT_1.`height`)-1),0) as ZSQL9,"
            + " COUNT(TXT_1.`weight`) as ZSQL10, MIN(TXT_1.`weight`) as ZSQL11,"
            + " MAX(TXT_1.`weight`) as ZSQL12, SUM(TXT_1.`weight`) as ZSQL13,"
            + " COALESCE(VAR_SAMP(TXT_1.`weight`)*(COUNT(TXT_1.`weight`)-1),0) as ZSQL14 from"
            + " `btestm` TXT_1 where TXT_1.`school` = 'AJH' group by"
            + " TXT_1.`school`, TXT_1.`time`";

    try (Statement stmt = con.createStatement()) {
      dropTable(stmt, newTableName + "M");
      stmt.executeUpdate(
          "create table btestm (name varchar(8), sex varchar(8), height double, weight double, school varchar(8), `time` double) TBLPROPERTIES('SASFMT:t'='TIME(10.0)')");
      stmt.executeUpdate(queryValues);

      try (ResultSet res = stmt.executeQuery(failingQuery)) {
        assertTrue(res.next());
        assertEquals(null, res.getString(2));
        ResultSetMetaData rsmd = res.getMetaData();
        assertEquals(19, rsmd.getPrecision(1));
        assertEquals(-5, rsmd.getColumnType(1));
        assertEquals(14, rsmd.getColumnCount());
        printResults(stmt, "describe formatted btestm");
        ResultSet res2 = stmt.executeQuery("describe formatted btestm");
        while (res2.next()) {
          String section = res2.getString(1);
          if (section.contains("Detailed"))
            break;
        }
        boolean found = false;
        while (res2.next()) {
          String value = res2.getString(2);
          if (value != null && value.contains("SASFMT"))

          {
            assertEquals("SASFMT:t", res2.getString(2).trim());
            assertEquals("TIME(10.0)", res2.getString(3).trim());
            found = true;
          }
        }
        assertTrue(found);
      }
      dropTable(stmt, newTableName + "M");
    }
  }

  @Test
  public void testFrequency() throws Exception {

    String queryValues =
        "insert into table btestf values (1, 'blue', 'fair', 23),  (1, 'blue', 'red', 7),  (1, 'blue', 'medium', 24),"
            + "(1, 'blue', 'dark', 11), (1, 'green', 'fair', 19),  (1, 'green', 'red', 7),"
            + "(1, 'green', 'medium', 18), (1, 'green', 'dark', 14),  (1, 'brown', 'fair', 34),"
            + "(1, 'brown', 'red', 5), (1, 'brown', 'medium', 41),  (1, 'brown', 'dark', 40),"
            + "(1, 'brown', 'black', 3), (2, 'blue', 'fair', 46), (2, 'blue', 'red', 21),"
            + "(2, 'blue', 'medium', 44), (2, 'blue', 'dark', 40), (2, 'blue', 'black', 6),"
            + "(2, 'green', 'fair', 50), (2, 'green', 'red', 31), (2, 'green', 'medium', 37),"
            + "(2, 'green', 'dark', 23), (2, 'brown', 'fair', 56), (2, 'brown', 'red', 42),"
            + "(2, 'brown', 'medium', 53), (2, 'brown', 'dark', 54), (2, 'brown', 'black', 13)"
            + "";

    String freqQuery =
        "select SUM(TXT_1.`count`) as ZSQL1, MIN(TXT_1.`count`) as ZSQL2, case  when COUNT(*) > COUNT(TXT_1.`eyes`) then ' ' else MIN(TXT_1.`eyes`) end as ZSQL3, case  when COUNT(*) > COUNT(TXT_1.`hair`) then ' ' else MIN(TXT_1.`hair`) end as ZSQL4 from `btestf` TXT_1 where (TXT_1.`count` is not null) and (TXT_1.`count` <> 0) group by TXT_1.`eyes`, TXT_1.`hair`";

    try (Statement stmt = con.createStatement()) {
      dropTable(stmt, newTableName + "F");
      stmt.executeUpdate(
          "create table btestf (region int, eyes varchar(8), hair varchar(8), count int)");
      stmt.executeUpdate(queryValues);
      assertEquals("55", printResults(stmt, freqQuery));
      dropTable(stmt, newTableName + "F");
    }
  }

  @Test
  public void testRank() throws Exception {

    String queryValues = "insert into table btestr values ('Davis', 77, 84),"
        + "('Orlando', 93, 80)," + "('Ramey', 68, 72)," + "('Roe', 68, 75),"
        + "('Sanders', 56, 79)," + "('Simms', 68, 77),"
        + "('Strickland', 82, 79)";

    String rankQuery =
        "WITH `subquery0` AS ( SELECT `name` AS `name`, `present` AS `present`, `taste` AS `taste` FROM btestr ) SELECT `table0`.`name`, `table0`.`present`, `table0`.`taste`, `table1`.`rankalias0` AS `PresentRank`, `table2`.`rankalias1` AS `TasteRank` FROM `subquery0` AS `table0` LEFT JOIN ( SELECT DISTINCT `present`, `rankalias0` FROM ( SELECT `present`, `tempcol0` AS `rankalias0` FROM ( SELECT `present`, MIN( `tempcol1` ) OVER ( PARTITION BY `present` ) AS `tempcol0` FROM( SELECT `present`, CAST( ROW_NUMBER() OVER ( ORDER BY `present` DESC ) AS DOUBLE ) AS `tempcol1` FROM `subquery0` WHERE ( ( `present` IS NOT NULL ) ) ) AS `subquery3` ) AS `subquery1` ) AS subquery2 ) AS `table1` ON ( ( `table0`.`present` = `table1`.`present` ) ) LEFT JOIN ( SELECT DISTINCT `taste`, `rankalias1` FROM ( SELECT `taste`, `tempcol2` AS `rankalias1` FROM ( SELECT `taste`, MIN( `tempcol3` ) OVER ( PARTITION BY `taste` ) AS `tempcol2` FROM( SELECT `taste`, CAST( ROW_NUMBER() OVER ( ORDER BY `taste` DESC ) AS DOUBLE ) AS `tempcol3` FROM `subquery0` WHERE ( ( `taste` IS NOT NULL ) ) ) AS `subquery6` ) AS `subquery4` ) AS subquery5 ) AS `table2` ON ( ( `table0`.`taste` = `table2`.`taste` ) )";

    try (Statement stmt = con.createStatement()) {
      dropTable(stmt, newTableName + "R");
      stmt.executeUpdate(
          "create table btestr (name varchar(10), present int, taste int)");
      stmt.executeUpdate(queryValues);
      assertEquals("Strickland", printResults(stmt, rankQuery));
      dropTable(stmt, newTableName + "R");
    }
  }

  @Test
  public void testSort() throws Exception {

    String queryValues =
        "insert into table btests values ('Paul''s Pizza', 83.00, 1019, 'Apex'),"
            + "('World Wide Electronics', 119.95, 1122, 'Garner'),"
            + "('Strickland Industries', 657.22, 1675, 'Morrisville'),"
            + "('Ice Cream Delight', 299.98, 2310, 'Holly Springs'),"
            + "('Watson Tabor Travel', 37.95, 3131, 'Apex'),"
            + "('Boyd & Sons Accounting', 312.49, 4762, 'Garner'),"
            + "('Bob''s Beds', 119.95, 4998, 'Morrisville'),"
            + "('Tina''s Pet Shop', 37.95, 5108, 'Apex'),"
            + "('Elway Piano and Organ', 65.79, 5217, 'Garner'),"
            + "('Tim''s Burger Stand', 119.95, 6335, 'Holly Springs'),"
            + "('Peter''s Auto Parts', 65.79, 7288, 'Apex'),"
            + "('Deluxe Hardware', 467.12, 8941, 'Garner'),"
            + "('Pauline''s Antiques', 302.05, 9112, 'Morrisville'),"
            + "('Apex Catering', 37.95, 9923, 'Apex')";

    String sortQuery =
        "WITH `subquery0` AS ( SELECT `accountnumber` AS `accountnumber`, `company` AS `company`, `debt` AS `debt`, `town` AS `town` FROM btests ) SELECT `table0`.`company`, `table0`.`debt`, `table0`.`accountnumber`, `table0`.`town` FROM ( SELECT `accountnumber`, `company`, `debt`, `town` FROM ( SELECT `accountnumber`, `company`, `debt`, `town`, ROW_NUMBER() OVER ( PARTITION BY `town` ORDER BY CASE WHEN `town` IS NULL THEN 0 ELSE 1 END, `town` ) AS `tempcol0` FROM `subquery0` ) AS `subquery1` WHERE ( `tempcol0` = 1 ) ) AS `table0` ORDER BY CASE WHEN `table0`.`town` IS NULL THEN 0 ELSE 1 END, `table0`.`town`";

    try (Statement stmt = con.createStatement()) {
      dropTable(stmt, newTableName + "S");
      stmt.executeUpdate(
          "create table btests (company varchar(22), debt double, accountnumber double, town varchar(13))");
      stmt.executeUpdate(queryValues);
      assertEquals("Paulines Antiques", printResults(stmt, sortQuery));
      dropTable(stmt, newTableName + "S");
    }
  }

  @Test
  public void testSpecifiedLocation() throws Exception {

    String queryValues = "insert into table `class` values"
        + "('Alfred', 'M', 14, 69, 112.5)," + "('Alice', 'F', 13, 56.5, 84),"
        + "('Barbara', 'F', 13, 65.3, 98)," + "('Carol', 'F', 14, 62.8, 102.5),"
        + "('Henry', 'M', 14, 63.5, 102.5)," + "('James', 'M', 12, 57.3, 83),"
        + "('Jane', 'F', 12, 59.8, 84.5)," + "('Janet', 'F', 15, 62.5, 112.5),"
        + "('Jeffrey', 'M', 13, 62.5, 84)," + "('John', 'M', 12, 59, 99.5),"
        + "('Joyce', 'F', 11, 51.3, 50.5)," + "('Judy', 'F', 14, 64.3, 90),"
        + "('Louise', 'F', 12, 56.3, 77)," + "('Mary', 'F', 15, 66.5, 112),"
        + "('Philip', 'M', 16, 72, 150)," + "('Robert', 'M', 12, 64.8, 128),"
        + "('Ronald', 'M', 15, 67, 133)," + "('Thomas', 'M', 11, 57.5, 85),"
        + "('William', 'M', 15, 66.5, 112)";

    String locationQuery = "CREATE TABLE test42 ROW FORMAT SERDE"
        + "'org.apache.hadoop.hive.serde2.columnar.ColumnarSerDe' STORED AS RCFILE"
        + " LOCATION '/tmp/test1' AS SELECT  `CLASS`.`age`,"
        + " `CLASS`.`name`, `CLASS`.`sex`, `CLASS`.`height`, `CLASS`.`weight`  FROM"
        + " `CLASS` TBLPROPERTIES(\\\"transactional\\\"=\\\"true\\\")";

    try (Statement stmt = con.createStatement()) {
      dropTable(stmt, "class");
      dropTable(stmt, "test42");
      stmt.executeUpdate(
          "create table `class` (name varchar(8), sex varchar(1), age double precision, height double precision, weight double precision)");
      stmt.executeUpdate(queryValues);
      try {
        stmt.execute(locationQuery);
      } catch (SQLException e) {
      }
      dropTable(stmt, "class");
      dropTable(stmt, "test42");
    }
  }
}
