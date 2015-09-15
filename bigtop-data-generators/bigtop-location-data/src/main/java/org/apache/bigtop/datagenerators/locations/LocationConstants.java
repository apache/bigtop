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
package org.apache.bigtop.datagenerators.locations;

import java.io.File;

public class LocationConstants {
  public static final File COORDINATES_FILE = new File("zips.csv");
  public static final File INCOMES_FILE = new File(
          "ACS_12_5YR_S1903/ACS_12_5YR_S1903_with_ann.csv");
  public static final File POPULATION_FILE = new File("population_data.csv");
}
