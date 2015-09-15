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
package org.apache.bigtop.datagenerators.weatherman.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

public class WeatherParametersReader {
  private InputStream getResource(File filename) throws Exception {
    InputStream stream = getClass()
            .getResourceAsStream("/input_data/" + filename);
    return new BufferedInputStream(stream);
  }

  private WeatherStationParameters parseLine(String line) {
    line = line.trim();
    String[] cols = line.split(",");

    String WBAN = cols[0];
    String city = cols[1];
    String state = cols[2];
    double latitude = Double.parseDouble(cols[3]);
    double longitude = Double.parseDouble(cols[4]);
    Pair<Double, Double> coordinates = Pair.of(latitude, longitude);
    double temperatureAverage = Double.parseDouble(cols[5]);
    double temperatureRealCoeff = Double.parseDouble(cols[6]);
    double temperatureImagCoeff = Double.parseDouble(cols[7]);
    double temperatureDerivStd = Double.parseDouble(cols[8]);
    double precipitationAverage = Double.parseDouble(cols[9]);
    double windSpeedRealCoeff = Double.parseDouble(cols[10]);
    double windSpeedImagCoeff = Double.parseDouble(cols[11]);
    double windSpeedK = Double.parseDouble(cols[12]);
    double windSpeedTheta = Double.parseDouble(cols[13]);

    return new WeatherStationParameters(WBAN, city, state, coordinates,
            temperatureAverage, temperatureRealCoeff, temperatureImagCoeff,
            temperatureDerivStd, precipitationAverage, windSpeedRealCoeff,
            windSpeedImagCoeff, windSpeedK, windSpeedTheta);
  }

  public List<WeatherStationParameters> readParameters() throws Exception {
    InputStream inputStream = getResource(
            WeatherConstants.WEATHER_PARAMETERS_FILE);
    Scanner scanner = new Scanner(inputStream);

    // skip header
    scanner.nextLine();

    List<WeatherStationParameters> parameterList = Lists.newArrayList();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      WeatherStationParameters parameters = parseLine(line);
      parameterList.add(parameters);
    }

    scanner.close();

    return parameterList;
  }
}
