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
 * Class containing utility methods for test classes to use (in a static
 * setup method) for obtaining the values of parameters passed in via
 * environment variables or system properties. The parameters are obtained
 * by introspecting the {@link org.apache.bigtop.itest.Contract Contract}
 * annotation of the test class.
 */
public class ParameterSetter {

  /**
   * Sets the values of parameters passed in via environment variables, using
   * convention.
   * Assumes the presence in the target class of static fields (the parameters)
   * with the same names as the environment variables.
   * (Unix/Linux environment variable names shall consist solely of uppercase
   * letters, digits, and the '_' (underscore) character, and shall not begin
   * with a digit.)
   * If an environment variable is required and it is not set, an
   * AssertionError is thrown.
   *
   * @param target the test class
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  public static void setEnv(Class target)
      throws NoSuchFieldException, IllegalAccessException {
    Contract contract = (Contract) target.getAnnotation(Contract.class);
    Variable[] vars = contract.env();
    for (int i = 0; i < vars.length; i++) {
      Variable var = vars[i];
      String name = var.name();
      Field field = target.getDeclaredField(name);
      String value = System.getenv(name);
      if (value == null && var.required()) {
        assertNotNull(name + " is not set", value);
      }
      field.setAccessible(true);
      field.set(target, value);
    }
  }

  /**
   * Sets the values of parameters passed in via environment variables.
   * Assumes the presence in the target class of static fields with the given
   * names.
   * If an environment variable is required and it is not set, an
   * AssertionError is thrown.
   *
   * @param target     the test class
   * @param fieldNames the names of the static fields corresponding to the
   *                   environment variables; the number of names must match the number of
   *                   environment variables
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  public static void setEnv(Class target, String[] fieldNames)
      throws NoSuchFieldException, IllegalAccessException {
    Contract contract = (Contract) target.getAnnotation(Contract.class);
    Variable[] vars = contract.env();
    assert vars.length == fieldNames.length;
    for (int i = 0; i < vars.length; i++) {
      Variable var = vars[i];
      String name = var.name();
      Field field = target.getDeclaredField(fieldNames[i]);
      String value = System.getenv(name);
      if (value == null && var.required()) {
        assertNotNull(name + " is not set", value);
      }
      field.setAccessible(true);
      field.set(target, value);
    }
  }

  /**
   * Sets the values of parameters passed in via system properties, using
   * convention.
   * Assumes the presence in the target class of static fields (the parameters)
   * with the same names as the system properties, except with '.' replaced by
   * '_'. (The names of the system properties shall be chosen so that the
   * corresponding field names are valid Java identifiers.)
   * If a system property is not set, the parameter is set to a default value.
   * Therefore usable default values must be provided in the annotation or else
   * test logic must be written to handle the lack thereof.
   *
   * @param target the test class
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  public static void setProperties(Class target)
      throws NoSuchFieldException, IllegalAccessException {
    Contract contract = (Contract) target.getAnnotation(Contract.class);
    Property[] props = contract.properties();
    for (int i = 0; i < props.length; i++) {
      Property prop = props[i];
      String name = prop.name();
      Field field = target.getDeclaredField(name.replace('.', '_'));
      Object value = null;
      switch (prop.type()) {
        case STRING:
          value = System.getProperty(name, prop.defaultValue());
          break;
        case INT:
          value = Integer.getInteger(name, prop.intValue());
          break;
        case LONG:
          value = Long.getLong(name, prop.longValue());
          break;
        case BOOLEAN:
          value = Boolean.getBoolean(name);
      }
      field.setAccessible(true);
      field.set(target, value);
    }
  }

  /**
   * Sets the values of parameters passed in via system properties.
   * Assumes the presence in the target class of static fields with the given
   * names.
   * If a system property is not set, the parameter is set to a default value.
   * Therefore usable default values must be provided in the annotation or else
   * test logic must be written to handle the lack thereof.
   *
   * @param target     the test class
   * @param fieldNames the names of the static fields corresponding to the
   *                   system properties; the number of names must match the number of
   *                   system properties
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  public static void setProperties(Class target, String[] fieldNames)
      throws NoSuchFieldException, IllegalAccessException {
    Contract contract = (Contract) target.getAnnotation(Contract.class);
    Property[] props = contract.properties();
    assert props.length == fieldNames.length;
    for (int i = 0; i < props.length; i++) {
      Property prop = props[i];
      String name = prop.name();
      Field field = target.getDeclaredField(fieldNames[i]);
      Object value = null;
      switch (prop.type()) {
        case STRING:
          value = System.getProperty(name, prop.defaultValue());
          break;
        case INT:
          value = Integer.getInteger(name, prop.intValue());
          break;
        case LONG:
          value = Long.getLong(name, prop.longValue());
          break;
        case BOOLEAN:
          value = Boolean.getBoolean(name);
      }
      field.setAccessible(true);
      field.set(target, value);
    }
  }
}
