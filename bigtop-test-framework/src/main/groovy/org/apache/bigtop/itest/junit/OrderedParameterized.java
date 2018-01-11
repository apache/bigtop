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

package org.apache.bigtop.itest.junit;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * This is a modification of a Parameterized JUnit runner (which has been relicensed
 * under APL for this very hack BIGTOP-18) that takes care of two things:
 * 1. it lets arrange individual tests into ordered sequence of run stages via adding a
 * a &#064;RunStage(level=X) annotation to the desired testcases (default run stage
 * is 0). Later on run stages are executed according to the order of their levels
 * and testcases within the same run stage have no guaranteed order of execution.
 * 2. it lets give names to the parameterized testcases via making the factory method
 * &#064;Parameters return a Map (mapping names to testcases) instead of a List.
 * <p>
 * Here's how to use it:
 * <pre>
 * public class Example {
 *    <b>&#064;RunStage(level=-1)</b>
 *    <b>&#064;Test</b>
 *    public void earlyTest() {
 *    }
 *
 *    <b>&#064;RunStage(level=1)</b>
 *    <b>&#064;Test</b>
 *    public void lateTest() {
 *    }
 *
 *    <b>&#064;Test</b>
 *    public void defaultTest() {
 *      // this test will be executed at run stage 0 (default of level)
 *    }
 *
 *    &#064;Parameters
 *    public static Map&lt;String, Object[]&gt; generateTests() {
 *      HashMap&lt;String, Object[]&gt; res = new HashMap();
 *      res.put("test name", new Object[] {1, 2});
 * return res;
 *    }
 * }
 * </pre>
 */
public class OrderedParameterized extends Suite {
  /**
   * Annotation for a method which provides parameters to be injected into the
   * test class constructor by <code>Parameterized</code>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface RunStage {
    int level() default 0;
  }

  ;

  private class TestClassRunnerForParameters extends
      BlockJUnit4ClassRunner {
    private final String fParameterSetNumber;

    private final Map<String, Object[]> fParameterList;

    TestClassRunnerForParameters(Class<?> type,
                                 Map<String, Object[]> parameterList, String i) throws InitializationError {
      super(type);
      fParameterList = parameterList;
      fParameterSetNumber = i;
    }

    @Override
    public Object createTest() throws Exception {
      return getTestClass().getOnlyConstructor().newInstance(
          computeParams());
    }

    private Object[] computeParams() throws Exception {
      try {
        return fParameterList.get(fParameterSetNumber);
      } catch (ClassCastException e) {
        throw new Exception(String.format(
            "%s.%s() must return a Map from Strings to arrays.",
            getTestClass().getName(), getParametersMethod(
            getTestClass()).getName()));
      }
    }

    @Override
    protected List<FrameworkMethod> getChildren() {
      List<FrameworkMethod> c = super.getChildren();
      Collections.sort(c, new Comparator<FrameworkMethod>() {
        public int compare(FrameworkMethod m1, FrameworkMethod m2) {
          RunStage r1 = m1.getAnnotation(RunStage.class);
          RunStage r2 = m2.getAnnotation(RunStage.class);
          return ((r1 != null) ? r1.level() : 0) -
              ((r2 != null) ? r2.level() : 0);
        }
      });
      return c;
    }

    @Override
    protected String getName() {
      return String.format("[%s]", fParameterSetNumber);
    }

    @Override
    protected String testName(final FrameworkMethod method) {
      return String.format("%s[%s]", method.getName(),
          fParameterSetNumber);
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
      validateOnlyOneConstructor(errors);
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
      return childrenInvoker(notifier);
    }
  }

  private FrameworkMethod getParametersMethod(TestClass testClass)
      throws Exception {
    List<FrameworkMethod> methods = testClass
        .getAnnotatedMethods(Parameterized.Parameters.class);
    for (FrameworkMethod each : methods) {
      int modifiers = each.getMethod().getModifiers();
      if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
        return each;
    }

    throw new Exception("No public static parameters method on class "
        + testClass.getName());
  }

  private final ArrayList<Runner> runners = new ArrayList<Runner>();

  @Override
  protected List<Runner> getChildren() {
    return runners;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object[]> getParametersList(TestClass klass) throws Throwable {
    return (Map<String, Object[]>) getParametersMethod(klass).invokeExplosively(null);
  }

  public OrderedParameterized(Class<?> klass) throws Throwable {
    super(klass, Collections.<Runner>emptyList());
    Map<String, Object[]> parametersMap = getParametersList(getTestClass());
    for (Map.Entry<String, Object[]> entry : parametersMap.entrySet())
      runners.add(new TestClassRunnerForParameters(getTestClass().getJavaClass(),
          parametersMap, entry.getKey()));
  }
}
