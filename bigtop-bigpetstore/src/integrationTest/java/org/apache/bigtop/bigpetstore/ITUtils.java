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

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.bigtop.bigpetstore.generator.BPSGenerator;
import org.apache.bigtop.bigpetstore.util.BigPetStoreConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class ITUtils {

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
			throw new RuntimeException("Major error:  Probably issue.   " + "Check hadoop version?  " + e.getMessage()
					+ " .... check these classpath elements:" + msg);
		}
	}
	public static final Path BPS_TEST_GENERATED = fs.makeQualified(new Path("bps_integration_",
			BigPetStoreConstants.OUTPUTS.generated.name()));
	public static final Path BPS_TEST_PIG_CLEANED = fs.makeQualified(new Path("bps_integration_",
			BigPetStoreConstants.OUTPUTS.cleaned.name()));
	public static final Path BPS_TEST_MAHOUT_IN = fs.makeQualified(new Path("bps_integration_",
			BigPetStoreConstants.OUTPUTS.MAHOUT_CF_IN.name()));
	public static final Path BPS_TEST_MAHOUT_OUT = fs.makeQualified(new Path("bps_integration_",
			BigPetStoreConstants.OUTPUTS.MAHOUT_CF_OUT.name()));

	public static void main(String[] args) {

	}

	// public static final Path CRUNCH_OUT = new
	// Path("bps_integration_",BigPetStoreConstants.OUTPUT_3).makeQualified(fs);

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
		int records = 10;
		/**
		 * Setup configuration with prop.
		 */
		Configuration conf = new Configuration();

		// debugging for jeff and others in local fs
		// that wont build
		checkConf(conf);

		conf.setInt(BPSGenerator.props.bigpetstore_records.name(), records);

		/**
		 * Only create if doesnt exist already.....
		 */
		if (FileSystem.getLocal(conf).exists(BPS_TEST_GENERATED)) {
			return;
		}

		/**
		 * Create the data set.
		 */
		Job createInput = BPSGenerator.createJob(BPS_TEST_GENERATED, conf);
		createInput.waitForCompletion(true);

		Path outputfile = new Path(BPS_TEST_GENERATED, "part-r-00000");
		List<String> lines = Files.readLines(FileSystem.getLocal(conf).pathToFile(outputfile), Charset.defaultCharset());
		log.info("output : " + FileSystem.getLocal(conf).pathToFile(outputfile));
		for (String l : lines) {
			System.out.println(l);
		}
	}
}
