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
import org.apache.sqoop.model.MConnection
import org.apache.sqoop.model.MFormList
import org.apache.sqoop.model.MJob
import org.apache.sqoop.model.MPersistableEntity
import org.apache.sqoop.model.MSubmission
import org.apache.sqoop.validation.Status;

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNotSame
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
    (mysql_user == null) ? "mytestuser" : mysql_user;
  private static final String MYSQL_PASSWORD =
    (mysql_password == null) ? "password" : mysql_password;
  private static final String MYSQL_HOST = System.getProperty("mysql.host", "localhost");

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
  private static final String INPUT = System.getProperty("input.dir", "/tmp/input-dir");
  private static final String SQOOP_SERVER_URL = System.getProperty("sqoop.server.url", "http://localhost:12000/sqoop/");
  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell my = new Shell("/bin/bash","root");

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -test -e $INPUT");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash $INPUT");
      assertTrue("Deletion of previous $INPUT from HDFS failed",
          sh.getRet() == 0);
    }
    sh.exec("sed -i s/MYSQLHOST/$MYSQL_HOST/g $DATA_DIR/mysql-create-user.sql");
    my.exec("mysql test < $DATA_DIR/mysql-create-user.sql");
    sh.exec("hadoop fs -mkdir $INPUT");
    assertTrue("Could not create $INPUT directory", sh.getRet() == 0);

    sh.exec("hadoop fs -mkdir $INPUT/testtable");
    assertTrue("Could not create $INPUT/testtable directory", sh.getRet() == 0);
    sh.exec("hadoop fs -mkdir $INPUT/t_bool");
    assertTrue("Could not create $INPUT/t_bool directory", sh.getRet() == 0);
    sh.exec("hadoop fs -mkdir $INPUT/t_date");
    assertTrue("Could not create $INPUT/t_date directory", sh.getRet() == 0);
    sh.exec("hadoop fs -mkdir $INPUT/t_string");
    assertTrue("Could not create $INPUT/t_string directory", sh.getRet() == 0);
    sh.exec("hadoop fs -mkdir $INPUT/t_fp");
    assertTrue("Could not create $INPUT/t_fp directory", sh.getRet() == 0);
    sh.exec("hadoop fs -mkdir $INPUT/t_int");
    assertTrue("Could not create $INPUT/t_int directory", sh.getRet() == 0);

    // unpack resource
    JarContent.unpackJarContainer(TestSqoopExport.class, '.' , null)

    // upload data to HDFS 
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-testtable.out $INPUT/testtable/part-m-00000");
    sh.exec("hadoop fs -put $DATA_DIR/sqoop-t_bool.out $INPUT/t_bool/part-m-00000");
    sh.exec("hadoop fs -copyFromLocal $DATA_DIR/sqoop-t_date-export.out $INPUT/t_date/part-m-00000");
    sh.exec("hadoop fs -copyFromLocal $DATA_DIR/sqoop-t_string.out $INPUT/t_string/part-m-00000");
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
        sh.exec("hadoop fs -rmr -skipTrash $INPUT");
        assertTrue("Deletion of $INPUT from HDFS failed",
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
  protected void fillInputForm(MJob job, String inputDir) {
    MFormList forms = job.getFrameworkPart();
    forms.getStringInput("input.inputDirectory").setValue(inputDir);
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

  protected void runSqoopClientExport(String tableName) {
    // Connection creation
    MConnection connection = getClient().newConnection(1L);
    fillConnectionForm(connection);
    createConnection(connection);

    // Job creation
    MJob job = getClient().newJob(connection.getPersistenceId(), MJob.Type.EXPORT);

    // Connector values
    MFormList forms = job.getConnectorPart();
    forms.getStringInput("table.schemaName").setValue("mysqltestdb");
    forms.getStringInput("table.tableName").setValue(tableName);
    // Framework values
    fillInputForm(job, "$INPUT".toString() + "/" + tableName);
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
  public void testDateTimeExport() {
    String tableName = "t_date";

    runSqoopClientExport(tableName);

    sh.exec("echo 'use mysqltestdb;select * from t_date' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_date.out");
    assertEquals("sqoop export did not match with  expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_date-export-com.out t_date.out").getRet());
  }

  @Test
  public void testStringExport() {
    String tableName = "t_string";

    runSqoopClientExport(tableName);

    sh.exec("echo 'use mysqltestdb;select * from t_string' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_string.out");
    assertEquals("sqoop export did not write expected data",
            0, sh.exec("diff -u $DATA_DIR/sqoop-t_string_export.out t_string.out").getRet());
  }


  @Test
  public void testBooleanExport() {
    String tableName = "t_bool";

    runSqoopClientExport(tableName);

    sh.exec("echo 'use mysqltestdb;select * from t_bool' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_bool.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_bool-export.out t_bool.out").getRet());
  }

  @Test
  public void testIntegerExport() {
    String tableName = "t_int";

    runSqoopClientExport(tableName);

    sh.exec("echo 'use mysqltestdb;select * from t_int' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_int.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_int.out t_int.out").getRet());
  }

  @Test
  public void testFixedPointFloatingPointExport() {
    String tableName = "t_fp";

    runSqoopClientExport(tableName);

    sh.exec("echo 'use mysqltestdb;select * from t_fp' | $MYSQL_COMMAND --skip-column-names | sed 's/\t/,/g' > t_fp.out");
    assertEquals("sqoop export did not write expected data",
        0, sh.exec("diff -u $DATA_DIR/sqoop-t_fp.out t_fp.out").getRet());
  }
}
