/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.odpi.specs.runtime.hive;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStore;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.AddPartitionsRequest;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.DropPartitionsRequest;
import org.apache.hadoop.hive.metastore.api.EnvironmentContext;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.RequestPartsSpec;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TestThrift {

  private static ThriftHiveMetastore.Iface client = null;
  private static HiveConf conf;

  private Random rand;

  @BeforeClass
  public static void connect() throws MetaException {
    if (JdbcConnector.testActive(JdbcConnector.TEST_THRIFT, "Test Thrift ")) {
      String url = JdbcConnector.getProperty(JdbcConnector.METASTORE_URL, "Thrift metastore URL");
      conf = new HiveConf();
      conf.setVar(HiveConf.ConfVars.METASTOREURIS, url);
      client = new HiveMetaStore.HMSHandler("ODPi test", conf, true);
    }
  }

  @Before
  public void checkIfActive() {
    Assume.assumeTrue(JdbcConnector.testActive(JdbcConnector.TEST_THRIFT, "Test Thrift "));
    rand = new Random();
  }

  @Test
  public void db() throws TException {
    final String dbName = "odpi_thrift_db_" + rand.nextInt(Integer.MAX_VALUE);

    String location = JdbcConnector.getProperty(JdbcConnector.LOCATION, " HDFS location we can " +
        "write to");
    Database db = new Database(dbName, "a db", location, new HashMap<String, String>());
    client.create_database(db);
    db = client.get_database(dbName);
    Assert.assertNotNull(db);
    db = new Database(db);
    db.getParameters().put("a", "b");
    client.alter_database(dbName, db);
    List<String> alldbs = client.get_databases("odpi_*");
    Assert.assertNotNull(alldbs);
    Assert.assertTrue(alldbs.size() > 0);
    alldbs = client.get_all_databases();
    Assert.assertNotNull(alldbs);
    Assert.assertTrue(alldbs.size() > 0);
    client.drop_database(dbName, true, true);
  }

  // Not testing types calls, as they aren't used AFAIK

  @Test
  public void nonPartitionedTable() throws TException {
    final String tableName = "odpi_thrift_table_" + rand.nextInt(Integer.MAX_VALUE);
    String location = JdbcConnector.getProperty(JdbcConnector.LOCATION, " HDFS location we can " +
        "write to");

    // I don't test every operation related to tables, but only those that are frequently used.
    SerDeInfo serde = new SerDeInfo("default_serde",
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), new HashMap<String, String>());
    FieldSchema fs = new FieldSchema("a", "int", "no comment");
    StorageDescriptor sd = new StorageDescriptor(Collections.singletonList(fs), location,
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT),
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT), false, 0, serde, null, null,
        new HashMap<String, String>());
    Table table = new Table(tableName, "default", "me", 0, 0, 0, sd, null,
        new HashMap<String, String>(), null, null, TableType.MANAGED_TABLE.toString());
    EnvironmentContext envContext = new EnvironmentContext(new HashMap<String, String>());
    client.create_table_with_environment_context(table, envContext);

    table = client.get_table("default", tableName);
    Assert.assertNotNull(table);

    List<Table> tables =
        client.get_table_objects_by_name("default", Collections.singletonList(tableName));
    Assert.assertNotNull(tables);
    Assert.assertEquals(1, tables.size());

    List<String> tableNames = client.get_tables("default", "odpi_*");
    Assert.assertNotNull(tableNames);
    Assert.assertTrue(tableNames.size() >= 1);

    tableNames = client.get_all_tables("default");
    Assert.assertNotNull(tableNames);
    Assert.assertTrue(tableNames.size() >= 1);

    List<FieldSchema> cols = client.get_fields("default", tableName);
    Assert.assertNotNull(cols);
    Assert.assertEquals(1, cols.size());

    cols = client.get_schema_with_environment_context("default", tableName, envContext);
    Assert.assertNotNull(cols);
    Assert.assertEquals(1, cols.size());

    table = new Table(table);
    table.getParameters().put("a", "b");
    client.alter_table_with_cascade("default", tableName, table, false);

    table.getParameters().put("c", "d");
    client.alter_table_with_environment_context("default", tableName, table, envContext);

    client.drop_table_with_environment_context("default", tableName, true, envContext);
  }

  @Test
  public void partitionedTable() throws TException {
    final String tableName = "odpi_thrift_partitioned_table_" + rand.nextInt(Integer.MAX_VALUE);
    String location = JdbcConnector.getProperty(JdbcConnector.LOCATION, " HDFS location we can " +
        "write to");

    // I don't test every operation related to tables, but only those that are frequently used.
    SerDeInfo serde = new SerDeInfo("default_serde",
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), new HashMap<String, String>());
    FieldSchema fs = new FieldSchema("a", "int", "no comment");
    StorageDescriptor sd = new StorageDescriptor(Collections.singletonList(fs), location,
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT),
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT), false, 0, serde, null, null,
        new HashMap<String, String>());
    FieldSchema pk = new FieldSchema("pk", "string", "");
    Table table = new Table(tableName, "default", "me", 0, 0, 0, sd, Collections.singletonList(pk),
        new HashMap<String, String>(), null, null, TableType.MANAGED_TABLE.toString());
    EnvironmentContext envContext = new EnvironmentContext(new HashMap<String, String>());
    client.create_table_with_environment_context(table, envContext);

    sd = new StorageDescriptor(Collections.singletonList(fs), location + "/x",
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE),
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), false, 0, serde, null, null,
        new HashMap<String, String>());
    Partition partition = new Partition(Collections.singletonList("x"), "default", tableName, 0,
        0, sd, new HashMap<String, String>());
    client.add_partition_with_environment_context(partition, envContext);

    sd = new StorageDescriptor(Collections.singletonList(fs), location + "/y",
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE),
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), false, 0, serde, null, null,
        new HashMap<String, String>());
    partition = new Partition(Collections.singletonList("y"), "default", tableName, 0,
        0, sd, new HashMap<String, String>());
    client.add_partitions(Collections.singletonList(partition));

    sd = new StorageDescriptor(Collections.singletonList(fs), location + "/z",
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE),
        conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), false, 0, serde, null, null,
        new HashMap<String, String>());
    partition = new Partition(Collections.singletonList("z"), "default", tableName, 0,
        0, sd, new HashMap<String, String>());
    AddPartitionsRequest rqst = new AddPartitionsRequest("default", tableName,
        Collections.singletonList(partition), true);
    client.add_partitions_req(rqst);

    List<Partition> parts = client.get_partitions("default", tableName, (short)-1);
    Assert.assertNotNull(parts);
    Assert.assertEquals(3, parts.size());

    parts = client.get_partitions_with_auth("default", tableName, (short)-1, "me",
        Collections.<String>emptyList());
    Assert.assertNotNull(parts);
    Assert.assertEquals(3, parts.size());

    parts = client.get_partitions_ps("default", tableName, Collections.singletonList("x"),
        (short)-1);
    Assert.assertNotNull(parts);
    Assert.assertEquals(1, parts.size());

    parts = client.get_partitions_by_filter("default", tableName, "pk = \"x\"", (short)-1);
    Assert.assertNotNull(parts);
    Assert.assertEquals(1, parts.size());

    parts = client.get_partitions_by_names("default", tableName, Collections.singletonList("pk=x"));
    Assert.assertNotNull(parts);
    Assert.assertEquals(1, parts.size());

    partition = client.get_partition("default", tableName, Collections.singletonList("x"));
    Assert.assertNotNull(partition);

    partition = client.get_partition_by_name("default", tableName, "pk=x");
    Assert.assertNotNull(partition);

    partition = client.get_partition_with_auth("default", tableName, Collections.singletonList("x"),
        "me", Collections.<String>emptyList());
    Assert.assertNotNull(partition);

    List<String> partitionNames = client.get_partition_names("default", tableName, (short)-1);
    Assert.assertNotNull(partitionNames);
    Assert.assertEquals(3, partitionNames.size());

    partition = new Partition(partition);
    partition.getParameters().put("a", "b");
    client.alter_partition("default", tableName, partition);

    for (Partition p : parts) p.getParameters().put("c", "d");
    client.alter_partitions("default", tableName, parts);

    // Not testing get_partitions_by_expr because I don't want to hard code some byte sequence
    // from the parser.  The odds that anyone other than Hive parser would call this method seem
    // low, since you'd have to exactly match the serliazation of the Hive parser.

    // Not testing partition marking events, not used by anyone but Hive replication AFAIK

    client.drop_partition_by_name_with_environment_context("default", tableName, "pk=x", true,
        envContext);
    client.drop_partition_with_environment_context("default", tableName,
        Collections.singletonList("y"), true, envContext);
    DropPartitionsRequest dropRequest = new DropPartitionsRequest("default", tableName,
        RequestPartsSpec.names(Collections.singletonList("pk=z")));
    client.drop_partitions_req(dropRequest);
  }

  // Not testing index calls, as no one uses indices


  // Not sure if anyone uses stats calls or not.  Other query engines might.  Ignoring for now.

  // Not sure if anyone else uses functions, though I'm guessing not as without Hive classes they
  // won't be runable.

  // Not testing authorization calls as AFAIK no one else uses Hive security

  // Not testing transaction/locking calls, as those are used only by Hive.

  // Not testing notification logging calls, as those are used only by Hive replication.

}
