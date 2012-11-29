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

class TestSqoopImport {
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
  private static final String OUTPUT = System.getProperty("output.dir", "output-dir");
  private static Shell sh = new Shell("/bin/bash -s");

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -test -e $OUTPUT");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $OUTPUT");
      assertTrue("Deletion of previous $OUTPUT from HDFS failed",
          sh.getRet() == 0);
    }
    sh.exec("hadoop fs -mkdir $OUTPUT");
    assertTrue("Could not create $OUTPUT directory", sh.getRet() == 0);
    // unpack resource
    JarContent.unpackJarContainer(TestSqoopImport.class, '.' , null)
    // create the database
    sh.exec("cat $DATA_DIR/mysql-create-db.sql | $MYSQL_COMMAND");
    //create tables
    sh.exec("cat $DATA_DIR/mysql-create-tables.sql | $MYSQL_COMMAND");
    //populate data
    sh.exec("cat $DATA_DIR/mysql-insert-data.sql | $MYSQL_COMMAND");
  }

  @AfterClass
  static void tearDown() {
    if ('YES'.equals(System.getProperty('delete.testdata','no').toUpperCase())) {
      sh.exec("hadoop fs -test -e $OUTPUT");
      if (sh.getRet() == 0) {
        sh.exec("hadoop fs -rmr -skipTrash $OUTPUT");
        assertTrue("Deletion of $OUTPUT from HDFS failed",
            sh.getRet() == 0);
      }
    }
  }

  @Test
  public void testBooleanImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table t_bool --target-dir $OUTPUT/t_bool");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/t_bool/part-* > t_bool.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_bool.out t_bool.out").getRet());
  }

  
  @Test
  public void testIntegerImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table t_int --target-dir $OUTPUT/t_int");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/t_int/part-* > t_int.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_int.out t_int.out").getRet());
  }

  @Test
  public void testFixedPointFloatingPointImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table t_fp --target-dir $OUTPUT/t_fp");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/t_fp/part-* > t_fp.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_fp.out t_fp.out").getRet());
  }

  @Test
  public void testDateTimeImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table t_date --target-dir $OUTPUT/t_date");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/t_date/part-* > t_date.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_date.out t_date.out").getRet());
  }

  @Test
  public void testStringImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table t_string --target-dir $OUTPUT/t_string");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/t_string/part-* > t_string.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_string.out t_string.out").getRet());
  }
  
  @Test
  public void testAppendImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --target-dir $OUTPUT/append");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    //import again with append
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --append --target-dir $OUTPUT/append");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/append/part-* > append.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-append.out append.out").getRet());
  }
  
  @Test
  public void testColumnsImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --columns id,fname --target-dir $OUTPUT/columns");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/columns/part-* > columns.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-columns.out columns.out").getRet());
  }

  @Test
  public void testDirectImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --direct --target-dir $OUTPUT/direct");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/direct/part-* > direct.out");    
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable.out direct.out").getRet());
  }
  
  @Test
  public void testNumMappersImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --num-mappers 1 --target-dir $OUTPUT/num-mappers");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/num-mappers/part-*0 > num-mappers.out");    
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable.out num-mappers.out").getRet());
  }

  @Test
  public void testQueryImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --query 'select t1.id as id, t2.fname as fname from testtable as t1 join testtable2 as t2 on (t1.id = t2.id        ) where t1.id < 3 AND \$CONDITIONS' --split-by t1.id --target-dir $OUTPUT/query");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/query/part-* > query.out");    
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-query.out query.out").getRet());
  }

  @Test
  public void testSplityByImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --split-by fname --target-dir $OUTPUT/split-by");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/split-by/part-* > split-by.out");    
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable.out split-by.out").getRet());
  }

  @Test
  public void testWarehouseDirImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --warehouse-dir $OUTPUT/warehouse-dir");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/warehouse-dir/testtable/part-* > warehouse-dir.out");    
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable.out warehouse-dir.out").getRet());
  }

  @Test
  public void testWhereClauseImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --where \"id < 5\" --target-dir $OUTPUT/where-clause");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);    
    sh.exec("hadoop fs -cat $OUTPUT/where-clause/part-* > where-clause.out");    
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-where-clause.out where-clause.out").getRet());
  }

  @Test
  public void testNullStringImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testnullvalues --null-string mynullstring --target-dir $OUTPUT/null-string");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/null-string/part-* > null-string.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-null-string.out null-string.out").getRet());
  }

  @Test
  public void testNullNonStringImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testnullvalues --null-non-string 10 --target-dir $OUTPUT/non-null-string");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/non-null-string/part-* > non-null-string.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-null-non-string.out non-null-string.out").getRet());
  }
  
  //database name is hardcoded here 
  @Test
    public void testImportAllTables() {
    String SQOOP_CONNECTION_IMPORT_ALL =
    "--connect jdbc:mysql://$MYSQL_HOST/mysqltestdb2 --username=$MYSQL_USER" +
    (("".equals(MYSQL_PASSWORD)) ? "" : " --password=$MYSQL_PASSWORD");

    sh.exec("sqoop import-all-tables $SQOOP_CONNECTION_IMPORT_ALL --warehouse-dir $OUTPUT/alltables");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/alltables/testtable*/part-* > all-tables.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-all-tables.out all-tables.out").getRet());
  }
}

