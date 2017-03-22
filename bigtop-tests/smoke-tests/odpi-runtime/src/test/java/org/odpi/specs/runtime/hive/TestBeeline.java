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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

public class TestBeeline {

    public static final Log LOG = LogFactory.getLog(TestBeeline.class.getName());

    private static final String URL = "bigtop.test.hive.jdbc.url";
    private static final String USER = "bigtop.test.hive.jdbc.user";
    private static final String PASSWD = "bigtop.test.hive.jdbc.password";

    private static Map<String, String> results;
    private static String beelineUrl;
    private static String beelineUser;
    private static String beelinePasswd;

    //creating beeline base command with username and password as per inputs
    private static CommandLine beelineBaseCommand = new CommandLine("beeline");

    @BeforeClass
    public static void initialSetup() {
        TestBeeline.beelineUrl = System.getProperty(URL);
        TestBeeline.beelineUser = System.getProperty(USER);
        TestBeeline.beelinePasswd = System.getProperty(PASSWD);

        if (beelineUser != null && beelineUser != "" && beelinePasswd != null && beelinePasswd != "") {
            beelineBaseCommand.addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd);
        } else if (beelineUser != null && beelineUser != "") {
            beelineBaseCommand.addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser);
        } else {
            beelineBaseCommand.addArgument("-u").addArgument(beelineUrl);
        }
        LOG.info("URL is " + beelineUrl);
        LOG.info("User is " + beelineUser);
        LOG.info("Passwd is " + beelinePasswd);
        LOG.info("Passwd is null " + (beelinePasswd == null));
    }

    @Test
    public void checkBeeline() {
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline -u FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("connecting to " + beelineUrl.toLowerCase()) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
    }

    @Test
    public void checkBeelineConnect() {
        try (PrintWriter out = new PrintWriter("connect.url")) {
            out.println("!connect " + beelineUrl + " " + beelineUser + " " + beelinePasswd);
            out.println("!quit");
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -f connect.url", false));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline !connect FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("connecting to " + beelineUrl.toLowerCase()) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
    }

    @Test
    public void checkBeelineHelp() {
        results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("--help"));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline --help FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("display this message") && consoleMsg.contains("usage: java org.apache.hive.cli.beeline.beeline") && !consoleMsg.contains("exception"));
    }

    @Test
    public void checkBeelineQueryExecFromCmdLine() {
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-e").addArgument("SHOW DATABASES;"));
        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive;"));
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-e").addArgument("SHOW DATABASES;"));
        } else {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive;"));
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-e").addArgument("CREATE DATABASE bigtop_runtime_hive;"));
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-e").addArgument("SHOW DATABASES;"));
        }
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline -e FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("bigtop_runtime_hive") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
        HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-e").addArgument("DROP DATABASE bigtop_runtime_hive"));
    }

    @Test
    public void checkBeelineQueryExecFromFile() throws FileNotFoundException {

        try (PrintWriter out = new PrintWriter("beeline-f1.sql")) {
            out.println("SHOW DATABASES;");
        }
        try (PrintWriter out = new PrintWriter("beeline-f2.sql")) {
            out.println("CREATE DATABASE bigtop_runtime_hive;");
        }
        try (PrintWriter out = new PrintWriter("beeline-f3.sql")) {
            out.println("DROP DATABASE bigtop_runtime_hive;");
            out.println("CREATE DATABASE bigtop_runtime_hive;");
        }
        try (PrintWriter out = new PrintWriter("beeline-f4.sql")) {
            out.println("DROP DATABASE bigtop_runtime_hive;");
        }
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-f").addArgument("beeline-f1.sql", false));

        if (!results.get("outputStream").contains("bigtop_runtime_hive")) {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-f").addArgument("beeline-f2.sql", false));
        } else {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-f").addArgument("beeline-f3.sql", false));
        }

        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-f").addArgument("beeline-f1.sql", false));

        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline -f FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("bigtop_runtime_hive") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
        HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-f").addArgument("beeline-f4.sql", false));
    }

    @Test
    public void checkBeelineInitFile() throws FileNotFoundException {

        try (PrintWriter out = new PrintWriter("beeline-i1.sql")) {
            out.println("SHOW DATABASES;");
        }
        try (PrintWriter out = new PrintWriter("beeline-i2.sql")) {
            out.println("CREATE DATABASE bigtop_runtime_beeline_init;");
        }
        try (PrintWriter out = new PrintWriter("beeline-i3.sql")) {
            out.println("DROP DATABASE bigtop_runtime_beeline_init;");
            out.println("CREATE DATABASE bigtop_runtime_beeline_init;");
        }
        try (PrintWriter out = new PrintWriter("beeline-i4.sql")) {
            out.println("DROP DATABASE bigtop_runtime_beeline_init;");
        }
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-i").addArgument("beeline-i1.sql", false));

        if (!results.get("outputStream").contains("bigtop_runtime_beeline_init")) {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-i").addArgument("beeline-i2.sql", false));
        } else {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-i").addArgument("beeline-i3.sql", false));
        }

        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-i").addArgument("beeline-i1.sql", false));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline -i FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("bigtop_runtime_beeline_init") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
        HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("-i").addArgument("beeline-i4.sql", false));
    }

    @Test
    public void checkBeelineHiveVar() throws FileNotFoundException {

        try (PrintWriter out = new PrintWriter("beeline-hv1.sql")) {
            out.println("SHOW DATABASES;");
        }
        try (PrintWriter out = new PrintWriter("beeline-hv2.sql")) {
            out.println("CREATE DATABASE ${db};");
        }
        try (PrintWriter out = new PrintWriter("beeline-hv3.sql")) {
            out.println("DROP DATABASE ${db};");
            out.println("CREATE DATABASE ${db};");
        }
        try (PrintWriter out = new PrintWriter("beeline-hv4.sql")) {
            out.println("DROP DATABASE ${db};");
        }
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--hivevar").addArgument("db=bigtop_runtime_beeline_hivevar").addArgument("-i").addArgument("beeline-hv1.sql", false));

        if (!results.get("outputStream").contains("bigtop_runtime_beeline_hivevar")) {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--hivevar").addArgument("db=bigtop_runtime_beeline_hivevar").addArgument("-i").addArgument("beeline-hv2.sql", false));
        } else {
            results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--hivevar").addArgument("db=bigtop_runtime_beeline_hivevar").addArgument("-i").addArgument("beeline-hv3.sql", false));
        }

        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--hivevar").addArgument("db=bigtop_runtime_beeline_hivevar").addArgument("-i").addArgument("beeline-hv1.sql", false));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline --hivevar FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("bigtop_runtime_beeline_hivevar") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
        HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--hivevar").addArgument("db=bigtop_runtime_beeline_hivevar").addArgument("-i").addArgument("beeline-hv4.sql", false));
    }

    @Test
    public void checkBeelineFastConnect() {
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--fastConnect=false"));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline --fastConnect FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("set fastconnect to true to skip"));
    }

    @Test
    public void checkBeelineVerbose() {
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--verbose=true"));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline --verbose FAILED." + results.get("outputStream"), true, consoleMsg.contains("issuing: !connect jdbc:hive2:") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
    }

    @Test
    public void checkBeelineShowHeader() {
        results = HiveHelper.execCommand(new CommandLine(beelineBaseCommand).addArgument("--showHeader=false").addArgument("-e").addArgument("SHOW DATABASES;"));
        String consoleMsg = results.get("outputStream").toLowerCase();
        Assert.assertEquals("beeline --showHeader FAILED. \n" + results.get("outputStream"), true, consoleMsg.contains("default") && !consoleMsg.contains("database_name") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
    }

    @AfterClass
    public static void cleanup() throws FileNotFoundException {
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf beeline*.sql", false));
        results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf connect.url", false));
    }
}
