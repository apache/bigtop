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

import org.apache.sqoop.client.SqoopClient
import org.apache.sqoop.model.MPersistableEntity
import org.apache.sqoop.validation.Status
import org.junit.Ignore

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotSame
import static org.junit.Assert.assertTrue
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.shell.Shell

import org.apache.sqoop.framework.configuration.OutputFormat
import org.apache.sqoop.framework.configuration.StorageType
import org.apache.sqoop.model.MConnection
import org.apache.sqoop.model.MFormList
import org.apache.sqoop.model.MJob
import org.apache.sqoop.model.MSubmission;
import org.junit.experimental.categories.Category;
import org.apache.bigtop.itest.interfaces.EssentialTests;

class TestSqoopImport {
  private static String mysql_user =
    System.getenv("MYSQL_USER");
  private static String mysql_password =
    System.getenv("MYSQL_PASSWORD");
  private static final String MYSQL_USER =
    (mysql_user == null) ? "mytestuser" : mysql_user;
  private static final String MYSQL_PASSWORD =
    (mysql_password == null) ? "password" : mysql_password;
  private static final String MYSQL_HOST = System.getenv("MYSQL_HOST");

  private static final String MYSQL_COMMAND =
    "mysql -h $MYSQL_HOST --user=$MYSQL_USER" +
    (("".equals(MYSQL_PASSWORD)) ? "" : " --password=$MYSQL_PASSWORD");
  private static final String MYSQL_DBNAME = System.getProperty("mysql.dbname", "mysqltestdb");
  private static final String SQOOP_CONNECTION_STRING =
    "jdbc:mysql://$MYSQL_HOST/$MYSQL_DBNAME";
  private static final String SQOOP_CONNECTION =
    "--connect jdbc:mysql://$MYSQL_HOST/$MYSQL_DBNAME --username=$MYSQL_USER" +
    (("".equals(MYSQL_PASSWORD)) ? "" : " --password=$MYSQL_PASSWORD");
  static {
    System.out.println("SQOOP_CONNECTION string is " + SQOOP_CONNECTION );
  }
  private static final String DATA_DIR = System.getProperty("data.dir", "mysql-files");
  private static final String OUTPUT = System.getProperty("output.dir", "/tmp/output-dir");
  private static final String SQOOP_SERVER_URL = System.getenv("SQOOP_URL");
  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell my = new Shell("/bin/bash","root");

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -test -e $OUTPUT");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $OUTPUT");
      assertTrue("Deletion of previous $OUTPUT from HDFS failed",
          sh.getRet() == 0);
    }
    // unpack resource
    JarContent.unpackJarContainer(TestSqoopImport.class, '.' , null)

    // create the database
    sh.exec("sed -i s/MYSQLHOST/$MYSQL_HOST/g $DATA_DIR/mysql-create-user.sql");
    my.exec("mysql test < $DATA_DIR/mysql-create-user.sql");
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

  protected SqoopClient getClient() {
    String sqoopServerUrl = "$SQOOP_SERVER_URL".toString();
    return new SqoopClient(sqoopServerUrl);
  }

  /**
   * Fill connection form based on currently active provider.
   *
   * @param connection MConnection object to fill
   */
  protected void fillConnectionForm(MConnection connection) {
    MFormList forms = connection.getConnectorPart();
    forms.getStringInput("connection.jdbcDriver").setValue("com.mysql.jdbc.Driver");
    forms.getStringInput("connection.connectionString").setValue("$SQOOP_CONNECTION_STRING".toString());
    forms.getStringInput("connection.username").setValue("$MYSQL_USER".toString());
    forms.getStringInput("connection.password").setValue("$MYSQL_PASSWORD".toString());
  }

  /**
   * Fill output form with specific storage and output type. Mapreduce output directory
   * will be set to default test value.
   *
   * @param job MJOb object to fill
   * @param storage Storage type that should be set
   * @param output Output type that should be set
   */
  protected void fillOutputForm(MJob job, StorageType storage, OutputFormat output, String outputDir) {
    MFormList forms = job.getFrameworkPart();
    forms.getEnumInput("output.storageType").setValue(storage);
    forms.getEnumInput("output.outputFormat").setValue(output);
    forms.getStringInput("output.outputDirectory").setValue(outputDir);
  }

  /**
   * Create connection.
   *
   * With asserts to make sure that it was created correctly.
   *
   * @param connection
   */
  protected void createConnection(MConnection connection) {
    assertEquals(Status.FINE, getClient().createConnection(connection));
    assertNotSame(MPersistableEntity.PERSISTANCE_ID_DEFAULT, connection.getPersistenceId());
  }

  /**
   * Create job.
   *
   * With asserts to make sure that it was created correctly.
   *
   * @param job
   */
  protected void createJob(MJob job) {
    assertEquals(Status.FINE, getClient().createJob(job));
    assertNotSame(MPersistableEntity.PERSISTANCE_ID_DEFAULT, job.getPersistenceId());
  }

  protected void runSqoopClient(String tableName=null, String partitionColumn=null, String tableColumns=null, String tableSQL=null, String outputSubdir=null, int extractors=0, int loaders=0) {
    // Connection creation
    MConnection connection = getClient().newConnection(1L);
    fillConnectionForm(connection);
    createConnection(connection);

    // Job creation
    MJob job = getClient().newJob(connection.getPersistenceId(), MJob.Type.IMPORT);

    // Connector values
    MFormList connectorForms = job.getConnectorPart();

    if(tableName != null) {
      connectorForms.getStringInput("table.tableName").setValue(tableName);
    }

    if(partitionColumn != null) {
      connectorForms.getStringInput("table.partitionColumn").setValue(partitionColumn);
    }

    if(tableColumns != null) {
      connectorForms.getStringInput("table.columns").setValue(tableColumns);
    }

    if(tableSQL != null) {
      connectorForms.getStringInput("table.sql").setValue(tableSQL);
    }

    // Framework values
    MFormList frameworkForms = job.getFrameworkPart();

    if(extractors > 0) {
      frameworkForms.getIntegerInput("throttling.extractors").setValue(extractors);
    }

    if(loaders > 0) {
      frameworkForms.getIntegerInput("throttling.loaders").setValue(loaders);
    }

    String outSubdir;
    if(outputSubdir == null) {
      outSubdir = tableName;
    } else {
      outSubdir = outputSubdir;
    }

    fillOutputForm(job, StorageType.HDFS, OutputFormat.TEXT_FILE, "$OUTPUT".toString() + "/" + outSubdir);
    createJob(job);

    MSubmission submission = getClient().startSubmission(job.getPersistenceId());
    assertTrue(submission.getStatus().isRunning());

    // Wait until the job finish - this active waiting will be removed once
    // Sqoop client API will get blocking support.
    while (true) {
      Thread.sleep(5000);
      submission = getClient().getSubmissionStatus(job.getPersistenceId());
      if (!submission.getStatus().isRunning())
        break;
    }
  }

  @Test
  public void testBooleanImport() {
    String tableName = "t_bool";
    String partitionColumn = null;

    runSqoopClient(tableName, partitionColumn);

    sh.exec("hadoop fs -cat $OUTPUT/t_bool/part-* > t_bool.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_bool.out t_bool.out").getRet());
  }



