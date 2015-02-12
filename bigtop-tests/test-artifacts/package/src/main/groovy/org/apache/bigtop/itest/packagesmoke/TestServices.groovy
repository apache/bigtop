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
import org.junit.runner.RunWith
import org.apache.bigtop.itest.junit.OrderedParameterized
import org.junit.Rule
import org.junit.rules.ErrorCollector
import org.junit.runners.Parameterized.Parameters
import org.junit.AfterClass
import org.apache.bigtop.itest.posix.Service
import org.apache.bigtop.itest.junit.OrderedParameterized.RunStage
import org.hamcrest.Matcher
import static org.hamcrest.core.IsEqual.equalTo
import org.apache.bigtop.itest.shell.Shell

@RunWith(OrderedParameterized.class)
class TestServices {
  Map.Entry svcDescr;
  List<Service> svcs;
  StateVerifier verifier;
  List<String> killIDs;

  static Shell shRoot = new Shell("/bin/bash", "root");

  @Rule
  public ErrorCollector errors = new ErrorCollector();

  @Parameters
  static Map<String, Object[]> generateTests() {
    // Look for how it gets reset by CreateServiceState class to understand
    // why it works in the upgrade scenario case
    return selectServices(System.getProperty("bigtop.repo.version", "0.6.0"));
  }

  TestServices(Map.Entry svc) {
    svcDescr = svc;
    svcs = svcDescr.value.services.collect { new Service(it); };
    verifier = svcDescr.value.verifier;
    killIDs = svcDescr.value.killIDs;
  }

  static Map<String, Object[]> selectServices(String BTrelease) {
    Map<String, Object[]> res = [:];
    BTServices.getServices(BTrelease).each {
      String name = it.key.toString();
      res[name] = ([it] as Object[]);
    }
    return res;
  }

  @AfterClass
  static void tearDown() {
    // TODO: this is pretty silly, but it'll do for now
    BTServices.serviceDaemonUserNames.each {
      shRoot.exec("kill -9 `ps -U${it} -opid=`");
    }
  }

  @RunStage(level = -1)
  @Test
  void createState() {
    checkThat("failed to configure service ${svcs.get(0).getName()}",
      verifier.config(), equalTo(true));

    svcs.each {
      checkThat("service ${it.getName()} failed to start",
        it.start(), equalTo(0));
    }

    sleep(60001);
    verifier.createState();
    checkThat("initial state verification failed",
      verifier.verifyState(), equalTo(true));

    svcs.reverseEach {
      // TODO: we're only trying the best we can here
      // there's a total eradication of  services happening at @BeforeClass
      it.stop();
      sleep(5001);
    }
    sleep(5001);

    // TODO: this is pretty silly, but it'll do for now
    killIDs.each {
      shRoot.exec("kill -9 `ps -U${it} -opid=`");
    }
  }

  @RunStage(level = 1)
  @Test
  void verifyState() {
    svcs.each {
      checkThat("failed to configure service ${it.getName()}",
        verifier.config(), equalTo(true));
    }

    svcs.each {
      checkThat("service ${it.getName()} failed to start",
        it.start(), equalTo(0));
    }
    sleep(60001);
    checkThat("state verification failed after daemons got restarted",
      verifier.verifyState(), equalTo(true));

    svcs.reverseEach { it.stop(); }
    sleep(5001);
    // lets check if they are really stopped (if not -- we'll complain and kill them)
    killIDs.each {
      shRoot.exec("kill -0 `ps -U${it} -opid=`");
      if (!shRoot.getRet()) {
        shRoot.exec("kill -9 `ps -U${it} -opid=`");
        checkThat("service running under the name of $it is supposed to be stopped, but it is not",
          true, equalTo(false));
      }
    }
  }

  public void checkThat(String msg, Object value, Matcher<Object> matcher) {
    PackageTestErrorProxy.checkThat(errors, msg, value, matcher);
  }
}
