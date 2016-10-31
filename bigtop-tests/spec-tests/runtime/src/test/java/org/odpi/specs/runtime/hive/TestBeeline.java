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
	  
	  @BeforeClass
	  public static void checkHiveHome(){
		  results = HiveHelper.execCommand(new CommandLine("echo").addArgument("$HIVE_HOME"));
		  Assert.assertEquals("HIVE_HOME is not in the current path.", "", Integer.parseInt(results.get("outputStream")));
		  TestBeeline.beelineUrl = System.getProperty(URL);
		  TestBeeline.beelineUser = System.getProperty(USER);
		  TestBeeline.beelinePasswd = System.getProperty(PASSWD);
		  
		  // Create Url with username and/or passowrd to handle all ways to connect to beeline
		  
		  if (beelineUser != null && beelineUser != "") { beelineUrl = beelineUrl+" -n "+beelineUser; }
		  else if (beelineUser != null && beelineUser != "" && beelinePasswd != null && beelinePasswd != "") { beelineUrl = beelineUrl+" -n "+beelineUser+" -p "+"beelinePasswd"; }
		  
	  }
	  
	  @Test
	  public static void checkBeeline() {
	    
	    LOG.info("URL is " + beelineUrl); 
	    LOG.info("User is " + beelineUser);
	    LOG.info("Passwd is " + beelinePasswd); 
	    LOG.info("Passwd is null " + (beelinePasswd == null));
	    
	    results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl));
	    String consoleMsg = results.get("outputStream").toLowerCase();
	    //System.out.println(consoleMsg);
	    try {
			Assert.assertEquals("beeline is using beelineUrl", true, consoleMsg.contains("connecting to "+beelineUrl) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
			LOG.info("Beeline -u PASSED.");
		} catch (AssertionError e) {
			// TODO Auto-generated catch block
			LOG.error("Beeline -u FAILED.");
			LOG.error(results.get("outputStream"));
		}
	    
 	  }
	  
	  @Test
	  public static void checkBeelineConnect(){
		  try(PrintWriter out = new PrintWriter("connect.url")){ out.println("!connect " + beelineUrl+";"); out.println("!quit"); } 
		  catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		  results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -f connect.url",false));
		  String consoleMsg = results.get("outputStream").toLowerCase();
		   
		    try {
				Assert.assertEquals("beeline is able to connect to " +beelineUrl, true, consoleMsg.contains("connecting to "+beelineUrl) && !consoleMsg.contains("error") && !consoleMsg.contains("exception") );
				LOG.info("Beeline !connect PASSED.");
			} catch (AssertionError e) {
				// TODO Auto-generated catch block
				LOG.error("Beeline !connect FAILED.");
				LOG.error(results.get("outputStream"));
			}  
	  }
	  
	  @Test
	  public static void checkBeelineHelp(){
		   results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("--help"));
		  String consoleMsg = results.get("outputStream").toLowerCase();
		    try {
				Assert.assertEquals("beeline help works", true, consoleMsg.contains("usage: java org.apache.hive.cli.beeline.beeLine" ) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
				LOG.info("Beeline --help PASSED.");
			} catch (AssertionError e) {
				// TODO Auto-generated catch block
				LOG.error("Beeline --help FAILED.");
				LOG.error(results.get("outputStream"));
			}  
	  }

	  @Test
	  public static void checkBeelineQueryExecFromCmdLine(){
		  results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("SHOW DATABASES"));
		  
		  if(!results.get("outputStream").contains("odpi_runtime_hive")){
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive"));
				
			}else{
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive"));
				results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("CREATE DATABASE odpi_runtime_hive"));
			
			}
		  String consoleMsg = results.get("outputStream").toLowerCase();
		  try {
				Assert.assertEquals("beeline execution works", true, consoleMsg.contains("odpi_runtime_hive" ) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
				LOG.info("Beeline -e PASSED.");
			} catch (AssertionError e) {
				// TODO Auto-generated catch block
				LOG.error("Beeline -e FAILED.");
				LOG.error(results.get("outputStream"));
			}  
		  	
		  HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("-e").addArgument("DROP DATABASE odpi_runtime_hive"));		    
	  }
	  
	  @Test
	  public static void checkBeelineQueryExecFromFile() throws FileNotFoundException{
		  
			try(PrintWriter out = new PrintWriter("beeline-f1.sql")){ out.println("SHOW DATABASES;"); }
			try(PrintWriter out = new PrintWriter("beeline-f2.sql")){ out.println("CREATE DATABASE odpi_runtime_hive;"); }
			try(PrintWriter out = new PrintWriter("beeline-f3.sql")){ out.println("DROP DATABASE odpi_runtime_hive;"); out.println("CREATE DATABASE odpi_runtime_hive;"); }
		 	try(PrintWriter out = new PrintWriter("beeline-f4.sql")){ out.println("DROP DATABASE odpi_runtime_hive;"); }
		  results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -f beeline-f1.sql",false));

		  if(!results.get("outputStream").contains("odpi_runtime_hive")){
				results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -f beeline-f2.sql",false));
				
			}else{
				results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -f beeline-f3.sql",false));
			}
		  results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -f beeline-f1.sql",false));
		  
		  String consoleMsg = results.get("outputStream").toLowerCase();
		  try {
				Assert.assertEquals("beeline execution with file works", true, consoleMsg.contains("odpi_runtime_hive" ) && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
				LOG.info("Beeline -f PASSED.");
			} catch (AssertionError e) {
				// TODO Auto-generated catch block
				LOG.error("Beeline -f FAILED.");
				LOG.error(results.get("outputStream"));
			}  
		  
		  HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -f beeline-f4.sql",false));		    
	  }
	  
	  public static void checkBeelineInitFile() throws FileNotFoundException{
		  
			try(PrintWriter out = new PrintWriter("beeline-i1.sql")){ out.println("SHOW DATABASES;"); }
			try(PrintWriter out = new PrintWriter("beeline-i2.sql")){ out.println("CREATE DATABASE odpi_runtime_beeline_init;"); }
			try(PrintWriter out = new PrintWriter("beeline-i3.sql")){ out.println("DROP DATABASE odpi_runtime_beeline_init;"); out.println("CREATE DATABASE odpi_runtime_beeline_init;"); }
		 	try(PrintWriter out = new PrintWriter("beeline-i4.sql")){ out.println("DROP DATABASE odpi_runtime_beeline_init;"); }
		 	
		  results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -i beeline-i1.sql",false));
	  
		  if(!results.get("outputStream").contains("odpi_runtime_beeline_init")){
				results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -i beeline-i2.sql",false));
				
			}else{
				results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -i beeline-i3.sql",false));
			}
		  results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -i beeline-i1.sql",false));
		  String consoleMsg = results.get("outputStream").toLowerCase();
		  try {
				Assert.assertEquals("beeline execution with init file works", true, consoleMsg.contains("odpi_runtime_beeline_init") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
				LOG.info("Beeline -i PASSED.");
			} catch (AssertionError e) {
				// TODO Auto-generated catch block
				LOG.error("Beeline -i FAILED.");
				LOG.error(results.get("outputStream"));
			}  

		  HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" -i beeline-i4.sql",false));		    
	  }
	  
	  public static void checkBeelineHiveVar() throws FileNotFoundException{
		  
			try(PrintWriter out = new PrintWriter("beeline-hv1.sql")){ out.println("SHOW DATABASES;"); }
			try(PrintWriter out = new PrintWriter("beeline-hv2.sql")){ out.println("CREATE DATABASE ${db};"); }
			try(PrintWriter out = new PrintWriter("beeline-hv3.sql")){ out.println("DROP DATABASE ${db};"); out.println("CREATE DATABASE ${db};"); }
		 	try(PrintWriter out = new PrintWriter("beeline-hv4.sql")){ out.println("DROP DATABASE ${db};"); }
		 	
		  results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv1.sql",false));
		  String consoleMsg = results.get("outputStream");
		  
		  if(!results.get("outputStream").contains("odpi_runtime_beeline_hivevar")){
				results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv2.sql",false));
				
			}else{
				results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv3.sql",false));
			}
		  results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv1.sql",false));
		  consoleMsg = results.get("outputStream").toLowerCase();

		  try {
				Assert.assertEquals("beeline execution with hivevar file works", true, consoleMsg.contains("odpi_runtime_beeline_hivevar") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
				LOG.info("Beeline --hivevar PASSED.");
			} catch (AssertionError e) {
				// TODO Auto-generated catch block
				LOG.error("Beeline --hivevar FAILED.");
				LOG.error(results.get("outputStream"));
			}  
		  	
		  HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("beeline -u "+beelineUrl+" --hivevar db=odpi_runtime_beeline_hivevar -i beeline-hv4.sql",false));		    
	  }
	  
	  @Test
	  public static void CheckBeelineFastConnect(){
		   results = HiveHelper.execCommand(new CommandLine("beeline").addArgument("-u").addArgument(beelineUrl).addArgument("--fastConnect=false"));
		  String consoleMsg = results.get("outputStream").toLowerCase();
		    
		    try {
				Assert.assertEquals("beeline fastConnect works", true, consoleMsg.contains("set fastconnect to true to skip") && !consoleMsg.contains("error") && !consoleMsg.contains("exception"));
				LOG.info("Beeline --fastConnect PASSED.");
			} catch (AssertionError e) {
				// TODO Auto-generated catch block
				LOG.error("Beeline --fastConnect FAILED.");
				LOG.error(results.get("outputStream"));
			}  
	  }
	  
	  @AfterClass
	  public static void cleanup() throws FileNotFoundException {
	    
		  	results = HiveHelper.execCommand(new CommandLine("/bin/sh").addArgument("-c").addArgument("rm -rf beeline*.sql", false));
			
	  }


	  
}