@Category ( EssentialTests.class )
  @Test
  public void testIntegerImport() {
    String tableName = "t_int";
    String partitionColumn = null;

    runSqoopClient(tableName, partitionColumn);

    sh.exec("hadoop fs -cat $OUTPUT/t_int/part-* > t_int.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_int.out t_int.out").getRet());
  }

  @Test
  public void testFixedPointFloatingPointImport() {
    String tableName = "t_fp";
    String partitionColumn = null;

    runSqoopClient(tableName, partitionColumn);

    sh.exec("hadoop fs -cat $OUTPUT/t_fp/part-* > t_fp.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_fp.out t_fp.out").getRet());
  }

@Category ( EssentialTests.class )
  @Test
  public void testDateTimeImport() {
    String tableName = "t_date";
    String partitionColumn = null;

    runSqoopClient(tableName, partitionColumn);

    sh.exec("hadoop fs -cat $OUTPUT/t_date/part-* > t_date.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_date.out t_date.out").getRet());
  }

@Category ( EssentialTests.class )
  @Test
  public void testStringImport() {
    String tableName = "t_string";
    String partitionColumn = null;

    runSqoopClient(tableName, partitionColumn);

    sh.exec("hadoop fs -cat $OUTPUT/t_string/part-* > t_string.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_string.out t_string.out").getRet());
  }

  @Test
  public void testColumnsImport() {
    String tableName = "testtable";
    String partitionColumn = null;
    String tableColumns = "id,fname";

    runSqoopClient(tableName, partitionColumn, tableColumns);

    sh.exec("hadoop fs -cat $OUTPUT/testtable/part-* > columns.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-columns.out columns.out").getRet());
  }

  @Test
  public void testNumMappersImport() {
    String tableName = "testtable";
    String partitionColumn = null;
    String tableColumns = null;
    String tableSQL = null;
    String outputSubdir = "testtable-nummappers";
    int extractors = 1;
    int loaders = 1;

    runSqoopClient(tableName, partitionColumn, tableColumns, tableSQL, outputSubdir, extractors, loaders);

    sh.exec("hadoop fs -cat $OUTPUT/$outputSubdir/part-*0 > num-mappers.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable.out num-mappers.out").getRet());
  }

  @Test
  public void testQueryImport() {
    String tableName = null;
    String partitionColumn = "t1.id";
    String tableColumns = null;
    String tableSQL = 'select t1.id as id, t2.fname as fname from testtable as t1 join testtable2 as t2 on (t1.id = t2.id ) where t1.id < 3 AND \${CONDITIONS}';
    String outputSubdir = "testtable-query";

    runSqoopClient(tableName, partitionColumn, tableColumns, tableSQL, outputSubdir);

    sh.exec("hadoop fs -cat $OUTPUT/$outputSubdir/part-* > query.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-query.out query.out").getRet());
  }

  @Test
  public void testSplitByImport() {
    String tableName = "testtable";
    String partitionColumn = "id";
    String tableColumns = null;
    String tableSQL = null;
    String outputSubdir = "testtable-split";

    runSqoopClient(tableName, partitionColumn, tableColumns, tableSQL, outputSubdir);

    sh.exec("hadoop fs -cat $OUTPUT/$outputSubdir/part-* > split-by.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable2.out split-by.out").getRet());
  }




  //----------------------------------------@Ignore("Backward Compatibility")------------------------------------------//
  // The functionality of the tests below is not currently supported by Sqoop 2.

  //database name is hardcoded here
  @Ignore("Backward Compatibility")
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

  @Ignore("Backward Compatibility")
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

  @Ignore("Backward Compatibility")
  @Test
  public void testDirectImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --direct --target-dir $OUTPUT/direct");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/direct/part-* > direct.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable.out direct.out").getRet());
  }

  @Ignore("Backward Compatibility")
  @Test
  public void testWarehouseDirImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --warehouse-dir $OUTPUT/warehouse-dir");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/warehouse-dir/testtable/part-* > warehouse-dir.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-testtable.out warehouse-dir.out").getRet());
  }

  @Ignore("Backward Compatibility")
  @Test
  public void testWhereClauseImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testtable --where \"id < 5\" --target-dir $OUTPUT/where-clause");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/where-clause/part-* > where-clause.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-where-clause.out where-clause.out").getRet());
  }

  @Ignore("Backward Compatibility")
  @Test
  public void testNullStringImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testnullvalues --null-string mynullstring --target-dir $OUTPUT/null-string");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/null-string/part-* > null-string.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-null-string.out null-string.out").getRet());
  }

  @Ignore("Backward Compatibility")
  @Test
  public void testNullNonStringImport() {
    sh.exec("sqoop import $SQOOP_CONNECTION --table testnullvalues --null-non-string 10 --target-dir $OUTPUT/non-null-string");
    assertTrue("Sqoop job failed!", sh.getRet() == 0);
    sh.exec("hadoop fs -cat $OUTPUT/non-null-string/part-* > non-null-string.out");
    assertEquals("sqoop import did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-null-non-string.out non-null-string.out").getRet());
  }
}
