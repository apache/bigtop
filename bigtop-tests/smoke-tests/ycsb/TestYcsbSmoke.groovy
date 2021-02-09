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

import org.junit.BeforeClass
import org.junit.AfterClass
import org.apache.bigtop.itest.shell.Shell
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import org.junit.Test
import org.apache.bigtop.itest.JarContent
import org.apache.bigtop.itest.TestUtils
import org.junit.runner.RunWith
import org.apache.bigtop.itest.shell.Shell
import static org.apache.bigtop.itest.LogErrorsUtils.logError
import static org.junit.Assert.assertTrue

class TestYcsbSmoke {
  static Shell sh = new Shell("/bin/bash -s");
  static Shell shHBase = new Shell("hbase shell");

  static final String YCSB_HOME = System.getenv("YCSB_HOME");
  static final String ycsb_workloadA = YCSB_HOME + "/workloads/workloada ";

  static final String create_tbl = "n_splits = 20; create 'usertable', 'cf', {SPLITS => (1..n_splits).map {|i| \"user#{1000+i*(9999-1000)/n_splits}\"}}";

  static final String basic_load = "ycsb load basic ";
  static final String hbase_load = "ycsb load hbase20 ";
  static final String basic_run = "ycsb run basic ";
  static final String hbase_run = "ycsb run hbase20 ";

  static final String record_cnt = "recordcount=500000 ";
  static final String op_cnt = "operationcount=500000 ";

  @BeforeClass
  static void YcsbSetUp() {
    shHBase.exec(create_tbl);

    sh.exec("mkdir -p " + YCSB_HOME + "/hbase20-binding/conf");
    sh.exec("cp /etc/hbase/conf/hbase-site.xml /usr/lib/ycsb/hbase20-binding/conf");
  }

  @Test
  public void BasicDBTest() {
    sh.exec(basic_load
      + "-P " + ycsb_workloadA
      + "-p " + record_cnt
      + "-s >/dev/null 2>load_basic.dat"
    );
    logError(sh);
    assertTrue("YCSB basic load failed." + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);

    sh.exec(basic_run
      + "-P " + ycsb_workloadA
      + "-p " + record_cnt
      + "-p measurementtype=timeseries "
      + "-p timeseries.granularity=2000 "
      + "-threads 10 -target 100 "
      + "-s >/dev/null 2>transactions_basic.dat"
    );
    logError(sh);
    assertTrue("YCSB basic run failed." + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);
  }

  @Test
  public void HbaseTest() {
    sh.exec(hbase_load
      + "-P " + ycsb_workloadA
      + "-p table=usertable -p columnfamily=cf "
      + "-p " + record_cnt + "-p " + op_cnt
      + "-threads 10 "
    );
    logError(sh);
    assertTrue("YCSB HBase load failed." + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);

    sh.exec(hbase_run
      + "-P " + ycsb_workloadA
      + "-p table=usertable -p columnfamily=cf "
      + "-p " + record_cnt + "-p " + op_cnt
      + "-threads 10 "
    );
    logError(sh);
    assertTrue("YCSB HBase run failed." + sh.getOut() + " " + sh.getErr(), sh.getRet() == 0);
  }
}
