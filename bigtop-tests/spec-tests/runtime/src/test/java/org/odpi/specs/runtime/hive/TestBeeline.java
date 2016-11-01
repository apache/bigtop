package org.odpi.specs.runtime.hive;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;


/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class TestBeeline {

	public static final Log LOG = LogFactory.getLog(TestBeeline.class.getName());

	private static final String URL = "odpiHiveTestJdbcUrl";
	private static final String USER = "odpiHiveTestJdbcUser";
	private static final String PASSWD = "odpiHiveTestJdbcPassword";

	private static Map<String, String> results;

	private static String beelineUrl; 
	private static String beelineUser;
	private static String beelinePasswd;
	private static String testUrl;

	//flags to check if username and password should be added as argument in some tests
	private static boolean bothUserPass = false;
	private static boolean onlyUser = false;

	@BeforeClass
	public static void initialSetup(){

		TestBeeline.beelineUrl = System.getProperty(URL);
		TestBeeline.beelineUser = System.getProperty(USER);
		TestBeeline.beelinePasswd =System.getProperty(PASSWD);
		TestBeeline.testUrl = System.getProperty(URL);

		// Create Url with username and/or passowrd to handle all ways to connect to beeline
		if (beelineUser != null && beelineUser != "" && beelinePasswd != null && beelinePasswd != "") 
		{ 
			testUrl = beelineUrl+" -n "+beelineUser+" -p "+beelinePasswd; 
			bothUserPass=true;
		}
		else if (beelineUser != null && beelineUser != "") 
		{ 
			testUrl = beelineUrl+" -n "+beelineUser; 
			onlyUser=true;
		}
		System.out.println("Setting url"+testUrl); 

		LOG.info("URL is " + beelineUrl); 
		LOG.info("User is " + beelineUser);
		LOG.info("Passwd is " + beelinePasswd); 
		LOG.info("Passwd is null " + (beelinePasswd == null));
	}

	@Test
	public void checkBeeline() {

		System.out.println(beelineUrl);  

		results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(testUrl));
		String consoleMsg = results.get("outputStream").toLowerCase();
		Assert.assertEquals("beeline -u FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("connecting to "+beelineUrl) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));


	}

	@Test
	public void checkBeelineConnect(){
		try(PrintWriter out = new PrintWriter("connect.url")){ out.println("!connect " + beelineUrl+" "+beelineUser+" "+beelinePasswd+";"); out.println("!quit;"); } 
		catch (FileNotFoundException e1) {
			
			e1.printStackTrace();
		}
		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -f connect.url",false));
		String consoleMsg = results.get("outputStream").toLowerCase();


		Assert.assertEquals("beeline !connect FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("connecting to "+beelineUrl) && !consoleMsg.contains("error") && !consoleMsg.contains("exception") );  
	}

	@Test
	public void checkBeelineHelp(){
		results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("--help"));
		String consoleMsg = results.get("outputStream").toLowerCase();
		Assert.assertEquals("beeline --help FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("display this message" ) && consoleMsg.contains("usage: java org.apache.hive.cli.beeline.beeline") && !consoleMsg.contains("exception"));

	}

	@Test
	public void checkBeelineQueryExecFromCmdLine(){

		if (bothUserPass) 
		{ 
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("-e").addArgument("SHOW DATABASES;"));

			if(!results.get("outputStream").contains("odpi_runtime_hive")){

				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("-e").addArgument("SHOW DATABASES;"));
			}else{

				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("-e").addArgument("SHOW DATABASES;"));

			}
			String consoleMsg = results.get("outputStream").toLowerCase();
			Assert.assertEquals("beeline -e FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("odpi_runtime_hive" ) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));

			HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive"));
		}
		else if (onlyUser) 
		{ 
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-e").addArgument("SHOW DATABASES;"));

			if(!results.get("outputStream").contains("odpi_runtime_hive")){

				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-e").addArgument("SHOW DATABASES;"));
			}else{

				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-e").addArgument("SHOW DATABASES;"));

			}
			String consoleMsg = results.get("outputStream").toLowerCase();
			Assert.assertEquals("beeline -e FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("odpi_runtime_hive" ) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));

			HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive"));
		}
		else {
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("SHOW DATABASES;"));

			if(!results.get("outputStream").contains("odpi_runtime_hive")){
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive;"));

			}else{
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive;"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("SHOW DATABASES;"));
			

			}
			String consoleMsg = results.get("outputStream").toLowerCase();
			Assert.assertEquals("beeline -e FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("odpi_runtime_hive" ) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));

			HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive"));
		}
	}

	@Test
	public void checkBeelineQueryExecFromFile() throws FileNotFoundException{

		try(PrintWriter out = new PrintWriter("beeline-f1.sql")){ out.println("SHOW DATABASES;"); }
		try(PrintWriter out = new PrintWriter("beeline-f2.sql")){ out.println("CREATE DATABASE odpi_runtime_hive;"); }
		try(PrintWriter out = new PrintWriter("beeline-f3.sql")){ out.println("DROP DATABASE odpi_runtime_hive;"); out.println("CREATE DATABASE odpi_runtime_hive;"); }
		try(PrintWriter out = new PrintWriter("beeline-f4.sql")){ out.println("DROP DATABASE odpi_runtime_hive;"); }
		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -f beeline-f1.sql",false));

		if(!results.get("outputStream").contains("odpi_runtime_hive")){
			results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -f beeline-f2.sql",false));

		}else{
			results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -f beeline-f3.sql",false));
		}
		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -f beeline-f1.sql",false));

		String consoleMsg = results.get("outputStream").toLowerCase();
		Assert.assertEquals("beeline -f FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("odpi_runtime_hive" ) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));

		HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -f beeline-f4.sql",false));		    
	}

	@Test
	public void checkBeelineInitFile() throws FileNotFoundException{

		try(PrintWriter out = new PrintWriter("beeline-i1.sql")){ out.println("SHOW DATABASES;"); }
		try(PrintWriter out = new PrintWriter("beeline-i2.sql")){ out.println("CREATE DATABASE odpi_runtime_beeline_init;"); }
		try(PrintWriter out = new PrintWriter("beeline-i3.sql")){ out.println("DROP DATABASE odpi_runtime_beeline_init;"); out.println("CREATE DATABASE odpi_runtime_beeline_init;"); }
		try(PrintWriter out = new PrintWriter("beeline-i4.sql")){ out.println("DROP DATABASE odpi_runtime_beeline_init;"); }

		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -i beeline-i1.sql",false));

		if(!results.get("outputStream").contains("odpi_runtime_beeline_init")){
			results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -i beeline-i2.sql",false));

		}else{
			results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -i beeline-i3.sql",false));
		}
		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -i beeline-i1.sql",false));
		String consoleMsg = results.get("outputStream").toLowerCase();
		Assert.assertEquals("beeline -i FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("odpi_runtime_beeline_init") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));

		HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" -i beeline-i4.sql",false));		    
	}

	@Test
	public void checkBeelineHiveVar() throws FileNotFoundException{

		try(PrintWriter out = new PrintWriter("beeline-hv1.sql")){ out.println("SHOW DATABASES;"); }
		try(PrintWriter out = new PrintWriter("beeline-hv2.sql")){ out.println("CREATE DATABASE ${db};"); }
		try(PrintWriter out = new PrintWriter("beeline-hv3.sql")){ out.println("DROP DATABASE ${db};"); out.println("CREATE DATABASE ${db};"); }
		try(PrintWriter out = new PrintWriter("beeline-hv4.sql")){ out.println("DROP DATABASE ${db};"); }

		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv1.sql",false));
		String consoleMsg = results.get("outputStream");

		if(!results.get("outputStream").contains("odpi_runtime_beeline_hivevar")){
			results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv2.sql",false));

		}else{
			results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv3.sql",false));
		}
		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv1.sql",false));
		consoleMsg = results.get("outputStream").toLowerCase();

		Assert.assertEquals("beeline --hivevar FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("odpi_runtime_beeline_hivevar") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));

		HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+testUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv4.sql",false));		    
	}

	@Test
	public void checkBeelineFastConnect(){
		results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(testUrl).addArgument("--fastConnect=false"));
		String consoleMsg = results.get("outputStream").toLowerCase();
		Assert.assertEquals("beeline --fastConnect FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("set fastconnect to true to skip")); 
	}

	@Test
	public void checkBeelineVerbose(){

		//explicit check for username password again as url containing -u -p is not working in single addArgument function with testUrl

		if (bothUserPass) 
		{ 
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("--verbose=true"));
		}
		else if (onlyUser) 
		{ 
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("--verbose=true"));
		}
		else {
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("--verbose=true"));
		}
		String consoleMsg = results.get("outputStream").toLowerCase();
		Assert.assertEquals("beeline --verbose FAILED using url "+testUrl+". \n" +results.get("outputStream"), true, consoleMsg.contains("issuing: !connect jdbc:hive2:") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
	}

	@Test
	public void checkBeelineShowHeader(){
		
		//explicit check for username password again as url containing -u -p is not working in single addArgument function with testUrl

		if (bothUserPass) 
		{ 
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("-p").addArgument(beelinePasswd).addArgument("--showHeader=false").addArgument("-e").addArgument("SHOW DATABASES;"));
		}
		else if (onlyUser) 
		{ 
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-n").addArgument(beelineUser).addArgument("--showHeader=false").addArgument("-e").addArgument("SHOW DATABASES;"));
		}
		else {
			results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("--showHeader=false").addArgument("-e").addArgument("SHOW DATABASES;"));
		}
		String consoleMsg = results.get("outputStream").toLowerCase();
		Assert.assertEquals("beeline --showHeader FAILED. \n" +results.get("outputStream"), true, consoleMsg.contains("default")&&!consoleMsg.contains("database_name") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));

	}

	@AfterClass
	public static void cleanup() throws FileNotFoundException {

		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf beeline*.sql", false));
		results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf connect.url", false));

	}



}
