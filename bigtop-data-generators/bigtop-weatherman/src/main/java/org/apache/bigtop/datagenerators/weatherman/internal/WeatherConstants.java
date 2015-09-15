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

import java.io.File;

public class WeatherConstants {
  public static final File WEATHER_PARAMETERS_FILE = new File(
          "weather_parameters.csv");

  public static final double TEMPERATURE_GAMMA = 0.5; // 2 / day
  public static final int WEATHER_TIMESTEP = 1; // days
  public static final double TEMPERATURE_PERIOD = 365.0; // days

  public static final double PRECIPITATION_A = 0.2;
  public static final double PRECIPITATION_B = 27.0;
  public static final double PRECIPITATION_TO_SNOWFALL = 10.0;

  public static final double WIND_CHILL_PROBABILITY_A = 0.8;
  public static final double WIND_CHILL_PROBABILITY_B = 0.5;
  public static final double WIND_CHILL_PROBABILITY_C = 10.0; // F
  public static final double WIND_CHILL_PROBABILITY_D = 0.2;

  public static final double WIND_SPEED_PROBABILITY_A = -0.5;
  public static final double WIND_SPEED_PROBABILITY_B = 0.8;
  public static final double WIND_SPEED_PROBABILITY_C = 17.5; // mph
  public static final double WIND_SPEED_PROBABILITY_D = 1.0;

  public static final double SNOWFALL_PROBABILITY_A = -0.8;
  public static final double SNOWFALL_PROBABILITY_B = 10.0;
  public static final double SNOWFALL_PROBABILITY_C = 0.75; // in
  public static final double SNOWFALL_PROBABILITY_D = 1.0;

  public static final double RAINFALL_PROBABILITY_A = -0.6;
  public static final double RAINFALL_PROBABILITY_B = 7.5;
  public static final double RAINFALL_PROBABILITY_C = 0.75; // in
  public static final double RAINFALL_PROBABILITY_D = 1.0;

  public static final double M_PER_S_TO_MPH = 2.23694;
}
