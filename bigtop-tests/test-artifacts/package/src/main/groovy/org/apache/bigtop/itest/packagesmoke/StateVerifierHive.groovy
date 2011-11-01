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

package org.apache.bigtop.itest.packagesmoke

import org.apache.bigtop.itest.shell.Shell

class StateVerifierHive extends StateVerifier {
  final static String schema = "CREATE TABLE state_table(A INT) PARTITIONED BY (dt STRING) row format delimited fields terminated by ','  escaped by '\\\\\\\\' stored as textfile;";

  Shell sh = new Shell();

  public void createState() {
    File tmpFile = File.createTempFile("StateVerifierHiveData", ".txt");
    tmpFile.withWriter { (1..15).each { num -> it.write("$num\n"); } };

    sh.exec("hive -e \"${schema}\"");
    ["2008-08-08", "2008-08-09"].each {
      sh.exec("hive -e \"LOAD DATA LOCAL INPATH '${tmpFile.getCanonicalPath()}' OVERWRITE INTO TABLE state_table partition (dt='$it');\"");
    }
    tmpFile.delete();
  }

  public boolean verifyState() {
    sh.exec("hive -e 'SELECT COUNT(*) from state_table;'")
    return (sh.getOut() =~ /30/).find();
  }
}
