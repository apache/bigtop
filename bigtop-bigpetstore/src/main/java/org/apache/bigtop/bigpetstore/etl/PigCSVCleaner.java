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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bigtop.bigpetstore.util.BigPetStoreConstants.OUTPUTS;
import org.apache.bigtop.bigpetstore.util.DeveloperTools;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;

/**
 * This class operates by ETL'ing the data-set into pig.
 * The pigServer is persisted through the life of the class, so that the
 * intermediate data sets created in the constructor can be reused.
 */
public class PigCSVCleaner  {

    PigServer pigServer;

    private static Path getCleanedTsvPath(Path outputPath) {
      return new Path(outputPath, OUTPUTS.tsv.name());
    }

    public PigCSVCleaner(Path inputPath, Path outputPath, ExecType ex, File... scripts)
            throws Exception {
        FileSystem fs = FileSystem.get(inputPath.toUri(), new Configuration());

        if(! fs.exists(inputPath)){
            throw new RuntimeException("INPUT path DOES NOT exist : " + inputPath);
        }

        if(fs.exists(outputPath)){
            throw new RuntimeException("OUTPUT already exists : " + outputPath);
        }
        // run pig in local mode
        pigServer = new PigServer(ex);

        /**
         * First, split the tabs up.
         *
         * BigPetStore,storeCode_OK,2 1,yang,jay,3,flea collar,69.56,Mon Dec 15 23:33:49 EST 1969
         *
         * ("BigPetStore,storeCode_OK,2", "1,yang,jay,3,flea collar,69.56,Mon Dec 15 23:33:49 EST 1969")
         */
        pigServer.registerQuery("csvdata = LOAD '<i>' AS (ID,DETAILS);".replaceAll("<i>", inputPath.toString()));

        // currentCustomerId, firstName, lastName, product.id, product.name.toLowerCase, product.price, date
        /**
         * Now, we want to split the two tab delimited fields into uniform
         * fields of comma separated values. To do this, we 1) Internally split
         * the FIRST and SECOND fields by commas "a,b,c" --> (a,b,c) 2) FLATTEN
         * the FIRST and SECOND fields. (d,e) (a,b,c) -> d e a b c
         */
        pigServer.registerQuery(
              "id_details = FOREACH csvdata GENERATE "
              + "FLATTEN(STRSPLIT(ID, ',', 3)) AS " +
			"(drop, code, transaction) ,"

              + "FLATTEN(STRSPLIT(DETAILS, ',', 7)) AS " +
                  "(custId, fname, lname, productId, product:chararray, price, date);");
        pigServer.registerQuery("mahout_records = FOREACH id_details GENERATE custId, productId, 1;");
        pigServer.store("id_details", getCleanedTsvPath(outputPath).toString());
        pigServer.store("mahout_records", new Path(outputPath, OUTPUTS.MahoutPaths.Mahout.name()).toString());
        /**
         * Now we run scripts... this is where you can add some
         * arbitrary analytics.
         *
         * We add "input" and "output" parameters so that each
         * script can read them and use them if they want.
         *
         * Otherwise, just hardcode your inputs into your pig scripts.
         */
        int i = 0;
        for(File script : scripts) {
            Map<String,String> parameters = new HashMap<>();
            parameters.put("input", getCleanedTsvPath(outputPath).toString());

            Path dir = outputPath.getParent();
            Path adHocOut = new Path(dir, OUTPUTS.pig_ad_hoc_script.name() + (i++));
            System.out.println("Setting default output to " + adHocOut);
            parameters.put("output", adHocOut.toString());
            pigServer.registerScript(script.getAbsolutePath(), parameters);
        }
    }

    private static File[] files(String[] args,int startIndex) {
        List<File> files = new ArrayList<File>();
        for(int i = startIndex ; i < args.length ; i++) {
            File f = new File(args[i]);
            if(! f.exists()) {
                throw new RuntimeException("Pig script arg " + i + " " + f.getAbsolutePath() + " not found. ");
            }
            files.add(f);
        }
        System.out.println(
                "Ad-hoc analytics:"+
                "Added  " + files.size() + " pig scripts to post process.  "+
                "Each one will be given $input and $output arguments.");
        return files.toArray(new File[]{});
    }

    public static void main(final String[] args) throws Exception {
        System.out.println("Starting pig etl " + args.length);
        Configuration c = new Configuration();
        int res = ToolRunner.run(c, new Tool() {
                    Configuration conf;
                    @Override
                    public void setConf(Configuration conf) {
                        this.conf=conf;
                    }

                    @Override
                    public Configuration getConf() {
                        return this.conf;
                    }

                    @Override
                    public int run(String[] args) throws Exception {
                        DeveloperTools.validate(
                                args,
                                "generated data directory",
                                "pig output directory");
                        new PigCSVCleaner(
                                new Path(args[0]),
                                new Path(args[1]),
                                ExecType.MAPREDUCE,
                                files(args,2));
                        return 0;
                    }
                }, args);
        System.exit(res);
      }
}