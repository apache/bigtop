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

package org.apache.itest.sqoop;

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.shell.Shell

class TestSqoopExport {
  private static String mysql_user =
    System.getenv("MYSQL_USER");
  private static String mysql_password =
    System.getenv("MYSQL_PASSWORD");
  private static final String MYSQL_USER =
    (mysql_user == null) ? "root" : mysql_user;
  private static final String MYSQL_PASSWORD =
    (mysql_password == null) ? "" : mysql_password;
  private static final String MYSQL_HOST = System.getProperty("mysql.host", "localhost");
  private static final String HADOOP_HOME =
    System.getenv('HADOOP_HOME');
  private static String streaming_home = System.getenv('STREAMING_HOME');
  private static final String STREAMING_HOME =
    (streaming_home == null) ? HADOOP_HOME + "/contrib/streaming" :
        streaming_home;
  private static final String SQOOP_HOME =
    System.getenv("SQOOP_HOME");
  static {
    assertNotNull("HADOOP_HOME is not set", HADOOP_HOME);
    assertNotNull("SQOOP_HOME is not set", SQOOP_HOME);
    assertNotNull("mysql connector jar is required to be present in $SQOOP_HOME/lib",
      JarContent.getJarName("$SQOOP_HOME/lib", "mysql-connector-java.*.jar"));
  }
  private static String sqoop_jar =
    JarContent.getJarName(SQOOP_HOME, "sqoop-1.*.jar");
  private static String streaming_jar =
    JarContent.getJarName(STREAMING_HOME, "hadoop.*streaming.*.jar");
  static {
    assertNotNull("Can't find sqoop.jar", sqoop_jar);
    assertNotNull("Can't find hadoop-streaming.jar", streaming_jar);
  }
  private static final String SQOOP_JAR = SQOOP_HOME + "/" + sqoop_jar;
  private static final String STREAMING_JAR = STREAMING_HOME + "/" + streaming_jar;
  private static final String MYSQL_COMMAND =
    "mysql --user=$MYSQL_USER" +
    (("".equals(MYSQL_PASSWORD)) ? "" : " --password=$MYSQL_PASSWORD");
  private static final String MYSQL_DBNAME = System.getProperty("mysql.dbname", "mysqltestdb");
  private static final String SQOOP_CONNECTION =
    "--connect jdbc:mysql://$MYSQL_HOST/$MYSQL_DBNAME --username=$MYSQL_USER" +
    (("".equals(MYSQL_PASSWORD)) ? "" : " --password=$MYSQL_PASSWORD");
  static {
    System.out.println("SQOOP_CONNECTION string is " + SQOOP_CONNECTION );
  }
  private static final String DATA_DIR = System.getProperty("data.dir", "mysql-files");
  private static final String INPUT = System.getProperty("input.dir", "input-dir");
  private static Shell sh = new Shell("/bin/bash -s");

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -test -e $INPUT");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $INPUT");
      assertTrue("Deletion of previous $INPUT from HDFS failed",
          sh.getRet() == 0);
    }
    sh.exec("hadoop fs -mkdir $INPUT");
    assertTrue("Could not create $INPUT directory", sh.getRet() == 0);
    // unpack resource
    JarContent.unpackJarContainer(TestSqoopExport.class, '.' , null)
    // upload data to HDFS 
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-testtable.out input-dir/testtable/part-m-00000");
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-t_bool.out $INPUT/t_bool/part-m-00000");
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-t_date-export.out $INPUT/t_date/part-m-00000");
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-t_string.out $INPUT/t_string/part-m-00000");
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-t_fp.out $INPUT/t_fp/part-m-00000");
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-t_int.out $INPUT/t_int/part-m-00000"); 
    
    //create db
    sh.exec("cat $DATA_DIR/mysql-create-db.sql | $MYSQL_COMMAND");
    //create tables
    sh.exec("cat $DATA_DIR/mysql-create-tables.sql | $MYSQL_COMMAND");
  }

  @AfterClass
  static void tearDown() {
    if ('YES'.equals(System.getProperty('delete.testdata','no').toUpperCase())) {
      sh.exec("hadoop fs -test -e $INPUT");
      if (sh.getRet() == 0) {
       // sh.exec("hadoop fs -rmr -skipTrash $INPUT");
        assertTrue("Deletion of $INPUT from HDFS failed",
            sh.getRet() == 0);
      }
    }
  }

  @Test
  public void testBooleanExport() {
    sh.exec("sqoop export $SQOOP_CONNECTION --table t_bool --export-dir $INPUT/t_bool");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("echo 'use mysqltestdb;select * from t_bool' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_bool.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_bool-export.out t_bool.out").getRet());
  }

  
  @Test
  public void testIntegerExport() {
    sh.exec("sqoop export $SQOOP_CONNECTION --table t_int --export-dir $INPUT/t_int");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("echo 'use mysqltestdb;select * from t_int' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_int.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_int.out t_int.out").getRet());
  }

  @Test
  public void testFixedPointFloatingPointExport() {
    sh.exec("sqoop export $SQOOP_CONNECTION --table t_fp --export-dir $INPUT/t_fp");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("echo 'use mysqltestdb;select * from t_fp' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_fp.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_fp.out t_fp.out").getRet());
  }

  @Test
  public void testDateTimeExport() {
    sh.exec("sqoop export $SQOOP_CONNECTION --table t_date --export-dir $INPUT/t_date");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("echo 'use mysqltestdb;select * from t_date' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_date.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_date.out t_date.out").getRet());
  }

  @Test
  public void testStringExport() {
    sh.exec("sqoop export $SQOOP_CONNECTION --table t_string --export-dir $INPUT/t_string");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("echo 'use mysqltestdb;select * from t_string' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_string.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_string.out t_string.out").getRet());
  }

}

