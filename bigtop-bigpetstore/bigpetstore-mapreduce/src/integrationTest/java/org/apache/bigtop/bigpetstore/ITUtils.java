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
package org.apache.bigtop.bigpetstore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.bigtop.bigpetstore.generator.BPSGenerator;
import org.apache.bigtop.bigpetstore.util.BigPetStoreConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.io.Files;

public class ITUtils {
  public static final Path TEST_OUTPUT_DIR = new Path("bps_integration_");

  public static Predicate<String> VERIFICATION_PERDICATE = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return true;
    }
  };

	static final Logger log = LoggerFactory.getLogger(ITUtils.class);

	static FileSystem fs;
	static {
		try {
			fs = FileSystem.getLocal(new Configuration());
		} catch (Throwable e) {
			String cpath = (String) System.getProperties().get("java.class.path");
			String msg = "";
			for (String cp : cpath.split(":")) {
				if (cp.contains("hadoop")) {
					msg += cp.replaceAll("hadoop", "**HADOOP**") + "\n";
				}
			}
			throw new RuntimeException("Major error:  Probably issue.   "
			        + "Check hadoop version?  " + e.getMessage()
			        + " .... check these classpath elements:" + msg);
		}
	}

	public static final Path BPS_TEST_GENERATED =
	        createTestOutputPath(BigPetStoreConstants.OUTPUTS.generated.name());

	//there will be a tsv file under here...
	public static final Path BPS_TEST_PIG_CLEANED_ROOT =
	        createTestOutputPath (BigPetStoreConstants.OUTPUTS.cleaned.name());
	public static Path createTestOutputPath(String... pathParts) {
	  Path path = TEST_OUTPUT_DIR;
	  for(String pathPart: pathParts) {
	    path = new Path(path, pathPart);
	  }
	  return path;
	}

	/**
	 * Some simple checks to make sure that unit tests in local FS. these arent
	 * designed to be run against a distribtued system.
	 */
	public static void checkConf(Configuration conf) throws Exception {
		if (conf.get("mapreduce.jobtracker.address") == null) {
			log.warn("Missing mapreduce.jobtracker.address???????!!!! " + "This can be the case in hive tests which use special "
					+ "configurations, but we should fix it sometime.");
			return;
		}
		if (!conf.get("mapreduce.jobtracker.address").equals("local")) {
			throw new RuntimeException("ERROR: bad conf : " + "mapreduce.jobtracker.address");
		}
		if (!conf.get("fs.AbstractFileSystem.file.impl").contains("Local")) {
			throw new RuntimeException("ERROR: bad conf : " + "mapreduce.jobtracker.address");
		}
		try {
			InetAddress addr = java.net.InetAddress.getLocalHost();
			System.out.println("Localhost = hn=" + addr.getHostName() + " / ha=" + addr.getHostAddress());
		} catch (Throwable e) {
			throw new RuntimeException(" ERROR : Hadoop wont work at all  on this machine yet"
					+ "...I can't get / resolve localhost ! Check java version/ " + "/etc/hosts / DNS or other networking related issues on your box"
					+ e.getMessage());
		}
	}

	/**
	 * Creates a generated input data set in
	 *
	 * test_data_directory/generated. i.e.
	 * test_data_directory/generated/part-r-00000
	 */
	public static void setup() throws Throwable {
		Configuration conf = new Configuration();

		// debugging for Jeff and others in local fs that won't build
		checkConf(conf);

		conf.setInt(BPSGenerator.props.bigpetstore_records.name(), BPSGenerator.DEFAULT_NUM_RECORDS);

		if (FileSystem.getLocal(conf).exists(BPS_TEST_GENERATED)) {
			return;
		}

		Job createInput = BPSGenerator.getCreateTransactionRecordsJob(BPS_TEST_GENERATED, conf);
		createInput.waitForCompletion(true);

		Path outputfile = new Path(BPS_TEST_GENERATED, "part-r-00000");
		List<String> lines = Files.readLines(FileSystem.getLocal(conf).pathToFile(outputfile), Charset.defaultCharset());
		log.info("output : " + FileSystem.getLocal(conf).pathToFile(outputfile));
		for (String l : lines) {
			System.out.println(l);
		}
	}


	// A functions that logs the output file as a verification test
	public static void assertOutput(Path base, Predicate<String> validator) throws Exception {
	  FileSystem fs = FileSystem.getLocal(new Configuration());

	  FileStatus[] files = fs.listStatus(base);
	  // print out all the files.
	  for (FileStatus stat : files) {
	    System.out.println(stat.getPath() + "  " + stat.getLen());
	  }

	  /**
	   * Support map OR reduce outputs
	   */
	  Path partm = new Path(base, "part-m-00000");
	  Path partr = new Path(base, "part-r-00000");
	  Path p = fs.exists(partm) ? partm : partr;

	  /**
	   * Now we read through the file and validate its contents.
	   */
	  BufferedReader r = new BufferedReader(new InputStreamReader(fs.open(p)));

	  // line:{"product":"big chew toy","count":3}
	  while (r.ready()) {
	    String line = r.readLine();
	    log.info("line:" + line);
	    // System.out.println("line:"+line);
	    Assert.assertTrue("validationg line : " + line, validator.apply(line));
	  }
	}

}
