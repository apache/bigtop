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

package org.apache.bigtop.itest.packagesmoke

import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import org.junit.runner.RunWith

import org.apache.bigtop.itest.junit.OrderedParameterized
import org.apache.bigtop.itest.junit.OrderedParameterized.RunStage

@RunWith(OrderedParameterized.class)
class TestPackagesPseudoDistributedWithRM extends TestPackagesPseudoDistributed {

  public TestPackagesPseudoDistributedWithRM(String pkgName, Node pkgGolden) {
    super(pkgName, pkgGolden);
  }

  @RunStage(level = 1)
  @Test
  void testPackageRemove() {
    checkComplimentary32bitInstall();
    checkDaemonStart();
    sleep(3001);
    // TODO FIXME: CDH-2816 should address the timing of daemons start.
    checkRemoval();
  }
}
