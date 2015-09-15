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

import java.io.Serializable;

import org.apache.bigtop.datagenerators.weatherman.WeatherRecord;
import org.joda.time.LocalDate;

public class WeatherRecordBuilder implements Serializable, WeatherRecord {
  private static final long serialVersionUID = -6397575341624071821L;

  private LocalDate date;
  private Double temperature;
  private Double precipitation;
  private Double windSpeed;
  private Double windChill;
  private Double rainFall;
  private Double snowFall;

  public WeatherRecordBuilder(LocalDate date) {
    this.date = date;
  }

  public WeatherRecordBuilder(LocalDate date, double temperature,
          double precipitation, double windSpeed, double windChill,
          double rainFall, double snowFall) {
    this.date = date;
    this.temperature = temperature;
    this.precipitation = precipitation;
    this.windSpeed = windSpeed;
    this.windChill = windChill;
    this.rainFall = rainFall;
    this.snowFall = snowFall;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.bigtop.bigpetstore.datagenerator.generators.weather.
   * WeatherRecord#getDate()
   */
  @Override
  public LocalDate getDate() {
    return date;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.bigtop.bigpetstore.datagenerator.generators.weather.
   * WeatherRecord#getTemperature()
   */
  @Override
  public double getTemperature() {
    return temperature;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.bigtop.bigpetstore.datagenerator.generators.weather.
   * WeatherRecord#getPrecipitation()
   */
  @Override
  public double getPrecipitation() {
    return precipitation;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.bigtop.bigpetstore.datagenerator.generators.weather.
   * WeatherRecord#getWindSpeed()
   */
  @Override
  public double getWindSpeed() {
    return windSpeed;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.bigtop.bigpetstore.datagenerator.generators.weather.
   * WeatherRecord#getWindChill()
   */
  @Override
  public double getWindChill() {
    return windChill;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.bigtop.bigpetstore.datagenerator.generators.weather.
   * WeatherRecord#getRainFall()
   */
  @Override
  public double getRainFall() {
    return rainFall;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.bigtop.bigpetstore.datagenerator.generators.weather.
   * WeatherRecord#getSnowFall()
   */
  @Override
  public double getSnowFall() {
    return snowFall;
  }

  public WeatherRecord build() {
    if (temperature == null || precipitation == null || windSpeed == null
            || windChill == null || rainFall == null || snowFall == null) {
      throw new IllegalStateException("Not all fields have been initialized.");
    }

    return this;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public void setPrecipitation(Double precipitation) {
    this.precipitation = precipitation;
  }

  public void setWindSpeed(Double windSpeed) {
    this.windSpeed = windSpeed;
  }

  public void setWindChill(Double windChill) {
    this.windChill = windChill;
  }

  public void setRainFall(Double rainFall) {
    this.rainFall = rainFall;
  }

  public void setSnowFall(Double snowFall) {
    this.snowFall = snowFall;
  }
}
