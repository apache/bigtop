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

package org.apache.bigtop.itest.integration.sqoop;

import org.apache.bigtop.itest.shell.Shell
import org.junit.Test
import org.junit.Before
import org.apache.bigtop.itest.JarContent
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import org.junit.After
import static org.junit.Assert.assertFalse;

public class IntegrationTestSqoopHBase {
  private static final String MYSQL_ROOTPW =
    System.getProperty("MYSQL_ROOTPW", "");
  private static final String HADOOP_HOME =
    System.getenv("HADOOP_HOME");
  private static final String SQOOP_HOME =
    System.getenv("SQOOP_HOME");
  private static final String HBASE_HOME =
    System.getenv("HBASE_HOME");
  private static final String ZOOKEEPER_HOME =
    System.getenv("ZOOKEEPER_HOME");

  static {
    // make all validations of the above here
    // check for mysql connector library
    assertNotNull("SQOOP_HOME is unset", SQOOP_HOME);
    assertNotNull("HBASE_HOME is unset", HBASE_HOME);
    assertNotNull("ZOOKEEPER_HOME is unset", ZOOKEEPER_HOME);
    assertNotNull("mysql connector jars are required to be present in $SQOOP_HOME/lib",
      JarContent.getJarName("$SQOOP_HOME/lib", "mysql-connector-java.*.jar"));
  }
  private static final String DATA_DIR = 'hbase-sqoop';
//  private static final String MYSQL_COMMAND = "mysql -u root -p${MYSQL_ROOTPW}";
  private static final String MYSQL_COMMAND = "mysql";

  private static Shell shell = new Shell("/bin/bash -s");

  public static int mkdir(String path) {
    Shell test = new Shell("/bin/bash -s");
    test.exec("hadoop fs -mkdir $path");
    return test.getRet();
  }

  public static int rmr(String path) {
    Shell rmr = new Shell("/bin/bash -s");
    rmr.exec("hadoop fs -rmr -skipTrash $path");
    return rmr.getRet();
  }

  public static String mktemps() {
    mkdir("IntegrationTestSqoopHBase-${(new Date().getTime())}");
  }

  @Before
  public void setUp() {
    JarContent.unpackJarContainer(IntegrationTestSqoopHBase.class, '.', DATA_DIR);

    rmr('test_table');
    shell.exec("cat $DATA_DIR/mysql-create-db.sql | $MYSQL_COMMAND -u root ${''.equals(MYSQL_ROOTPW) ?: '-p' + MYSQL_ROOTPW}");
    assertEquals('Unable to run mysql-create-db.sql script', 0, shell.getRet());
    shell.exec("cat $DATA_DIR/mysql-load-db.sql | $MYSQL_COMMAND -u root testhbase");
    assertEquals('Unable to run mysql-load-db.sql script', 0, shell.getRet());
    println "MySQL database prepared for test";

    shell.exec("cat $DATA_DIR/drop-table.hxt | $HBASE_HOME/bin/hbase shell");
    shell.exec("cat $DATA_DIR/create-table.hxt | $HBASE_HOME/bin/hbase shell")
    def out = shell.out.join('\n');

    assertFalse("Unable to create HBase table by script create-table.hxt",
      (out =~ /ERROR/).find());
  }

  @After
  public void tearDown() {
    shell.exec("cat $DATA_DIR/drop-table.hxt | $HBASE_HOME/bin/hbase shell");
    rmr('test_table');
  }

  @Test
  public void hBaseSqoop() {
    def hostname = shell.exec('hostname').out.get(0);
    def dbURL = "jdbc:mysql://$hostname/testhbase";
    def OUTFILE = 'outfile.txt';
    //Run Sqoop HBase import now
    shell.exec("$SQOOP_HOME/bin/sqoop import --connect $dbURL --username root --table test_table --hbase-table test_table --column-family data --verbose");
    assertEquals("Failed to run Sqoop import with HBase", 0, shell.getRet());
    // Verify if it was imported correctly
    shell.exec("cat $DATA_DIR/select-table.hxt| ${HBASE_HOME}/bin/hbase shell | awk '/^ [0-9]+/ { print \$1\"  \"\$4 }' | sort > $OUTFILE");
    shell.exec("sort $DATA_DIR/expected-hbase-output.txt > expected-hbase-output.txt.resorted");
    // TODO need to conver shell callouts to power tools with Java parsing of
    // ' 10                                    column=data:b, timestamp=1301075559859, value=ten'
    assertEquals("HBase scan output did not match expected output. File: $OUTFILE",
      0, shell.exec("diff -u $OUTFILE expected-hbase-output.txt.resorted").getRet());
  }
}
