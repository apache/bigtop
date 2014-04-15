/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.bigpetstore.etl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.bigtop.bigpetstore.util.BigPetStoreConstants;
import org.apache.bigtop.bigpetstore.util.NumericalIdUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.parse.HiveParser_IdentifiersParser.booleanValue_return;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Hive View creator is designed to read from Pigs cleaned output.
 * The basic strategy is:
 *
 * 1) store pig output as a hive table
 * 2) use "select .. as" to select a subset
 *
 * Note on running locally:
 *
 * 1) Local mode requires a hive and hadoop tarball, with HIVE_HOME and
 * HADOOP_HOME pointing to it. 2) In HADOOP_HOME, you will need to cp the
 * HIVE_HOME/lib/hive-serde*jar file into HADOOP_HOME/lib.
 *
 * Then, the below queries will run.
 *
 * The reason for this is that the hive SerDe stuff is used in the MapReduce
 * phase of things, so those utils need to be available to hadoop itself. That
 * is because the regex input/output is processed vthe mappers
 *
 */
public class HiveViewCreator implements Tool {

    static {
        try{
            Class.forName("org.apache.hadoop.hive.ql.exec.mr.ExecDriver");
            System.out.println("found exec driver !!!!!!!!!!!!!!!!");
        }
        catch(Throwable t) {
            throw new RuntimeException(t);
        }
        try{
            //Class.forName("org.apache.hadoop.hive.ql.exec.mr.ExecDriver");
        }
        catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }
    Configuration conf;
    @Override
    public void setConf(Configuration conf) {
        this.conf=conf;
    }

    @Override
    public Configuration getConf() {
        return conf;
    }

    /**
     * Input args:
     *  Cleaned data files from pig (tsv)
     *  Ouptut table (desired path to mahout input data set)
     *
     */
    @Override
    public int run(String[] args) throws Exception {
        Statement stmt = getConnection();
        stmt.execute("DROP TABLE IF EXISTS " + BigPetStoreConstants.OUTPUTS.MAHOUT_CF_IN.name());
        System.out.println("input data " + args[0]);
        System.out.println("output table " + args[1]);

        Path inTablePath =  new Path(args[0]);
        String inTableName = "cleaned"+System.currentTimeMillis();
        String outTableName = BigPetStoreConstants.OUTPUTS.MAHOUT_CF_IN.name();

        Path outTablePath = new Path (inTablePath.getParent(),outTableName);

        final String create = "CREATE EXTERNAL TABLE "+inTableName+" ("
                + "  dump STRING,"
                + "  state STRING,"
                + "  trans_id STRING,"
                + "  lname STRING,"
                + "  fname STRING,"
                + "  date STRING,"
                + "  price STRING,"
                + "  product STRING"
                + ") ROW FORMAT "
                + "DELIMITED FIELDS TERMINATED BY '\t' "
                + "LINES TERMINATED BY '\n' "
                + "STORED AS TEXTFILE "
                + "LOCATION '"+inTablePath+"'";
        boolean res = stmt.execute(create);
        System.out.println("Execute return code : " +res);
        //will change once we add hashes into pig ETL clean
        String create2 =
                "create table "+outTableName+" as "+
                "select hash(concat(state,fname,lname)),',',hash(product),',',1 "
                + "from "+inTableName;

        System.out.println("CREATE = " + create2  );
        System.out.println("OUT PATH = " + outTablePath);
        boolean res2 = stmt.execute(create2);

        String finalOutput = String.format(
                "INSERT OVERWRITE DIRECTORY '%s' SELECT * FROM %s",outTablePath, outTableName) ;

        stmt.execute(finalOutput);
        System.out.println("FINAL OUTPUT STORED : " + outTablePath);
        return 0;
    }

    public static final String HIVE_JDBC_DRIVER = "org.apache.hive.jdbc.HiveDriver";
    public static final String HIVE_JDBC_EMBEDDED_CONNECTION = "jdbc:hive2://";

    final static Logger log = LoggerFactory.getLogger(HiveViewCreator.class);


    private Statement getConnection() throws ClassNotFoundException,
            SQLException {
        Class.forName(HIVE_JDBC_DRIVER);
        Connection con = DriverManager.getConnection(
                HIVE_JDBC_EMBEDDED_CONNECTION, "", "");
        System.out.println("hive con = " + con.getClass().getName());
        Statement stmt = con.createStatement();
        return stmt;
    }

    public static void main(String[] args) throws Exception {
        new HiveViewCreator()
            .run(args);
    }
}