/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.BeforeClass
import org.junit.AfterClass

import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import static org.apache.bigtop.itest.LogErrorsUtils.logError

class TestKnox {
  static private Log LOG = LogFactory.getLog(Object.class)

  static Shell sh = new Shell("/bin/bash -s")

  @BeforeClass
  static void setUp() {
    // noop for now.
  }

  @AfterClass
  public static void tearDown() {
    // noop for now
  }

  @Test
  void testKnox() {
    // Basic test to verify that Knox cli can list its topologies.


    sh.exec("export KNOX_GATEWAY_CONF_DIR=/etc/knox && export KNOX_GATEWAY_DATA_DIR=/var/lib/knox/data && export APP_JAVA_LIB_PATH=/usr/lib/knox/lib && export KNOX_GATEWAY_HOME_DIR=/usr/lib/knox && export KNOX_CLI_LOG_OPTS=\"-Dapp.log.dir=/var/log/knox -Dlauncher.name=knoxcli -Dlog4j.configuration=/etc/knox/knoxcli-log4j.properties\" && /usr/lib/knox/bin/knoxcli.sh list-topologies");
    String ret = sh.getOut()
    assertTrue(ret.contains("sandbox"));
    LOG.info('Test Knox cli finished');
    LOG.info('Found topology '+ret);


    LOG.info('Running systemctl status knox-gateway');
    sh.exec("systemctl status knox-gateway | grep \"active (running)\"");
    logError(sh);
    assertTrue("Failed ...", sh.getRet() == 0);
  }
}
