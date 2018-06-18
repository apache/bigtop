
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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.hive.service.cli.HiveSQLException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class HiveJdbcGeneralTest extends TestMethods {

  @Test // (expected=java.sql.SQLDataException.class)
  public void testTableCreation()
      throws SQLException, ClassNotFoundException, InstantiationException,
      IllegalAccessException, IOException, URISyntaxException,
      FileNotFoundException, ParserConfigurationException, SAXException {
    final File f = new File(HiveJdbcGeneralTest.class.getProtectionDomain()
        .getCodeSource().getLocation().getPath());
    // String jdbcDriver = propertyValue("hive-site.xml",
    // "javax.jdo.option.ConnectionDriverName");
    String hivePort = System.getenv("HIVE_PORT");
    // String qualifiedName = propertyValue("hdfs-site.xml",
    // "dfs.internal.nameservices");
    // String[] haNodes = propertyValue("hdfs-site.xml",
    // "dfs.ha.namenodes."+qualifiedName).split(",");
    // String primaryNode = haNodes[0];
    String hdfsConnection =
        propertyValue("hdfs-site.xml", "dfs.namenode.rpc-address");
    String jdbcConnection = System.getenv("HIVE_JDBC_URL");

    Connection con;
    String username = System.getenv("HIVE_USER");
    String password = System.getenv("HIVE_PASSWORD");
    Properties connectionProps = new Properties();
    connectionProps.put("user", username);
    connectionProps.put("password", password);
    Class.forName("org.apache.hive.jdbc.HiveDriver");
    con = DriverManager.getConnection(
        jdbcConnection + ":" + hivePort + "/default;UseNativeQuery=0",
        connectionProps);
    Statement stmt = con.createStatement();
    String newTableName = "btest";
    String columnNames =
        "Flight int, Dates varchar(255), Depart varchar(10), Orig varchar(5), Dest varchar(5), Miles int, Boarded int, Capacity int";
    String localFilepath = f + "/samdat1.csv";
    String HdfsURI = "hdfs://" + hdfsConnection;
    String filePath = "/tmp/htest/00000_";
    String fileDestination = HdfsURI + filePath;
    int columnVerificationNumber = 3; // The column used to verify the
    // result set data
    assertFalse(con.getMetaData().supportsRefCursors());
    getTables(con, newTableName);
    dropTable(stmt, newTableName);
    dropTable(stmt, newTableName + "NT");
    dropTable(stmt, newTableName + "T");
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
    showTables(stmt, "show tables like 'b*'");
    loadFile(localFilepath, HdfsURI, fileDestination + ".txt");
    loadData(stmt, filePath + ".txt", newTableName);
    describeTable(stmt, newTableName);
    updateTable(stmt, "Insert into table btestt SELECT * from btest");
    deleteFile(stmt, filePath + ".txt", HdfsURI);
    deleteFile(stmt, filePath + "0.orc", HdfsURI);
    assertEquals(
        printResults(stmt, "Select * from btest", columnVerificationNumber),
        "20:22");
    assertEquals(printResults(stmt,
        "Select * from btest where Dest = 'LAX' order by boarded desc",
        columnVerificationNumber), "7:10");
    assertEquals(printResults(stmt,
        "Select * from btest where boarded between 160 and 180 order by boarded asc",
        columnVerificationNumber), "12:19");
    assertEquals(
        printResults(stmt, "Select * from btest where Dest='LAX' or Dest='ORD'",
            columnVerificationNumber),
        "10:43");
    assertEquals(printResults(stmt,
        "Select * from btest where Dest= 'LAX' and boarded >= 180",
        columnVerificationNumber), "7:10");
    assertEquals(printResults(stmt,
        "Select * from btest where Dest ='LAX' and boarded = 197",
        columnVerificationNumber), "7:10");
    assertEquals(preparedStatement(con,
        "Select * from btest where Dest ='LAX' and boarded = 197",
        columnVerificationNumber), "7:10");
    assertEquals(setFetchSizeStatement(stmt), 15);
    assertEquals(setFetchSizePreparedStatement(con), 15);
    // assertEquals(callableStatement(con, -20), -1);
    printResults(stmt, "Select * from btest", columnVerificationNumber);
    assertEquals(
        updateTable(stmt, "Update btestt set Orig= 'test' where Dest= 'LAX'"),
        0);
    try {
      loadData(stmt, filePath + ".txt", newTableName+"T");
      fail("shouldn't get here");
    } catch (HiveSQLException e) {
    }
    printResults(stmt, "Select * from btestt order by Dest",
        columnVerificationNumber);
    dropTable(stmt, newTableName);
    dropTable(stmt, newTableName + "NT");
    dropTable(stmt, newTableName + "T");
    setNegativeFetchSize(stmt);
    con.close();
    // Drop the table if it's already there
    // See if show tables works
    // Create the blank table
    // Reprint the list of tables
    // Load File into HDFS
    // load data into table
    // describe table
    // Delete Uploaded File
    // Print table contents with various queries
  }

}
