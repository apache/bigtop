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
package org.apache.bigtop.itest.packagesmoke;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageTestMatchers {
  static public class MapHasKeys extends TypeSafeMatcher<Map<Object, Object>> {
    private Map<Object, Object> golden_map;

    public MapHasKeys(Map<Object, Object> map) {
      golden_map = map;
    }

    @Override
    public boolean matchesSafely(Map<Object, Object> map) {
      if (map.size() != golden_map.size()) {
        return false;
      }
      for (Map.Entry<Object, Object> entry : map.entrySet()) {
        if (!golden_map.containsKey(entry.getKey())) {
          return false;
        }
      }
      return true;
    }

    public void describeTo(Description description) {
      description.appendText("<[");
      for (Map.Entry<Object, Object> entry : golden_map.entrySet()) {
        description.appendText(entry.getKey().toString() + "=" + entry.getKey().toString() + ", ");
      }
      description.appendText("]>");
    }

    @Factory
    public static <T> Matcher<Map<Object, Object>> hasSameKeys(Map<Object, Object> map) {
      return new MapHasKeys(map);
    }

    @Factory
    public static <T> Matcher<Map<Object, Object>> hasSameKeys(List<Object> list) {
      return new MapHasKeys(mapify(list));
    }

    @Factory
    public static <T> Matcher<Map<Object, Object>> hasSameKeys(String text) {
      HashMap<Object, Object> map = new HashMap<Object, Object>();
      map.put(text, text);
      return new MapHasKeys(map);
    }

    private static Map<Object, Object> mapify(List<Object> list) {
      HashMap<Object, Object> map = new HashMap<Object, Object>();
      if (list != null) {
        for (Object it : list) {
          map.put(it, it);
        }
      }
      return map;
    }
  }
}