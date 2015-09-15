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
package org.apache.bigtop.datagenerators.weatherman;

import org.joda.time.LocalDate;

/**
 * Weather data for a single day and location
 *
 * @author rnowling
 *
 */
public interface WeatherRecord {

  /**
   * Day
   *
   * @return date
   */
  public abstract LocalDate getDate();

  /**
   * Average daily temperature in units of Fahrenheit
   *
   * @return temperature
   */
  public abstract double getTemperature();

  /**
   * Daily total precipitation measured in millimeters
   *
   * @return total precipitation
   */
  public abstract double getPrecipitation();

  /**
   * Daily average wind speed in meters per second.
   *
   * @return average wind speed
   */
  public abstract double getWindSpeed();

  /**
   * Daily average wind chill in Fahrenheit
   *
   * @return average wind chill
   */
  public abstract double getWindChill();

  /**
   * Daily total rainfall in millimeters
   *
   * @return total rainfall
   */
  public abstract double getRainFall();

  /**
   * Daily total snowfall in centimeters
   *
   * @return total snowfall
   */
  public abstract double getSnowFall();

}