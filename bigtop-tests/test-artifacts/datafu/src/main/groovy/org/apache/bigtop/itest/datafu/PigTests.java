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

package org.apache.bigtop.itest.datafu;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.pig.data.Tuple;
import org.apache.pig.pigunit.PigTest;
import org.apache.pig.tools.parameters.ParseException;

public abstract class PigTests
{    
  protected String[] getDefaultArgs()
  {
    String[] args = {
        "JAR_PATH=" + getJarPath()
      };
    return args;
  }
  
  protected List<String> getDefaultArgsAsList()
  {
    String[] args = getDefaultArgs();
    List<String> argsList = new ArrayList<String>(args.length);
    for (String arg : args)
    {
      argsList.add(arg);
    }
    return argsList;
  }
  
  protected PigTest createPigTest(String scriptPath, String... args) throws IOException
  {
    // append args to list of default args
    List<String> theArgs = getDefaultArgsAsList();
    for (String arg : args)
    {
      theArgs.add(arg);
    }
    
    String[] lines = getLinesFromFile(scriptPath);
    
    for (String arg : theArgs)
    {
      String[] parts = arg.split("=",2);
      if (parts.length == 2)
      {
        for (int i=0; i<lines.length; i++)
        {
          lines[i] = lines[i].replaceAll(Pattern.quote("$" + parts[0]), parts[1]);
        }
      }
    }
    
    return new PigTest(lines);
  }
  
  protected PigTest createPigTest(String scriptPath) throws IOException
  {
    return createPigTest(scriptPath, getDefaultArgs());
  }
  
  protected String getJarPath()
  {
    String jarDir = "dist";
    if (System.getProperty("datafu.jar.dir") != null)
    {
      jarDir = System.getProperty("datafu.jar.dir");
    }
    
    String jarDirPath = new File(/* System.getProperty("user.dir"), */ jarDir).getAbsolutePath();
            
    File userDir = new File(jarDirPath);
    
    String[] files = userDir.list(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name)
      {
        return name.startsWith("datafu") && name.endsWith(".jar") && !name.contains("sources") && !name.contains("javadoc");
      }
      
    });
    
    if (files.length == 0)
    {
      throw new RuntimeException("Could not find JAR file");
    }
    else if (files.length > 1)
    {
      throw new RuntimeException("Found more JAR files than expected");
    }
    
    return  userDir.getAbsolutePath() + "/" + files[0];
  }
  
  protected List<Tuple> getLinesForAlias(PigTest test, String alias) throws IOException, ParseException
  {
    return getLinesForAlias(test,alias,true);
  }
  
  protected List<Tuple> getLinesForAlias(PigTest test, String alias, boolean logValues) throws IOException, ParseException
  {
    Iterator<Tuple> tuplesIterator = test.getAlias(alias);
    List<Tuple> tuples = new ArrayList<Tuple>();
    if (logValues)
    {
      System.out.println(String.format("Values for %s: ", alias));
    }
    while (tuplesIterator.hasNext())
    {
      Tuple tuple = tuplesIterator.next();
      if (logValues)
      {
        System.out.println(tuple.toString());
      }
      tuples.add(tuple);
    }
    return tuples;
  }
    
  protected void writeLinesToFile(String fileName, String... lines) throws IOException
  {
    File inputFile = deleteIfExists(getFile(fileName));
    writeLinesToFile(inputFile, lines);
  }
  
  protected void writeLinesToFile(File file, String[] lines) throws IOException
  {
    FileWriter writer = new FileWriter(file);
    for (String line : lines)
    {
      writer.write(line + "\n");
    }
    writer.close();
  }

  protected void assertOutput(PigTest test, String alias, String... expected) throws IOException, ParseException
  {
    List<Tuple> tuples = getLinesForAlias(test, alias);
    assertEquals(expected.length, tuples.size());
    int i=0;
    for (String e : expected)
    {
      assertEquals(e, tuples.get(i++).toString());
    }
  }
  
  protected File deleteIfExists(File file)
  {
    if (file.exists())
    {
      file.delete();
    }
    return file;
  }
  
  protected File getFile(String fileName)
  {
    return new File(System.getProperty("user.dir"), fileName).getAbsoluteFile();
  }
  
  /**
   * Gets the lines from a given file.
   * 
   * @param relativeFilePath The path relative to the datafu-tests project.
   * @return The lines from the file
   * @throws IOException
   */
  protected String[] getLinesFromFile(String relativeFilePath) throws IOException
  {
    // assume that the working directory is the datafu-tests project
    File file = new File(System.getProperty("user.dir"), relativeFilePath).getAbsoluteFile();
    BufferedInputStream content = new BufferedInputStream(new FileInputStream(file));
    Object[] lines = IOUtils.readLines(content).toArray();
    String[] result = new String[lines.length];
    for (int i=0; i<lines.length; i++)
    {
      result[i] = (String)lines[i];
    }
    return result;
  }
}
