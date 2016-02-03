/**
 * Marker interface for fast-running tests.
 *
 * Priority of Test Categories is in the following order
 * EssentialTests > NormalTests > LongRunTests> PerformanceTests
 * Tests are categorized at both method and class level. A category when defined
 * at class level means all the test methods in that test class fall under that
 * category. To avoid rerunning a test method which falls under a category at
 * method level and under another at class level, we have used <exclude.groups>
 * in files/pom.xml. When a certain category is excluded all the tests
 * which fall under that category are not executed.
 * "A category defined at class level can be superseded only by a higher priority
 * category in the method level"
 * In case of NormalTests category, EssentialTests is excluded because in case of
 * conflicts the later is defined at method level.
 */

package org.apache.bigtop.itest.interfaces;


public interface  LongRunTests {}



class TestDummyForJavadoc_LT {


        public void test_LT() {

        }


}

