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

package org.apache.bigtop.itest.posix

import org.apache.bigtop.itest.shell.Shell


class Service {
  private String svc_name;
  private List run_levels;

  private Shell shRoot = new Shell("/bin/bash -s", "root");
  private Shell sh = new Shell("/bin/bash -s");

  public Service(String name) {
    svc_name = name;
  }

  /**
   * Start a System V service via a service(8)
   *
   * @return exit code of a service(8) call 
   */
  public int start() {
    shRoot.exec("service $svc_name start");
    return shRoot.ret;
  }

  /**
   * Stop a System V service via a service(8)
   *
   * @return exit code of a service(8) call 
   */
  public int stop() {
    shRoot.exec("service $svc_name stop");
    return shRoot.ret;
  }

  /**
   * Restart a System V service via a service(8)
   *
   * @return exit code of a service(8) call 
   */
  public int restart() {
    shRoot.exec("service $svc_name restart");
    return shRoot.ret;
  }

  /**
   * init a System V service via a service(8)
   *
   * @return exit code of a service(8) call
   */
  public int init() {
    shRoot.exec("service $svc_name init");
    return shRoot.ret;
  }

  /**
   * Get a status of a System V service via a service(8)
   *
   * @return an output of a service(8) call 
   */
  public String status() {
    sh.exec("service $svc_name status");
    return sh.out;
  }
  /**
   * Returns name of the service
   * @return service name
   */
  public String getName() {
    return svc_name;
  }
  /**
   * Returns a list of runlevels this service is registered for (we do lazy loading of run level info)
   * @return list of run levels as strings (we'd use list of integers but there's "S" run level)
   */
  public List getRunLevels() {
    if (!run_levels) {
      run_levels = [];
      sh.exec("chkconfig --list $svc_name");
      (sh.out.join('') =~ /([0-9Ss]):(off|on)/).each {
        if ("on".equals(it[2])) {
          run_levels.add(it[1]);
        }
      }
    }
    return run_levels;
  }
}
