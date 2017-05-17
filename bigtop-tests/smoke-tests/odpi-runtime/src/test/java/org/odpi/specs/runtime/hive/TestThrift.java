/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hive;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TestThrift {

    private static final Log LOG = LogFactory.getLog(TestThrift.class.getName());

    private static IMetaStoreClient client = null;
    private static HiveConf conf;

    private Random rand;

    @BeforeClass
    public static void connect() throws MetaException {
        if (JdbcConnector.testActive(JdbcConnector.TEST_THRIFT, "Test Thrift ")) {
            String url = JdbcConnector.getProperty(JdbcConnector.METASTORE_URL, "Thrift metastore URL");
            conf = new HiveConf();
            conf.setVar(HiveConf.ConfVars.METASTOREURIS, url);
            LOG.info("Set to test against metastore at " + url);
            client = new HiveMetaStoreClient(conf);
        }
    }

    @Before
    public void checkIfActive() {
        Assume.assumeTrue(JdbcConnector.testActive(JdbcConnector.TEST_THRIFT, "Test Thrift "));
        rand = new Random();
    }

    @Test
    public void db() throws TException {
        final String dbName = "bigtop_thrift_db_" + rand.nextInt(Integer.MAX_VALUE);

        Database db = new Database(dbName, "a db", null, new HashMap<String, String>());
        client.createDatabase(db);
        db = client.getDatabase(dbName);
        Assert.assertNotNull(db);
        db = new Database(db);
        db.getParameters().put("a", "b");
        client.alterDatabase(dbName, db);
        List<String> alldbs = client.getDatabases("bigtop_*");
        Assert.assertNotNull(alldbs);
        Assert.assertTrue(alldbs.size() > 0);
        alldbs = client.getAllDatabases();
        Assert.assertNotNull(alldbs);
        Assert.assertTrue(alldbs.size() > 0);
        client.dropDatabase(dbName, true, true);
    }

    // Not testing types calls, as they aren't used AFAIK

    @Test
    public void nonPartitionedTable() throws TException {
        final String tableName = "bigtop_thrift_table_" + rand.nextInt(Integer.MAX_VALUE);

        // I don't test every operation related to tables, but only those that are frequently used.
        SerDeInfo serde = new SerDeInfo("default_serde",
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), new HashMap<String, String>());
        FieldSchema fs = new FieldSchema("a", "int", "no comment");
        StorageDescriptor sd = new StorageDescriptor(Collections.singletonList(fs), null,
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT),
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT), false, 0, serde, null, null,
                new HashMap<String, String>());
        Table table = new Table(tableName, "default", "me", 0, 0, 0, sd, null,
                new HashMap<String, String>(), null, null, TableType.MANAGED_TABLE.toString());
        client.createTable(table);

        table = client.getTable("default", tableName);
        Assert.assertNotNull(table);

        List<Table> tables =
                client.getTableObjectsByName("default", Collections.singletonList(tableName));
        Assert.assertNotNull(tables);
        Assert.assertEquals(1, tables.size());

        List<String> tableNames = client.getTables("default", "bigtop_*");
        Assert.assertNotNull(tableNames);
        Assert.assertTrue(tableNames.size() >= 1);

        tableNames = client.getAllTables("default");
        Assert.assertNotNull(tableNames);
        Assert.assertTrue(tableNames.size() >= 1);

        List<FieldSchema> cols = client.getFields("default", tableName);
        Assert.assertNotNull(cols);
        Assert.assertEquals(1, cols.size());

        cols = client.getSchema("default", tableName);
        Assert.assertNotNull(cols);
        Assert.assertEquals(1, cols.size());

        table = new Table(table);
        table.getParameters().put("a", "b");
        client.alter_table("default", tableName, table, false);

        table.getParameters().put("c", "d");
        client.alter_table("default", tableName, table);

        client.dropTable("default", tableName, true, false);
    }

    @Test
    public void partitionedTable() throws TException {
        final String tableName = "bigtop_thrift_partitioned_table_" + rand.nextInt(Integer.MAX_VALUE);

        // I don't test every operation related to tables, but only those that are frequently used.
        SerDeInfo serde = new SerDeInfo("default_serde",
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), new HashMap<String, String>());
        FieldSchema fs = new FieldSchema("a", "int", "no comment");
        StorageDescriptor sd = new StorageDescriptor(Collections.singletonList(fs), null,
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT),
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTFILEFORMAT), false, 0, serde, null, null,
                new HashMap<String, String>());
        FieldSchema pk = new FieldSchema("pk", "string", "");
        Table table = new Table(tableName, "default", "me", 0, 0, 0, sd, Collections.singletonList(pk),
                new HashMap<String, String>(), null, null, TableType.MANAGED_TABLE.toString());
        client.createTable(table);

        sd = new StorageDescriptor(Collections.singletonList(fs), null,
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE),
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), false, 0, serde, null, null,
                new HashMap<String, String>());
        Partition partition = new Partition(Collections.singletonList("x"), "default", tableName, 0,
                0, sd, new HashMap<String, String>());
        client.add_partition(partition);

        List<Partition> partitions = new ArrayList<>(2);
        sd = new StorageDescriptor(Collections.singletonList(fs), null,
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE),
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), false, 0, serde, null, null,
                new HashMap<String, String>());
        partitions.add(new Partition(Collections.singletonList("y"), "default", tableName, 0,
                0, sd, new HashMap<String, String>()));
        sd = new StorageDescriptor(Collections.singletonList(fs), null,
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE),
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), false, 0, serde, null, null,
                new HashMap<String, String>());
        partitions.add(new Partition(Collections.singletonList("z"), "default", tableName, 0,
                0, sd, new HashMap<String, String>()));
        client.add_partitions(partitions);

        List<Partition> parts = client.listPartitions("default", tableName, (short) -1);
        Assert.assertNotNull(parts);
        Assert.assertEquals(3, parts.size());

        parts = client.listPartitions("default", tableName, Collections.singletonList("x"),
                (short) -1);
        Assert.assertNotNull(parts);
        Assert.assertEquals(1, parts.size());

        parts = client.listPartitionsWithAuthInfo("default", tableName, (short) -1, "me",
                Collections.<String>emptyList());
        Assert.assertNotNull(parts);
        Assert.assertEquals(3, parts.size());

        List<String> partNames = client.listPartitionNames("default", tableName, (short) -1);
        Assert.assertNotNull(partNames);
        Assert.assertEquals(3, partNames.size());

        parts = client.listPartitionsByFilter("default", tableName, "pk = \"x\"", (short) -1);
        Assert.assertNotNull(parts);
        Assert.assertEquals(1, parts.size());

        parts = client.getPartitionsByNames("default", tableName, Collections.singletonList("pk=x"));
        Assert.assertNotNull(parts);
        Assert.assertEquals(1, parts.size());

        partition = client.getPartition("default", tableName, Collections.singletonList("x"));
        Assert.assertNotNull(partition);

        partition = client.getPartition("default", tableName, "pk=x");
        Assert.assertNotNull(partition);

        partition = client.getPartitionWithAuthInfo("default", tableName, Collections.singletonList("x"),
                "me", Collections.<String>emptyList());
        Assert.assertNotNull(partition);

        partition = new Partition(partition);
        partition.getParameters().put("a", "b");
        client.alter_partition("default", tableName, partition);

        for (Partition p : parts) p.getParameters().put("c", "d");
        client.alter_partitions("default", tableName, parts);

        // Not testing get_partitions_by_expr because I don't want to hard code some byte sequence
        // from the parser.  The odds that anyone other than Hive parser would call this method seem
        // low, since you'd have to exactly match the serliazation of the Hive parser.

        // Not testing partition marking events, not used by anyone but Hive replication AFAIK

        client.dropPartition("default", tableName, "pk=x", true);
        client.dropPartition("default", tableName, Collections.singletonList("y"), true);
    }

    // Not testing index calls, as no one uses indices


    // Not sure if anyone uses stats calls or not.  Other query engines might.  Ignoring for now.

    // Not sure if anyone else uses functions, though I'm guessing not as without Hive classes they
    // won't be runable.

    // Not testing authorization calls as AFAIK no one else uses Hive security

    // Not testing transaction/locking calls, as those are used only by Hive.

    // Not testing notification logging calls, as those are used only by Hive replication.

}
