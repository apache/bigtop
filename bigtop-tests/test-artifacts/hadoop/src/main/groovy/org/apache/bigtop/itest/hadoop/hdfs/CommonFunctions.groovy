/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hadoop.hdfs;
import java.util.ArrayList;
import java.util.List;

public class CommonFunctions {
  /**
   * lookForGivenString check the given string is present in the list data
   */
  private boolean lookForGivenString(List<String> data,
                                     String searchString) {
    boolean result = false;
    for( String output_String : data) {
      if(output_String.contains(searchString)) {
        result = true;
        break;
      }
    }
    return result;
  }
}
