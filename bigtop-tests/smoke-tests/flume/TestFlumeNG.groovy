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

package org.apache.bigtop.itest.flume

import org.junit.BeforeClass
import org.junit.AfterClass
import static org.junit.Assert.assertNotNull
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.hadoop.conf.Configuration
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import org.junit.runner.RunWith

class TestFlumeNG {
  static private Log LOG = LogFactory.getLog(Object.class);

  static Shell sh = new Shell("/bin/bash -s");


  @AfterClass
  public static void tearDown() {
    LOG.warn("Not deleting flume-test from hdfs, delete manually if necessary")
    //sh.exec("hadoop fs -rmr -skipTrash /tmp/flume-test");
  }

  @BeforeClass
  static void setUp() {
    sh.exec("hadoop fs -rmr -skipTrash /tmp/flume-test");
    /**
     *  Start a collecter that will run for 60 seconds.
     *  This test is HCFS Compatible, see FLUME-2140.
     */
    String conf = """\n
        agent.channels.memory-channel.type = memory
        agent.channels.memory-channel.capacity = 2000
        agent.channels.memory-channel.transactionCapacity = 100
        agent.sources.tail-source.type = exec
        agent.sources.tail-source.command = tail -F /tmp/flume-smoke.source
        agent.sources.tail-source.channels = memory-channel

        agent.sinks.log-sink.channel = memory-channel
        agent.sinks.log-sink.type = logger

        # Define a sink that outputs to the DFS
        agent.sinks.hdfs-sink.channel = memory-channel
        agent.sinks.hdfs-sink.type = hdfs
        agent.sinks.hdfs-sink.hdfs.path = /tmp/flume-test
        agent.sinks.hdfs-sink.hdfs.fileType = DataStream

        # activate the channels/sinks/sources
        agent.channels = memory-channel
        agent.sources = tail-source
        agent.sinks = log-sink hdfs-sink"""
    new File("./conf/flume.conf").write(conf);

    //Start the listener...

    Thread.start {
      sh.exec("timeout 60 flume-ng agent " +
          "--conf ./conf/ " +
          "-f ./conf/flume.conf " +
          "-Dflume.root.logger=DEBUG,console " +
          "-n agent > /tmp/flumetest.log")
    }

    LOG.info("Started threaded listener.")
    LOG.info("Waiting 60 seconds to finish ....")
    LOG.info("check /tmp/flumetest.log for progress")
  }


  @Test
  void test() {
    /**
     *    Now write to the sink.
     */
    File source = new File("/tmp/flume-smoke.source");
    for (i in 1..100) {
      Thread.sleep(1000 - i)
      source.withWriterAppend("UTF-8") {
        it.write("hello ${i} \n")
      }
    }

    LOG.info("RESULTS.........")
    sh.exec("hadoop fs -cat /tmp/flume-test/* | grep -q hello")
    assertTrue("Did not detect the contents in the flume SINK." + sh.getOut(), sh.getRet() == 0);


  }
}
