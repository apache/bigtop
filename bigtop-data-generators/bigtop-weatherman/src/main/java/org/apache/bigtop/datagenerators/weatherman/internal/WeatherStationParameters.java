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

import org.apache.commons.lang3.tuple.Pair;

public class WeatherStationParameters implements Serializable {
  private static final long serialVersionUID = -7268791467819627718L;

  private String WBAN;
  private String city;
  private String state;
  private Pair<Double, Double> coordinates;
  private double temperatureAverage;
  private double temperatureRealCoeff;
  private double temperatureImagCoeff;
  private double temperatureDerivStd;
  private double precipitationAverage;
  private double windSpeedRealCoeff;
  private double windSpeedImagCoeff;
  private double windSpeedK;
  private double windSpeedTheta;

  public WeatherStationParameters(String WBAN, String city, String state,
          Pair<Double, Double> coordinates, double temperatureAverage,
          double temperatureRealCoeff, double temperatureImagCoeff,
          double temperatureDerivStd, double precipitationAverage,
          double windSpeedRealCoeff, double windSpeedImagCoeff,
          double windSpeedK, double windSpeedTheta) {
    this.city = city;
    this.state = state;
    this.coordinates = coordinates;
    this.temperatureAverage = temperatureAverage;
    this.temperatureRealCoeff = temperatureRealCoeff;
    this.temperatureImagCoeff = temperatureImagCoeff;
    this.temperatureDerivStd = temperatureDerivStd;
    this.precipitationAverage = precipitationAverage;
    this.windSpeedRealCoeff = windSpeedRealCoeff;
    this.windSpeedImagCoeff = windSpeedImagCoeff;
    this.windSpeedK = windSpeedK;
    this.windSpeedTheta = windSpeedTheta;
  }

  public String getWBAN() {
    return WBAN;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public Pair<Double, Double> getCoordinates() {
    return coordinates;
  }

  public double getTemperatureAverage() {
    return temperatureAverage;
  }

  public double getTemperatureRealCoeff() {
    return temperatureRealCoeff;
  }

  public double getTemperatureImagCoeff() {
    return temperatureImagCoeff;
  }

  public double getTemperatureDerivStd() {
    return temperatureDerivStd;
  }

  public double getPrecipitationAverage() {
    return precipitationAverage;
  }

  public double getWindSpeedRealCoeff() {
    return windSpeedRealCoeff;
  }

  public double getWindSpeedImagCoeff() {
    return windSpeedImagCoeff;
  }

  public double getWindSpeedK() {
    return windSpeedK;
  }

  public double getWindSpeedTheta() {
    return windSpeedTheta;
  }
}
