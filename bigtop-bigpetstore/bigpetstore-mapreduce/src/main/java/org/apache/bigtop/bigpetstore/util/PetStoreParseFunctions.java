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

package org.apache.bigtop.bigpetstore.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: This might be dead code.
 */
public class PetStoreParseFunctions {

    String[] headers = { "code", "city", "country", "lat", "lon" };

    public Map<String, Object> parse(String line) {

        Map<String, Object> resultMap = new HashMap<String, Object>();

        List<String> csvObj = null;

        String[] temp = line.split(",");
        csvObj = new ArrayList<String>(Arrays.asList(temp));

        if (csvObj.isEmpty()) {
            return resultMap;
        }

        int k = 0;

        for (String valueStr : csvObj) {

            resultMap.put(headers[k++], valueStr);

        }

        return resultMap;
    }
}