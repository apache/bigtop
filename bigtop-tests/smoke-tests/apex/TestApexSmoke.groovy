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

package org.apache.bigtop.itest.apex

import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.Test
import static org.junit.Assert.assertTrue

import org.apache.bigtop.itest.shell.Shell
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.junit.runner.RunWith

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestApexSmoke {
  static private Log LOG = LogFactory.getLog(Object.class);

  static def testDir = "build/resources/test/"
  static Shell sh = new Shell("/bin/bash -s");
  static appId = null;

  @AfterClass
  public static void tearDown() {
    sh.exec('hadoop fs -rm -r -skipTrash datatorrent');
    sh.exec('rm -rf ' + testDir);
  }

  @BeforeClass
  static void setUp() {
    sh.exec('pushd ' + testDir,
            'mvn clean package -DskipTests',
            'popd');
    LOG.info('Setup Done. Package ready to launch.');
  }

  @Test
  void test1_Start() {
    String apaPath = testDir + "target/myapexapp-1.0-SNAPSHOT.apa";
    LOG.info('Starting apex application...');
    sh.exec('echo "launch ' + apaPath + '" | apex');
    
    String out = sh.getOut()[1].trim();
    LOG.info("ApplicationID: " + out);
    assertTrue(out.contains('{"appId": "application_'));
    out = out.replace('{"appId": "', '');
    appId = out.replace('"}', '');
    LOG.info('Sleeping for 20 sec for application to start...');
    sh.exec('sleep 20');
    sh.exec("yarn application -status " + appId);
    out = sh.getOut();
    for (String item : out) {
      if (item.contains('Application-Id :')) {
        assertTrue(item.contains(appId));
      }
      if (item.contains('Application-Name :')) {
        assertTrue(item.contains('MyFirstApplication'));
      }
      if (item.contains('Application-Type :')) {
        assertTrue(item.contains('DataTorrent'));
      }
      else if (item.contains('State :')) {
        assertTrue(item.contains('RUNNING'));
      }
      else if (item.contains('Final-State :')) {
        assertTrue(item.contains('UNDEFINED'));
      }
    }
  }

  @Test
  void test2_Stop() {
    LOG.info('Stopping apex application..');
    sh.exec('echo "shutdown-app ' + appId + '" | apex');
    LOG.info('Sleeping for 20 sec for application to stop...');
    sh.exec('sleep 20');
    sh.exec("yarn application -status " + appId);
    String out = sh.getOut();
    for (String item : out) {
      if (item.contains('Application-Id :')) {
        assertTrue(item.contains(appId));
      }
      if (item.contains('Application-Name :')) {
        assertTrue(item.contains('MyFirstApplication'));
      }
      if (item.contains('Application-Type :')) {
        assertTrue(item.contains('DataTorrent'));
      }
      else if (item.contains('State :')) {
        assertTrue(item.contains('FINISHED'));
      }
      else if (item.contains('Final-State :')) {
        assertTrue(item.contains('SUCCEEDED'));
      }
    } 
  }
}
