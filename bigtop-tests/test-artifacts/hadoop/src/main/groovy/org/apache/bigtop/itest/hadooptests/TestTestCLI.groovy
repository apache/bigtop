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
package org.apache.bigtop.itest.hadooptests

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.cli.*

// TODO: we have to stub it for 0.20.2 release, once we move to 0.21+ this can go
// import org.apache.hadoop.fs.CommonConfigurationKeys
class CommonConfigurationKeys {
  static final public String  FS_DEFAULT_NAME_KEY = "fs.default.name";
}

/** This test class serves only one purpose: to prepare environment for proper
 execution of parent (TestCLI)
 */
class TestTestCLI extends TestCLI {
  private static final String JT = "mapred.job.tracker";

  static {
    private static Configuration conf = new Configuration();
    conf.addDefaultResource('mapred-site.xml');
    System.properties.setProperty(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY,
        conf.get(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY));
    System.properties.setProperty(JT, conf.get(JT));
    System.properties.setProperty('test.cli.config', 'testConfCluster.xml');
    // The following doesn't really work because in the parent this property
    // is used to set a value to a static member. Basically, by the time
    // we get here it's too late already.
    // The property needs to be set from runtime
    System.properties.setProperty('test.cache.data', 'clitest_data');
  }

  @Test
  @Ignore("HADOOP-7730")
  @Override
  public void testAll() {
    super.testAll();
  }
}
