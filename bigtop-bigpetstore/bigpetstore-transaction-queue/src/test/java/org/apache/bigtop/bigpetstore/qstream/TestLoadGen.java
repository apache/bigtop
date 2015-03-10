/*
*  Licensed to the Apache Software Foundation (ASF) under one or more
*  contributor license agreements.  See the NOTICE file distributed with
*  this work for additional information regarding copyright ownership.
*  The ASF licenses this file to You under the Apache License, Version 2.0
*  (the "License"); you may not use this file except in compliance with
*  the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

package org.apache.bigtop.bigpetstore.qstream;

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Ignore;

import java.io.File;
import java.util.List;

/**
 * For simplicity, the tests for this application are in this class.
 */
public class TestLoadGen {

    @org.junit.Test
    public void testFileLoadGen(){
        new File("/tmp/transactions0.txt").delete();
        LoadGen.TESTING=true;
        LoadGen.main(new String[]{"/tmp","1","5","1500000","13241234"});
        Assert.assertTrue(new File("/tmp/transactions0.txt").length()>0);
    }

    /**
     * We don't run this test, except if we want to
     * test a live app which has an existing REST
     * API server running.
     */
    @Ignore
    @org.junit.Test
    public void testWebLoadGen(){
        LoadGen.TESTING=true;
        LoadGen.main(new String[]{"http://localhost:3000/rpush/guestbook", "1", "5", "1500000", "13241234"});
    }

    /**
     * This is a generic test that our server wrappers etc are okay.
     * The code paths it exersizes aren't necessarily used via the
     * prime purpose of the app as of the march 1 2015.
     * @throws Exception
     */
    @org.junit.Test
    public void testUtilsParameters() throws Exception {

        final String bind="localhost";
        final int port=8129;
        final String ctx="/";
        final String rsp = "This is the response...";

        SimpleHttpServer server = new SimpleHttpServer(port);

        List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("json", "{\"a\":\"json\"}"));
        params.add(new BasicNameValuePair("abc", "123"));

        HttpResponse r = Utils.get("localhost:8129","", params);
        System.out.println(r );
        String contents = org.apache.commons.io.IOUtils.toString(r.getEntity().getContent());
        System.out.println(contents);
        /**
         * Use responseLine() to test that the lines in the http response
         * which originate from the above parameters are returned exactly.
         */

        Assert.assertTrue(contents.contains(SimpleHttpServer.responseLine("abc","123")));
        Assert.assertTrue(contents.contains(SimpleHttpServer.responseLine("json", "%7B%22a%22%3A%22json%22%7D")));
        server.stop();

    }
}
