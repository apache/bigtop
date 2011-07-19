/*
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.itest

public class JUnitUtils {
  final private static String DETECT_ERRORS = "totalErrorsDetected";
  final private static String DETECT_FAILS = "totalFailsDetected";
  final private static String RESULTS_DIR = "com.cloudera.itest.JUnitUtils.results.dir";

  /**
   * A bulk executor of JUnit test clases capable of producing XML testresults
   * and forking a VM.
   *
   * @param testClasses an array of JUnit test classes to be executed in a forked VM
   * @return true if ALL the tests passed and false in any other case 
   */
  static boolean executeTests(Class... testClasses) {
    def ant = new AntBuilder()
    def res = ant.junit (printsummary:'yes', fork:'yes', forkmode:'once', 
                         errorproperty: DETECT_ERRORS, failureproperty: DETECT_FAILS) {
          System.getProperties().each { k, v ->
            sysproperty(key: k, value: v)
          }
          classpath {
             System.getProperty('java.class.path').
                    split(System.getProperty('path.separator',':')).each {
               pathelement(location: it);
             }
          }
          testClasses.each {
            test(name: it.getName(), todir: System.getProperty(RESULTS_DIR, '.'));
          }
          formatter(type:'xml');
    }
    return !(ant.project.getProperty(DETECT_FAILS) == "true" ||
             ant.project.getProperty(DETECT_ERRORS) == "true");
  }
}
