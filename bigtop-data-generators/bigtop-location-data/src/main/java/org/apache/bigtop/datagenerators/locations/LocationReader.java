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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class LocationReader {
  private static class ZipcodeLocationRecord {
    public final Pair<Double, Double> coordinates;
    public final String state;
    public final String city;

    public ZipcodeLocationRecord(Pair<Double, Double> coordinates, String city,
            String state) {
      this.coordinates = coordinates;
      this.city = city;
      this.state = state;
    }
  }

  private InputStream getResource(File filename) {
    InputStream stream = getClass()
            .getResourceAsStream("/input_data/" + filename);
    return new BufferedInputStream(stream);
  }

  private ImmutableMap<String, Double> readIncomeData(InputStream path)
          throws FileNotFoundException {
    Scanner scanner = new Scanner(path);

    // skip headers
    scanner.nextLine();
    scanner.nextLine();

    Map<String, Double> entries = Maps.newHashMap();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine().trim();
      String[] cols = line.split(",");
      // zipcodes are in the form "ZCTA5 XXXXX"
      String zipcode = cols[2].split(" ")[1].trim();
      try {
        double medianHouseholdIncome = Integer.parseInt(cols[5].trim());
        entries.put(zipcode, medianHouseholdIncome);
      } catch (NumberFormatException e) {

      }
    }

    scanner.close();

    return ImmutableMap.copyOf(entries);
  }

  private ImmutableMap<String, Long> readPopulationData(InputStream path)
          throws FileNotFoundException {
    Scanner scanner = new Scanner(path);

    // skip header
    scanner.nextLine();

    Map<String, Long> entries = Maps.newHashMap();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine().trim();

      if (line.length() == 0)
        continue;

      String[] cols = line.split(",");

      String zipcode = cols[0].trim();
      Long population = Long.parseLong(cols[1].trim());

      if (entries.containsKey(zipcode)) {
        entries.put(zipcode, Math.max(entries.get(zipcode), population));
      } else {
        entries.put(zipcode, population);
      }
    }

    scanner.close();

    return ImmutableMap.copyOf(entries);
  }

  private ImmutableMap<String, ZipcodeLocationRecord> readCoordinates(
          InputStream path) throws FileNotFoundException {
    Scanner scanner = new Scanner(path);

    // skip header
    scanner.nextLine();

    Map<String, ZipcodeLocationRecord> entries = Maps.newHashMap();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine().trim();

      String[] cols = line.split(", ");

      // remove quote marks
      String zipcode = cols[0].substring(1, cols[0].length() - 1);
      String state = cols[1].substring(1, cols[1].length() - 1);
      Double latitude = Double
              .parseDouble(cols[2].substring(1, cols[2].length() - 1));
      Double longitude = Double
              .parseDouble(cols[3].substring(1, cols[3].length() - 1));
      String city = cols[4].substring(1, cols[4].length() - 1);

      Pair<Double, Double> coords = Pair.of(latitude, longitude);

      ZipcodeLocationRecord record = new ZipcodeLocationRecord(coords, city,
              state);

      entries.put(zipcode, record);
    }

    scanner.close();

    return ImmutableMap.copyOf(entries);
  }

  public ImmutableList<Location> readData() throws FileNotFoundException {

    ImmutableMap<String, Double> incomes = readIncomeData(
            getResource(LocationConstants.INCOMES_FILE));
    ImmutableMap<String, Long> populations = readPopulationData(
            getResource(LocationConstants.POPULATION_FILE));
    ImmutableMap<String, ZipcodeLocationRecord> coordinates = readCoordinates(
            getResource(LocationConstants.COORDINATES_FILE));

    Set<String> zipcodeSubset = new HashSet<String>(incomes.keySet());
    zipcodeSubset.retainAll(populations.keySet());
    zipcodeSubset.retainAll(coordinates.keySet());

    List<Location> table = new Vector<Location>();
    for (String zipcode : zipcodeSubset) {
      Location record = new Location(zipcode,
              coordinates.get(zipcode).coordinates,
              coordinates.get(zipcode).city, coordinates.get(zipcode).state,
              incomes.get(zipcode), populations.get(zipcode));
      table.add(record);
    }

    return ImmutableList.copyOf(table);
  }
}
