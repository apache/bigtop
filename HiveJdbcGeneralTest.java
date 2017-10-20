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

import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.xml.sax.SAXException;

public class HiveJdbcGeneralTest extends TestMethods {

  @Test
  public void testTableCreation() throws SQLException,
  ClassNotFoundException, InstantiationException,
  IllegalAccessException, IOException, URISyntaxException,
  FileNotFoundException, ParserConfigurationException, SAXException {
    final File f = new File(HiveJdbcGeneralTest.class.getProtectionDomain()
        .getCodeSource().getLocation().getPath());
    // String jdbcDriver = propertyValue("hive-site.xml",
    // "javax.jdo.option.ConnectionDriverName");
    String hivePort = System.getenv("HIVE_PORT");
    String hdfsConnection = propertyValue("hdfs-site.xml",
        "dfs.namenode.rpc-address");
    String jdbcConnection = System.getenv("HIVE_JDBC_URL");

    Connection con;
    String username = System.getenv("HIVE_USER");
    String password = System.getenv("HIVE_PASSWORD");
    Properties connectionProps = new Properties();
    connectionProps.put("user", username);
    connectionProps.put("password", password);
    Class.forName("org.apache.hive.jdbc.HiveDriver");
    con = DriverManager.getConnection(jdbcConnection + ":" + hivePort,
        connectionProps);
    Statement stmt = con.createStatement();
    String newTableName = "btest";
    String columnNames = "Flight int, Dates varchar(255), Depart varchar(10), Orig varchar(5), Dest varchar(5), Miles int, Boarded int, Capacity int";
    String localFilepath = f + "/samdat1.csv";
    String HdfsURI = "hdfs://" + hdfsConnection;
    String fileDestination = HdfsURI + "/tmp/htest/Hadoop_File.txt";
    String filePath = "/tmp/htest/Hadoop_File.txt";
    Path upload = new Path(filePath);
    int columnVerificationNumber = 3; // The column used to verify the
    // result set data

    dropTable(stmt, newTableName);
    showTables(stmt, "show tables like 'b*'");
    createTable(stmt, newTableName, columnNames, ",");
    showTables(stmt, "show tables like 'b*'");
    loadFile(localFilepath, HdfsURI, fileDestination);
    loadData(stmt, filePath, newTableName);
    describeTable(stmt, newTableName);
    deleteFile(stmt, upload, HdfsURI);
    assertEquals(
        printResults(stmt, "Select * from btest",
            columnVerificationNumber), "20:22");
    assertEquals(
        printResults(
            stmt,
            "Select * from btest where Dest = 'LAX' order by boarded desc",
            columnVerificationNumber), "7:10");
    assertEquals(
        printResults(
            stmt,
            "Select * from btest where boarded between 160 and 180 order by boarded asc",
            columnVerificationNumber), "12:19");
    assertEquals(
        printResults(stmt,
            "Select * from btest where Dest='LAX' or Dest='ORD'",
            columnVerificationNumber), "10:43");
    assertEquals(
        printResults(
            stmt,
            "Select * from btest where Dest= 'LAX' and boarded >= 180",
            columnVerificationNumber), "7:10");
    assertEquals(
        printResults(
            stmt,
            "Select * from btest where Dest ='LAX' and boarded = 197",
            columnVerificationNumber), "7:10");
    dropTable(stmt, newTableName);
    // updateTable(stmt, "Update btest set Orig= 'test' where Dest= 'LAX'");
    // printResults(stmt, "Select * from btest");
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
