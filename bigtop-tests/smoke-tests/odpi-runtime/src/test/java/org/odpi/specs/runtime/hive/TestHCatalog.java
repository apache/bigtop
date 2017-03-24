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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hive.hcatalog.data.schema.HCatFieldSchema;
import org.apache.hive.hcatalog.data.schema.HCatSchema;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class TestHCatalog {
    private static final String JOBJAR = "bigtop.test.hive.hcat.job.jar";
    private static final String HCATCORE = "bigtop.test.hive.hcat.core.jar";

    private static final Log LOG = LogFactory.getLog(TestHCatalog.class.getName());

    private static IMetaStoreClient client = null;
    private static HiveConf conf;
    private static HCatSchema inputSchema;
    private static HCatSchema outputSchema;

    private Random rand;

    @BeforeClass
    public static void connect() throws MetaException {
        if (JdbcConnector.testActive(JdbcConnector.TEST_HCATALOG, "Test HCatalog ")) {
            String hiveConfDir = JdbcConnector.getProperty(JdbcConnector.HIVE_CONF_DIR,
                    "Hive conf directory ");
            String hadoopConfDir = JdbcConnector.getProperty(JdbcConnector.HADOOP_CONF_DIR,
                    "Hadoop conf directory ");
            conf = new HiveConf();
            String fileSep = System.getProperty("file.separator");
            conf.addResource(new Path(hadoopConfDir + fileSep + "core-site.xml"));
            conf.addResource(new Path(hadoopConfDir + fileSep + "hdfs-site.xml"));
            conf.addResource(new Path(hadoopConfDir + fileSep + "yarn-site.xml"));
            conf.addResource(new Path(hadoopConfDir + fileSep + "mapred-site.xml"));
            conf.addResource(new Path(hiveConfDir + fileSep + "hive-site.xml"));
            client = new HiveMetaStoreClient(conf);

        }
    }

    @Before
    public void checkIfActive() {
        Assume.assumeTrue(JdbcConnector.testActive(JdbcConnector.TEST_HCATALOG, "Test HCatalog "));
        rand = new Random();
    }

    @Test
    public void hcatInputFormatOutputFormat() throws TException, IOException, ClassNotFoundException,
            InterruptedException, URISyntaxException {
        // Create a table to write to
        final String inputTable = "bigtop_hcat_input_table_" + rand.nextInt(Integer.MAX_VALUE);
        SerDeInfo serde = new SerDeInfo("default_serde",
                conf.getVar(HiveConf.ConfVars.HIVEDEFAULTSERDE), new HashMap<String, String>());
        FieldSchema schema = new FieldSchema("line", "string", "");
        inputSchema = new HCatSchema(Collections.singletonList(new HCatFieldSchema(schema.getName(),
                HCatFieldSchema.Type.STRING, schema.getComment())));
        StorageDescriptor sd = new StorageDescriptor(Collections.singletonList(schema), null,
                "org.apache.hadoop.mapred.TextInputFormat",
                "org.apache.hadoop.hive.ql.io.IgnoreKeyTextOutputFormat", false, 0, serde, null, null,
                new HashMap<String, String>());
        Table table = new Table(inputTable, "default", "me", 0, 0, 0, sd, null,
                new HashMap<String, String>(), null, null, TableType.MANAGED_TABLE.toString());
        client.createTable(table);

        final String outputTable = "bigtop_hcat_output_table_" + rand.nextInt(Integer.MAX_VALUE);
        sd = new StorageDescriptor(Arrays.asList(
                new FieldSchema("word", "string", ""),
                new FieldSchema("count", "int", "")),
                null, "org.apache.hadoop.mapred.TextInputFormat",
                "org.apache.hadoop.hive.ql.io.IgnoreKeyTextOutputFormat", false, 0, serde, null, null,
                new HashMap<String, String>());
        table = new Table(outputTable, "default", "me", 0, 0, 0, sd, null,
                new HashMap<String, String>(), null, null, TableType.MANAGED_TABLE.toString());
        client.createTable(table);
        outputSchema = new HCatSchema(Arrays.asList(
                new HCatFieldSchema("word", HCatFieldSchema.Type.STRING, ""),
                new HCatFieldSchema("count", HCatFieldSchema.Type.INT, "")));

        // LATER Could I use HCatWriter here and the reader to read it?
        // Write some stuff into a file in the location of the table
        table = client.getTable("default", inputTable);
        String inputFile = table.getSd().getLocation() + "/input";
        Path inputPath = new Path(inputFile);
        FileSystem fs = FileSystem.get(conf);
        FSDataOutputStream out = fs.create(inputPath);
        out.writeChars("Mary had a little lamb\n");
        out.writeChars("its fleece was white as snow\n");
        out.writeChars("and everywhere that Mary went\n");
        out.writeChars("the lamb was sure to go\n");
        out.close();

        Map<String, String> env = new HashMap<>();
        env.put("HADOOP_CLASSPATH", System.getProperty(HCATCORE, ""));
        Map<String, String> results = HiveHelper.execCommand(new CommandLine("hive")
                .addArgument("--service")
                .addArgument("jar")
                .addArgument(System.getProperty(JOBJAR))
                .addArgument(HCatalogMR.class.getName())
                .addArgument("-it")
                .addArgument(inputTable)
                .addArgument("-ot")
                .addArgument(outputTable)
                .addArgument("-is")
                .addArgument(inputSchema.getSchemaAsTypeString())
                .addArgument("-os")
                .addArgument(outputSchema.getSchemaAsTypeString()), env);
        LOG.info(results.toString());
        Assert.assertEquals("HCat job failed", 0, Integer.parseInt(results.get("exitValue")));

        client.dropTable("default", inputTable);
        client.dropTable("default", outputTable);
    }

}
