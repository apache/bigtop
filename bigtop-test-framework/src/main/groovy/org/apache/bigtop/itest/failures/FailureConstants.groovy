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
 * Constants for cluster failure smoke tests.
 */
public final class FailureConstants {

  /**
   * Env variable which should contain full local path to the file with SSH private key
   * used to remotely login on cluster nodes without password.
   */
  public static final PRIVATE_KEY_PATH_ENV_VAR = "BIGTOP_SMOKES_CLUSTER_IDENTITY_FILE"

  /**
   * Env variable which should contain name of Linux user on the hosts where failures are running,
   * this user should have password-less SSH enabled and privileges to run password-less sudo
   * commands: service stop/start, pkill -9, iptables rules editing.
   */
  public static final BIGTOP_SMOKES_USER = "BIGTOP_SMOKES_USER"
}
