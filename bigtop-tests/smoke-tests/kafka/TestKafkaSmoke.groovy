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

package org.apache.bigtop.itest.kafka

import org.junit.BeforeClass
import org.junit.AfterClass
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.junit.runner.RunWith

class TestKafkaSmoke {
  static Shell sh = new Shell("/bin/bash -s");

  static final String ZOOKEEPER_HOME = "/usr/lib/zookeeper"
  static final String ZK_SERVER = ZOOKEEPER_HOME + "/bin/zkServer.sh"

  static final String KAFKA_HOME = "/usr/lib/kafka"
  static final String KAFKA_CONFIG = KAFKA_HOME + "/config/server.properties"
  static final String KAFKA_TOPICS = KAFKA_HOME + "/bin/kafka-topics.sh"
  static final String KAFKA_SERVER_START = KAFKA_HOME + "/bin/kafka-server-start.sh"

  @Test
  public void testCreateTopics() {
    sh.exec(ZK_SERVER + " start");
    sh.exec(KAFKA_SERVER_START + KAFKA_CONFIG + " &");
    sh.exec(KAFKA_TOPICS + " --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test");
    sh.exec(KAFKA_TOPICS + " --list --zookeeper localhost:2181");
    assertTrue(" Create Kafka topics failed. " + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);
  }
}
