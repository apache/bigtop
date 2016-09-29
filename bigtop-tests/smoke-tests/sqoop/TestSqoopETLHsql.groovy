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


package org.apache.bigtop.itest.sqoop

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.AfterClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.hsqldb.Server;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.junit.runner.RunWith
import org.hsqldb.persist.HsqlDatabaseProperties;
import org.hsqldb.persist.HsqlProperties;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.PrintWriter;

class TestSqoopHSQLDBETL {
  static private Log LOG = LogFactory.getLog(Object.class);

  static Shell sh = new Shell("/bin/bash -s");
  static final String hsql = "/usr/lib/sqoop/lib/hsqldb-1.8.0.10.jar"
  static final String port = System.getProperty("port", "9001");


  @AfterClass
  public static void tearDown() {
    //sh.exec("hadoop fs -rmr -skipTrash pigsmoketest");
    server.stop();
  }

  static Server server;

  @BeforeClass
  static void setUp() {
    sh.exec("rm -rf /tmp/sqooptest*")
    HsqlProperties p = new HsqlProperties();
    p.setProperty("server.database.0", "file:/tmp/sqooptest");
    // just use default.
    p.setProperty("server.dbname.0", "sqooptest");
    p.setProperty("hsqldb.default_table_type", "TEXT");
    p.setProperty("server.port", port);

    server = new Server();
    server.setProperties(p);
    server.setLogWriter(new PrintWriter(System.out, true));
    // can use custom writer
    server.setErrWriter(new PrintWriter(System.out, true));
    // can use custom writer
    server.start();

  }

  final static String DB_DRIVER = "org.hsqldb.jdbcDriver";
  static String DB_CONNECTION =
      "jdbc:hsqldb:hsql://localhost:$port/sqooptest";
  static String DB_USER = "sa";
  static String DB_PASSWORD = "";


  void execDB(String query) {
    Connection dbConnection = null;
    Statement statement = null;
    try {
      dbConnection = getDBConnection();
      statement = dbConnection.createStatement();
      statement.execute(query);
    }
    catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage() + "")
    }
    finally {
      if (statement != null) statement.close();
      if (dbConnection != null) dbConnection.close();
    }

  }


  @Test
  void test() {
    createDB()
    sh.exec("sqoop import --libjars /usr/lib/sqoop/lib/hsqldb-1.8.0.10.jar --connect \"jdbc:hsqldb:hsql://localhost:$port/sqooptest\" --username \"SA\" --query \'select ANIMALS.* from ANIMALS WHERE \$CONDITIONS\' --split-by ANIMALS.id --target-dir /tmp/sqooptest")
  }

  void createDB() {
    String createTableSQL =
        """
        CREATE TABLE ANIMALS(
         NAME VARCHAR(20) NOT NULL,
         COUNTRY VARCHAR(20) NOT NULL,
         ID INTEGER NOT NULL)
	"""

    execDB(createTableSQL);
    LOG.info("DONE CREATING ANIMALS DB")

    for (i in 1..300) {
      execDB("INSERT INTO ANIMALS (NAME,COUNTRY,ID) VALUES (\'ELEPHANT\',\'SOUTH AFRICA\',1)");
    }
    execDB("INSERT INTO ANIMALS (NAME,COUNTRY,ID) VALUES (\'ELEPHANT\',\'SOUTH AFRICA\',1)");
    execDB("INSERT INTO ANIMALS (NAME,COUNTRY,ID) VALUES (\'KOALA\', \'AUSTRALIA\', 2)");
    execDB("INSERT INTO ANIMALS (NAME,COUNTRY,ID) VALUES (\'ZEBRA\', \'USA\', 3)");

    LOG.info("DONE inserting 3 animals");
  }

  private static Connection getDBConnection() {
    Connection dbConnection = null;
    try {
      Class.forName(DB_DRIVER);
    }
    catch (ClassNotFoundException e) {
      LOG.info(e.getMessage());
      Assert.fail("Driver not found ${DB_DRIVER}")
    }
    try {
      dbConnection = DriverManager.
          getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
      return dbConnection;
    }
    catch (Exception e) {
      Assert.fail("Couldnt get connection ")
    }
    return dbConnection;
  }
}
