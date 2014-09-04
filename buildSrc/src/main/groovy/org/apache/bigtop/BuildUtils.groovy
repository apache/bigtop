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
package org.apache.bigtop

class BuildUtils {

  def evaluateBOM = { map, eval ->
    if (eval.contains("\$(")) {
      // Crazy case of using make's subst
      if (eval.contains("\$(subst")) {
        // Extracting name of the var from something like
        // $(subst -,.,$(BIGTOP_VERSION))
        def pattern = ~ /.*\$\(subst -,.,\$\((\w+[-]?\w+?)\)\)/
        def m = eval =~ pattern
        def token = ""
        if (m.matches()) {
          token = m[0][1]
        }
        eval = map.get(token).replaceAll("-", ".")
        return eval
      }
      // Extracting all variable names that might or not be separated by dash
      def pattern = ~/\$\((\w+[-]?\w+?)\)/
      def m = eval =~ pattern
      def counter = 0
      // Moving forward while matches are found
      while (m.find()) {
        eval = eval.replaceAll(/\$\(/, "").replaceAll(/\)/,"")
        (1..m.groupCount()).each { i ->
          def token = m[counter++][i]
          assert map.get(token) != null
          eval = eval.replaceAll(token, map.get(token))
        }
      }
    }
    eval
  }
}