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

package org.apache.bigtop.itest.integration.sqoop

import org.junit.Test
import org.junit.After
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.JarContent
import org.junit.Before
import org.apache.bigtop.itest.shell.Shell

class IntegrationTestSqoopHive {
  private static final String MYSQL_ROOTPW =
    System.getProperty("MYSQL_ROOTPW", "");
  private static final String HADOOP_HOME =
    System.getenv("HADOOP_HOME");
  private static final String SQOOP_HOME =
    System.getenv("SQOOP_HOME");
  private static final String HIVE_HOME =
    System.getenv("HIVE_HOME");
  private static final String ZOOKEEPER_HOME =
    System.getenv("ZOOKEEPER_HOME");

  static {
    // make all validations of the above here
    // check for mysql connector library
    assertNotNull("SQOOP_HOME is unset", SQOOP_HOME);
    assertNotNull("HIVE_HOME is unset", HIVE_HOME);
    assertNotNull("ZOOKEEPER_HOME is unset", ZOOKEEPER_HOME);
    assertNotNull("mysql connector jars are required to be present in $SQOOP_HOME/lib",
      JarContent.getJarName("$SQOOP_HOME/lib", "mysql-connector-java.*.jar"));
  }
  private static final String DATA_DIR = 'hive-sqoop';
  private static final String MYSQL_COMMAND = "mysql";

  private static Shell shell = new Shell("/bin/bash -s");
  def final OUTFILE = 'outfile.txt';

  public static int rmr(String path) {
    Shell rmr = new Shell("/bin/bash -s");
    rmr.exec("hadoop fs -rmr -skipTrash $path");
    return rmr.getRet();
  }

  @Before
  public void setUp() {
    JarContent.unpackJarContainer(IntegrationTestSqoopHive.class, '.', DATA_DIR);

    // MySQL preparations
    rmr('test_table');
    shell.exec("cat $DATA_DIR/mysql-create-db.sql | $MYSQL_COMMAND -u root ${''.equals(MYSQL_ROOTPW) ?: '-p' + MYSQL_ROOTPW}");
    assertEquals('Unable to run mysql-create-db.sql script', 0, shell.getRet());
    shell.exec("cat $DATA_DIR/mysql-load-db.sql | $MYSQL_COMMAND -u testhiveuser testhive");
    assertEquals('Unable to run mysql-load-db.sql script', 0, shell.getRet());
    println "MySQL database prepared for test";
    // Hive preparations
    shell.exec("$HIVE_HOME/bin/hive -f $DATA_DIR/hive-drop-table.hql");
    assertEquals("Unable to run hive-drop-table.hql script",
      0, shell.ret);
  }

  @After
  public void tearDown() {
    File outfile = new File(OUTFILE);
    outfile.deleteOnExit();
    shell.exec("$HIVE_HOME/bin/hive -f $DATA_DIR/hive-drop-table.hql");
    rmr('test_table');
  }

  @Test
  public void hiveSqoop() {
    def hostname = shell.exec('hostname').out.get(0);
    def dbURL = "jdbc:mysql://$hostname/testhive";
    //Run Sqoop Hive import now
    shell.exec("$SQOOP_HOME/bin/sqoop import --connect $dbURL --username root --table test_table --hive-import --verbose");
    assertEquals("Failed to run Sqoop import with Hive", 0, shell.getRet());
    // Verify if it was imported correctly
    shell.exec("${HIVE_HOME}/bin/hive -f $DATA_DIR/hive-select-table.hql > $OUTFILE");
    assertEquals("Unable to run hive-select-table.hql script", 0, shell.ret);
    assertEquals("Hive output did not match expected output. File: $OUTFILE",
      0, shell.exec("diff -u $OUTFILE $DATA_DIR/expected-hive-output.txt").getRet());
  }
}
