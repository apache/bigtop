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
package org.apache.bigtop.datagenerators.namegenerator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

import com.google.common.collect.Maps;

public class NameReader
{
	InputStream path;

	public NameReader() throws Exception
	{
		this.path = getResource(new File("namedb/data/data.dat"));
	}

	private InputStream getResource(File filename) throws Exception
	{
		InputStream stream = getClass().getResourceAsStream("/input_data/" + filename);
		return new BufferedInputStream(stream);
	}

	public Names readData() throws FileNotFoundException
	{
		Scanner scanner = new Scanner(path);

		Map<String, Double> firstNames = Maps.newHashMap();
		Map<String, Double> lastNames = Maps.newHashMap();

		while(scanner.hasNextLine())
		{
			String line = scanner.nextLine();
			String[] cols = line.trim().split(",");

			String name = cols[0];
			double weight = 1.0 / (Double.parseDouble(cols[5]) + 1.0);

			if(cols[4].equals("1"))
				firstNames.put(name, weight);
			if(cols[3].equals("1"))
				lastNames.put(name, weight);
		}

		scanner.close();

		return new Names(firstNames, lastNames);

	}
}
