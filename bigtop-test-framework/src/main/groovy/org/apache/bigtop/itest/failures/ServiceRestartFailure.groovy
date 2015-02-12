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
 * Can restart specified services on specified hosts during tests execution.
 */
public class ServiceRestartFailure extends AbstractFailure {

  private static final String STOP_SERVICE_TEMPLATE = "sudo service %s stop"
  private static final String START_SERVICE_TEMPLATE = "sudo service %s start"

  /**
   * Can restart specified service on specified hosts during tests run.
   *
   * @param hosts list of hosts on which specified service will be restarted
   * @param serviceName name of service to be restarted.
   */
  public ServiceRestartFailure(List<String> hosts, String serviceName) {
    super(hosts)
    populateCommandsList(hosts, serviceName)
  }

  /**
   * Can gracefully restart specified service on specified hosts during tests run.
   *
   * @param hosts list of hosts on which specified service will be restarted
   * @param serviceName name of service to be restarted
   * @param startDelay time in milliseconds) the failures will wait before start
   */
  public ServiceRestartFailure(List<String> hosts,
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
      failCommands.add(String.format(STOP_SERVICE_TEMPLATE, serviceName))
      restoreCommands.add(String.format(START_SERVICE_TEMPLATE, serviceName))
    } else {
      hosts.each { host ->
        failCommands.add(getSshWrappedCommand(String.format(STOP_SERVICE_TEMPLATE, serviceName), host))
        restoreCommands.add(getSshWrappedCommand(String.format(START_SERVICE_TEMPLATE, serviceName), host))
      }
    }
  }
}
