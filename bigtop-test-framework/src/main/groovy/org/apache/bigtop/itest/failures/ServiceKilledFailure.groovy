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
 * Can kill (with kill -9) specified service on specified hosts during tests run.
 */
public class ServiceKilledFailure extends AbstractFailure {

  private static final String KILL_SERVICE_TEMPLATE = "sudo pkill -9 -f %s"
  private static final String START_SERVICE_TEMPLATE = "sudo service %s start"

  /**
   * Can kill specified service on specified hosts during tests run.
   *
   * @param hosts list of hosts on which specified service will be killed
   * @param serviceName name of service to be killed.
   */
  public ServiceKilledFailure(List<String> hosts, String serviceName) {
    super(hosts)
    populateCommandsList(hosts, serviceName)
  }

  /**
   * Can kill specified service on specified hosts during tests run.
   *
   * @param hosts list of hosts on which specified service will be killed
   * @param serviceName name of service to be killed
   * @param startDelay time in milliseconds) the failures will wait before start
   */
  public ServiceKilledFailure(List<String> hosts,
                              String serviceName,
                              long startDelay) {

    super(hosts, startDelay)
    populateCommandsList(hosts, serviceName)
  }

  /*
   * Populate commands list, making choice between local execution and remote one.
   */

  private void populateCommandsList(List<String> hosts, String serviceName) {
    if (hosts.size() == 1 && "localhost".equalsIgnoreCase(hosts[0])) {
      failCommands.add(String.format(KILL_SERVICE_TEMPLATE, serviceName))
      restoreCommands.add(String.format(START_SERVICE_TEMPLATE, serviceName))
    } else {
      hosts.each { host ->
        failCommands.add(getSshWrappedCommand(String.format(KILL_SERVICE_TEMPLATE, serviceName), host))
        restoreCommands.add(getSshWrappedCommand(String.format(START_SERVICE_TEMPLATE, serviceName), host))
      }
    }
  }
}
