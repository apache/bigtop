/*
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
package com.example.myapexapp;

import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.annotation.Stateless;
import com.datatorrent.common.util.BaseOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class ConsoleOutputOperator extends BaseOperator
{
  private static final Logger logger = LoggerFactory.getLogger(ConsoleOutputOperator.class);
  public final transient DefaultInputPort<Object> input = new DefaultInputPort() {
    public void process(Object t) {
      String s;
      if(ConsoleOutputOperator.this.stringFormat == null) {
        s = t.toString();
      } else {
        s = String.format(ConsoleOutputOperator.this.stringFormat, new Object[]{t});
      }

      if(!ConsoleOutputOperator.this.silent) {
        System.out.println(s);
      }

      if(ConsoleOutputOperator.this.debug) {
        ConsoleOutputOperator.logger.info(s);
      }

    }
  };
  public boolean silent = false;
  private boolean debug;
  private String stringFormat;

  public ConsoleOutputOperator() {
  }

  public boolean isSilent() {
    return this.silent;
  }

  public void setSilent(boolean silent) {
    this.silent = silent;
  }

  public boolean isDebug() {
    return this.debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public String getStringFormat() {
    return this.stringFormat;
  }

  public void setStringFormat(String stringFormat) {
    this.stringFormat = stringFormat;
  }
}
