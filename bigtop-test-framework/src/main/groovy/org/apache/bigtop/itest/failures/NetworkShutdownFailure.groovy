/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.failures

/**
 * Can shutdown network connections between specified hosts during tests execution.
 */
public class NetworkShutdownFailure extends AbstractFailure {

  private static final String DROP_INPUT_CONNECTIONS = "sudo iptables -A INPUT -s %s -j DROP"
  private static final String DROP_OUTPUT_CONNECTIONS = "sudo iptables -A OUTPUT -d %s -j DROP"
  private static final String RESTORE_INPUT_CONNECTIONS = "sudo iptables -D INPUT -s %s -j DROP"
  private static final String RESTORE_OUTPUT_CONNECTIONS = "sudo iptables -D OUTPUT -d %s -j DROP"

  /**
   * Creates list of network disruptions between specified hosts.
   *
   * @param srcHost host whose connections will but cut
   * @param dstHosts destination hosts connections to which from srcHost will be shut down.
   */
  public NetworkShutdownFailure(String srcHost, List<String> dstHosts) {
    super(new ArrayList<String>())
    populateCommandsList(srcHost, dstHosts)
  }

  /**
   * Creates list of network disruptions between specified hosts,
   * allows to set all additional params.
   *
   * @param srcHost host whose connections will but cut
   * @param dstHosts destination hosts connections to which from srcHost will be shut down
   * @param startDelay time in milliseconds) the failures will wait before start
   */
  public NetworkShutdownFailure(String srcHost,
                                List<String> dstHosts,
                                long startDelay) {

    super(new ArrayList<String>(), startDelay)
    populateCommandsList(srcHost, dstHosts)
  }

  /*
   * Populate commands list, making choice between local execution and remote one.
   */

  private void populateCommandsList(String host, List<String> dstHosts) {
    if ("localhost".equalsIgnoreCase(host)) {
      dstHosts.each { dstHost ->
        failCommands.add(String.format(DROP_INPUT_CONNECTIONS, dstHost))
        failCommands.add(String.format(DROP_OUTPUT_CONNECTIONS, dstHost))
        restoreCommands.add(String.format(RESTORE_INPUT_CONNECTIONS, dstHost))
        restoreCommands.add(String.format(RESTORE_OUTPUT_CONNECTIONS, dstHost))
      }
    } else {
      dstHosts.each { dstHost ->
        failCommands.add(getSshWrappedCommand(String.format(DROP_INPUT_CONNECTIONS, dstHost), host))
        failCommands.add(getSshWrappedCommand(String.format(DROP_OUTPUT_CONNECTIONS, dstHost), host))
        restoreCommands.add(getSshWrappedCommand(String.format(RESTORE_INPUT_CONNECTIONS, dstHost), host))
        restoreCommands.add(getSshWrappedCommand(String.format(RESTORE_OUTPUT_CONNECTIONS, dstHost), host))
      }
    }
  }
}
