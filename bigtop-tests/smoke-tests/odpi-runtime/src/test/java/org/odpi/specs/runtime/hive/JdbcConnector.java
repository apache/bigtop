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
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class JdbcConnector {
    private static final Log LOG = LogFactory.getLog(JdbcConnector.class.getName());

    protected static final String URL = "bigtop.test.hive.jdbc.url";
    protected static final String USER = "bigtop.test.hive.jdbc.user";
    protected static final String PASSWD = "bigtop.test.hive.jdbc.password";
    protected static final String LOCATION = "bigtop.test.hive.location";
    protected static final String METASTORE_URL = "bigtop.test.hive.metastore.url";
    protected static final String TEST_THRIFT = "bigtop.test.hive.thrift.test";
    protected static final String TEST_HCATALOG = "bigtop.test.hive.hcatalog.test";
    protected static final String HIVE_CONF_DIR = "bigtop.test.hive.conf.dir";
    protected static final String HADOOP_CONF_DIR = "bigtop.test.hadoop.conf.dir";

    protected static Connection conn;

    @BeforeClass
    public static void connectToJdbc() throws SQLException {
        // Assume they've put the URL for the JDBC driver in an environment variable.
        String jdbcUrl = getProperty(URL, "the JDBC URL");
        String jdbcUser = getProperty(USER, "the JDBC user name");
        String jdbcPasswd = getProperty(PASSWD, "the JDBC password");

        Properties props = new Properties();
        props.put("user", jdbcUser);
        if (!jdbcPasswd.equals("")) props.put("password", jdbcPasswd);
        conn = DriverManager.getConnection(jdbcUrl, props);
    }

    @AfterClass
    public static void closeJdbc() throws SQLException {
        if (conn != null) conn.close();
    }

    protected static String getProperty(String property, String description) {
        String val = System.getProperty(property);
        if (val == null) {
            throw new RuntimeException("You must set the property " + property + " with " +
                    description);
        }
        LOG.debug(description + " is " + val);
        return val;
    }

    protected static boolean testActive(String property, String description) {
        String val = System.getProperty(property, "true");
        LOG.debug(description + " is " + val);
        return Boolean.valueOf(val);
    }

}
