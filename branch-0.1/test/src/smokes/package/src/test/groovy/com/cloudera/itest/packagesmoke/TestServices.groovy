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
package com.cloudera.itest.packagesmoke

import org.junit.Test
import org.junit.runner.RunWith
import com.cloudera.itest.junit.OrderedParameterized
import org.junit.Rule
import org.junit.rules.ErrorCollector
import org.junit.runners.Parameterized.Parameters
import org.junit.AfterClass
import com.cloudera.itest.posix.Service
import com.cloudera.itest.junit.OrderedParameterized.RunStage
import org.hamcrest.Matcher
import static org.hamcrest.core.IsEqual.equalTo

@RunWith(OrderedParameterized.class)
class TestServices {
  Map.Entry svcDescr;
  List<Service> svcs;
  StateVerifier verifier;

  @Rule
  public ErrorCollector errors = new ErrorCollector();

  @Parameters
  static Map<String, Object[]> generateTests() {
    // Look for how it gets reset by CreateServiceState class to understand
    // why it works in the upgrade scenario case
    return selectServices(System.getProperty("cdh.repo.version", "3"));
  }

  TestServices(Map.Entry svc) {
    svcDescr = svc;
    svcs = svcDescr.value.services.collect { new Service(it); };
    verifier = svcDescr.value.verifier;
  }

  static Map<String, Object[]> selectServices(String CDHrelease) {
    Map<String, Object[]> res = [:];
    CDHServices.getServices(CDHrelease).each {
      String name = it.key.toString();
      res[name] = ([it] as Object[]);
    }
    return res;
  }

  @RunStage(level=-1)
  @Test
  void createState() {
    svcs.each {
      checkThat("failed to configure service ${it.getName()}",
                verifier.config(), equalTo(true));
    }

    svcs.each {
      checkThat("service ${it.getName()} failed to start",
                it.start(), equalTo(0));
    }

    sleep(60001);
    verifier.createState();
    checkThat("initial state verification failed",
              verifier.verifyState(), equalTo(true));

    svcs.reverseEach {
      checkThat("service ${it.getName()} failed to stop",
                it.stop(), equalTo(0));
    }
    sleep(5001);
  }

  @RunStage(level=1)
  @Test
  void verifyState() {
    svcs.each {
      checkThat("service ${it.getName()} failed to start",
                it.start(), equalTo(0));
    }
    sleep(60001);
    checkThat("state verification failed after daemons got restarted",
              verifier.verifyState(), equalTo(true));

    svcs.reverseEach { it.stop(); }
  }

  public void checkThat(String msg, Object value, Matcher<Object> matcher) {
    PackageTestErrorProxy.checkThat(errors, msg, value, matcher);
  }
}
