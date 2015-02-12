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

package org.apache.bigtop.itest;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*

import org.apache.bigtop.itest.Contract;
import org.apache.bigtop.itest.ParameterSetter;
import org.apache.bigtop.itest.Property;
import org.apache.bigtop.itest.Variable;

@Contract(
  properties = [
  @Property(name = "foo.int1", type = Property.Type.INT, intValue = 1000),
  @Property(name = "foo.int2", type = Property.Type.INT),
  @Property(name = "foo.bar1", type = Property.Type.STRING, defaultValue = "xyz"),
  @Property(name = "foo.bar2", type = Property.Type.STRING),
  @Property(name = "foo.bool1", type = Property.Type.BOOLEAN),
  @Property(name = "foo.bool2", type = Property.Type.BOOLEAN)
  ],
  env = [
  @Variable(name = "HOME"),
  @Variable(name = "BIGTOP_UNLIKELY_FOO_ENV", required = false)
  ]
)
class TestContractGroovy {
  public static int foo_int1;
  public static int foo_int2;
  protected static String foo_bar1;
  protected static String foo_bar2;
  private static boolean foo_bool1;
  private static boolean foo_bool2;

  static String HOME;
  static String BIGTOP_UNLIKELY_FOO_ENV;

  @BeforeClass
  static void setUp() throws ClassNotFoundException, InterruptedException, NoSuchFieldException, IllegalAccessException {
    System.setProperty("foo.int2", "100");
    System.setProperty("foo.bool2", "true")

    ParameterSetter.setProperties(TestContractGroovy.class);
    ParameterSetter.setEnv(TestContractGroovy.class);
  }

  @Test
  void testPropSettings() {
    assertEquals("checking the value of foo_int1 from default value",
      1000, foo_int1);
    assertEquals("checking the value of foo_int2 from foo.int2",
      100, foo_int2);
    assertEquals("checking the value of foo_bar1 from default value",
      "xyz", foo_bar1);
    assertEquals("checking the value of foo_bar2 from unset value",
      "", foo_bar2);
    assertEquals("checking the value of foo_bool1 from unset value",
      false, foo_bool1);
    assertEquals("checking the value of foo_bar2 from foo.bool2",
      true, foo_bool2);
  }

  @Test
  void testEnvSettings() {
    assertEquals("checking the value of \$HOME",
      System.getenv("HOME"), HOME);
    assertEquals("checking the value of \$BIGTOP_UNLIKELY_FOO_ENV",
      null, BIGTOP_UNLIKELY_FOO_ENV);
  }
}
