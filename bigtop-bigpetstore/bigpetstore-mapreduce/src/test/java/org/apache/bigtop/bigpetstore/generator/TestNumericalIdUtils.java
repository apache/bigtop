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
package org.apache.bigtop.bigpetstore.generator;

import static org.junit.Assert.assertFalse;

import org.apache.bigtop.bigpetstore.generator.util.State;
import org.apache.bigtop.bigpetstore.util.NumericalIdUtils;
import org.junit.Test;

public class TestNumericalIdUtils {

    @Test
    public void testName() {
        String strId= State.OK.name()+"_"+ "jay vyas";
        long id = NumericalIdUtils.toId(strId);
        String strId2= State.CO.name()+"_"+ "jay vyas";
        long id2 = NumericalIdUtils.toId(strId2);
        System.out.println(id + " " + id2);
        assertFalse(id==id2);
    }
}