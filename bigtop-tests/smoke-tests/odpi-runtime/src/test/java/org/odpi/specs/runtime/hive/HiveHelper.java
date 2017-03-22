/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HiveHelper {

    private static final Log LOG = LogFactory.getLog(HiveHelper.class.getName());

    public static Map<String, String> execCommand(CommandLine commandline) {
        return execCommand(commandline, null);
    }

    public static Map<String, String> execCommand(CommandLine commandline,
                                                  Map<String, String> envVars) {

        System.out.println("Executing command:");
        System.out.println(commandline.toString());
        Map<String, String> env = null;
        Map<String, String> entry = new HashMap<String, String>();
        try {
            env = EnvironmentUtils.getProcEnvironment();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            LOG.debug("Failed to get process environment: " + e1.getMessage());
            e1.printStackTrace();
        }
        if (envVars != null) {
            for (String key : envVars.keySet()) {
                env.put(key, envVars.get(key));
            }
        }

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60 * 10000);
        Executor executor = new DefaultExecutor();
        executor.setExitValue(1);
        executor.setWatchdog(watchdog);
        executor.setStreamHandler(streamHandler);
        try {
            executor.execute(commandline, env, resultHandler);
        } catch (ExecuteException e) {
            // TODO Auto-generated catch block
            LOG.debug("Failed to execute command with exit value: " + String.valueOf(resultHandler.getExitValue()));
            LOG.debug("outputStream: " + outputStream.toString());
            entry.put("exitValue", String.valueOf(resultHandler.getExitValue()));
            entry.put("outputStream", outputStream.toString() + e.getMessage());
            e.printStackTrace();
            return entry;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            LOG.debug("Failed to execute command with exit value: " + String.valueOf(resultHandler.getExitValue()));
            LOG.debug("outputStream: " + outputStream.toString());
            entry.put("exitValue", String.valueOf(resultHandler.getExitValue()));
            entry.put("outputStream", outputStream.toString() + e.getMessage());
            e.printStackTrace();
            return entry;
        }

        try {
            resultHandler.waitFor();
            /*System.out.println("Command output: "+outputStream.toString());*/
            entry.put("exitValue", String.valueOf(resultHandler.getExitValue()));
            entry.put("outputStream", outputStream.toString());
            return entry;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
			/*System.out.println("Command output: "+outputStream.toString());*/
            LOG.debug("exitValue: " + String.valueOf(resultHandler.getExitValue()));
            LOG.debug("outputStream: " + outputStream.toString());
            entry.put("exitValue", String.valueOf(resultHandler.getExitValue()));
            entry.put("outputStream", outputStream.toString());
            e.printStackTrace();
            return entry;
        }
    }

    protected static String getProperty(String property, String description) {
        String val = System.getProperty(property);
        if (val == null) {
            throw new RuntimeException("You must set the property " + property + " with " +
                    description);
        }
        LOG.debug(description + " is " + val);
        return val;
    }


}
