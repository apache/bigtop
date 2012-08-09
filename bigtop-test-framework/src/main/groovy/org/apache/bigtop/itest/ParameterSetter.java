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

import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;

/**
   Class containing utility methods for test classes to use (in a static
   setup method) for obtaining the values of parameters passed in via
   environment variables or system properties. The parameters are obtained
   by introspecting the {@link org.apache.bigtop.itest.Parameters [Parameters]}
   annotation of the test class.
 */
public class ParameterSetter {

  public static void setEnv(Class target, String[] fieldNames)
      throws NoSuchFieldException, IllegalAccessException {
    Parameters params = (Parameters) target.getAnnotation(Parameters.class);
    Variable[] vars = params.env();
    assert vars.length == fieldNames.length;
    for (int i = 0; i < vars.length; i++) {
      Variable var = vars[i];
      Field field = target.getField(fieldNames[i]);
      String value = System.getenv(var.name());
      if (value == null && var.required()) {
	assertNotNull(var.name() + " is not set", value);
      }
      field.set(target, value);
    }
  }

  public static void setProperties(Class target, String[] fieldNames)
      throws NoSuchFieldException, IllegalAccessException {
    Parameters params = (Parameters) target.getAnnotation(Parameters.class);
    Property[] props = params.properties();
    assert props.length == fieldNames.length;
    for (int i = 0; i < props.length; i++) {
      Property prop = props[i];
      Field field = target.getField(fieldNames[i]);
      Object value = null;
      switch (prop.type()) {
      case STRING:
	value = System.getProperty(prop.name(), prop.defaultValue());
	break;
      case INT:
	value = Integer.getInteger(prop.name(), prop.intValue());
	break;
      case LONG:
	value = Long.getLong(prop.name(), prop.longValue());
	break;
      case BOOLEAN:
	value = Boolean.getBoolean(prop.name());
      }
      field.set(target, value);
    }
  }
}
