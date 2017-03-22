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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.Assert;

public class TestCLI {

    static Map<String, String> results;
    static String db = "javax.jdo.option.ConnectionURL=jdbc:derby:;databaseName=bigtop_metastore_db;create=true";

    @BeforeClass
    public static void setup() {

        results = HiveHelper.execCommand(new CommandLine("which").addArgument("hive"));
        Assert.assertEquals("Hive is not in the current path.", 0, Integer.parseInt(results.get("exitValue")));
    }

    @Test
    public void help() {
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-H"));
        //LOG.info(results.get("exitValue"));
        Assert.assertEquals("Error in executing 'hive -H'", 2, Integer.parseInt(results.get("exitValue")));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("--help"));
        Assert.assertEquals("Error in executing 'hive --help'", 0, Integer.parseInt(results.get("exitValue")));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-U"));
        Assert.assertEquals("Unrecognized option should exit 1.", 1, Integer.parseInt(results.get("exitValue")));
    }

    @Test
    public void sqlFromCmdLine() {

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("SHOW DATABASES command failed to execute.", 0, Integer.parseInt(results.get("exitValue")));
        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
            Assert.assertEquals("Could not create database bigtop_runtime_hive.", 0, Integer.parseInt(results.get("exitValue")));
        } else {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
            Assert.assertEquals("Could not create database bigtop_runtime_hive.", 0, Integer.parseInt(results.get("exitValue")));
        }
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
    }

    @Test
    public void sqlFromFiles() throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter("hive-f1.sql")) {
            out.println("SHOW DATABASES;");
        }
        try (PrintWriter out = new PrintWriter("hive-f2.sql")) {
            out.println("CREATE DATABASE bigtop_runtime_hive;");
        }
        try (PrintWriter out = new PrintWriter("hive-f3.sql")) {
            out.println("DROP DATABASE bigtop_runtime_hive;");
            out.println("CREATE DATABASE bigtop_runtime_hive;");
        }
        try (PrintWriter out = new PrintWriter("hive-f4.sql")) {
            out.println("DROP DATABASE bigtop_runtime_hive;");
        }
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-f").addArgument("hive-f1.sql").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("SHOW DATABASES command failed to execute.", 0, Integer.parseInt(results.get("exitValue")));
        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-f").addArgument("hive-f2.sql").addArgument("--hiveconf").addArgument(db));
            Assert.assertEquals("Could not create database bigtop_runtime_hive.", 0, Integer.parseInt(results.get("exitValue")));
        } else {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-f").addArgument("hive-f3.sql").addArgument("--hiveconf").addArgument(db));
            Assert.assertEquals("Could not create database bigtop_runtime_hive.", 0, Integer.parseInt(results.get("exitValue")));
        }
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-f").addArgument("hive-f4.sql").addArgument("--hiveconf").addArgument(db));
    }

    @Test
    public void silent() {
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("-S").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("-S option did not work.", new Boolean(false), results.get("outputStream").contains("Time taken:"));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--silent").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("--silent option did not work.", new Boolean(false), results.get("outputStream").contains("Time taken:"));
    }

    @Test
    public void verbose() {
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("-v").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("-v option did not work.", new Boolean(true), results.get("outputStream").contains("SHOW DATABASES"));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--verbose").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("--verbose option did not work.", new Boolean(true), results.get("outputStream").contains("SHOW DATABASES"));
    }

    @Test
    public void initialization() throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter("hive-init1.sql")) {
            out.println("CREATE DATABASE bigtop_runtime_hive;");
        }
        try (PrintWriter out = new PrintWriter("hive-init2.sql")) {
            out.println("DROP DATABASE bigtop_runtime_hive;");
            out.println("CREATE DATABASE bigtop_runtime_hive;");
        }

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("SHOW DATABASES command failed to execute.", 0, Integer.parseInt(results.get("exitValue")));
        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-i").addArgument("hive-init1.sql").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
            Assert.assertEquals("Could not create database bigtop_runtime_hive using the init -i option.", 0, Integer.parseInt(results.get("exitValue")));
            Assert.assertEquals("Could not create database bigtop_runtime_hive using the init -i option.", true, results.get("outputStream").contains("bigtop_runtime_hive"));
        } else {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-i").addArgument("hive-init2.sql").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
            Assert.assertEquals("Could not create database bigtop_runtime_hive.", 0, Integer.parseInt(results.get("exitValue")));
            Assert.assertEquals("Could not create database bigtop_runtime_hive using the init -i option.", true, results.get("outputStream").contains("bigtop_runtime_hive"));
        }
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
    }

    @Test
    public void database() {

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
        } else {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
        }
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("--database").addArgument("bigtop_runtime_hive_1234").addArgument("-e").addArgument("CREATE TABLE bigtop ( MYID INT );").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("Non-existent database returned with wrong exit code: " + Integer.parseInt(results.get("exitValue")), 88, Integer.parseInt(results.get("exitValue")));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("--database").addArgument("bigtop_runtime_hive").addArgument("-e").addArgument("CREATE TABLE bigtop ( MYID INT );").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("Failed to create table using --database argument.", 0, Integer.parseInt(results.get("exitValue")));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("--database").addArgument("bigtop_runtime_hive").addArgument("-e").addArgument("DESCRIBE bigtop").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("Failed to get expected column after creating bigtop table using --database argument.", true, results.get("outputStream").contains("myid"));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("--database").addArgument("bigtop_runtime_hive").addArgument("-e").addArgument("DROP TABLE bigtop").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("Failed to create table using --database argument.", 0, Integer.parseInt(results.get("exitValue")));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
    }

    @Test
    public void hiveConf() {
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("--hiveconf").addArgument("hive.root.logger=INFO,console").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("The --hiveconf option did not work in setting hive.root.logger=INFO,console.", true, results.get("outputStream").contains("INFO parse.ParseDriver: Parsing command: SHOW DATABASES"));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-hiveconf").addArgument("hive.root.logger=INFO,console").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
        Assert.assertEquals("The -hiveconf variant option did not work in setting hive.root.logger=INFO,console.", true, results.get("outputStream").contains("INFO parse.ParseDriver: Parsing command: SHOW DATABASES"));
    }

    @Test
    public void variableSubsitution() throws FileNotFoundException {
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
        } else {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
        }
        try (PrintWriter out = new PrintWriter("hive-define.sql")) {
            out.println("show ${A};");
            out.println("quit;");
        }
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("hive -d A=DATABASES --hiveconf '" + db + "' < hive-define.sql", false));
        Assert.assertEquals("The hive -d A=DATABASES option did not work.", 0, Integer.parseInt(results.get("exitValue")));
        Assert.assertEquals("The hive -d A=DATABASES option did not work.", true, results.get("outputStream").contains("bigtop_runtime_hive"));

        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("hive --define A=DATABASES --hiveconf '" + db + "' < hive-define.sql", false));
        Assert.assertEquals("The hive --define A=DATABASES option did not work.", 0, Integer.parseInt(results.get("exitValue")));
        Assert.assertEquals("The hive --define A=DATABASES option did not work.", true, results.get("outputStream").contains("bigtop_runtime_hive"));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
    }

    @Test
    public void hiveVar() throws FileNotFoundException {
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("SHOW DATABASES").addArgument("--hiveconf").addArgument(db));
        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
        } else {
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
            results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
        }
        try (PrintWriter out = new PrintWriter("hive-var.sql")) {
            out.println("show ${A};");
            out.println("quit;");
        }
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("hive --hivevar A=DATABASES --hiveconf '" + db + "' < hive-var.sql", false));
        Assert.assertEquals("The hive --hivevar A=DATABASES option did not work.", 0, Integer.parseInt(results.get("exitValue")));
        Assert.assertEquals("The hive --hivevar A=DATABASES option did not work.", true, results.get("outputStream").contains("bigtop_runtime_hive"));

        try (PrintWriter out = new PrintWriter("hiveconf-var.sql")) {
            out.println("show ${hiveconf:A};");
            out.println("quit;");
        }
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("hive --hiveconf A=DATABASES --hiveconf '" + db + "' < hiveconf-var.sql", false));
        Assert.assertEquals("The hive --hiveconf A=DATABASES option did not work.", 0, Integer.parseInt(results.get("exitValue")));
        Assert.assertEquals("The hive --hiveconf A=DATABASES option did not work.", true, results.get("outputStream").contains("bigtop_runtime_hive"));

        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
    }

    @AfterClass
    public static void cleanup() {
        results = HiveHelper.execCommand(new CommandLine("hive").addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive").addArgument("--hiveconf").addArgument(db));
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf hive-f*.sql", false));
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf hive-init*.sql", false));
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf hive-define.sql", false));
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf hive-var.sql", false));
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf hiveconf-var.sql", false));
    }

}
