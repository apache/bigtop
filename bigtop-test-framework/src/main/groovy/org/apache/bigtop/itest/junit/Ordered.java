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

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This is a variation of a OrderedParametrized JUnit runner that lets arrange individual
 * tests into ordered sequence of run stages via adding a @RunStage(level=X)
 * annotation to the desired testcases (default run stage
 * is 0). Later on run stages are executed according to the order of their levels
 * and testcases within the same run stage have no guaranteed order of execution.
 * <p>
 * Here's how to use it:
 * <pre>
 * public class Example {
 *    <b>@RunStage(level=-1)</b>
 *    <b>@Test</b>
 *    public void earlyTest() {
 *    }
 *
 *    <b>@RunStage(level=1)</b>
 *    <b>@Test</b>
 *    public void lateTest() {
 *    }
 * }
 * </pre>
 */
public class Ordered extends BlockJUnit4ClassRunner {
  /**
   * Annotation for a method which provides parameters to be injected into the
   * test class constructor by <code>Ordered</code>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface RunStage {
    int level() default 0;
  }

  public Ordered(Class klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected List<FrameworkMethod> computeTestMethods() {
    List<FrameworkMethod> c = super.computeTestMethods();
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
}
