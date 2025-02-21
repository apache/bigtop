package org.apache.bigtop.bigpetstore.qstream;

import com.github.rnowling.bps.datagenerator.datamodels.Transaction;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClients;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

/**
 * Consolidated utilities class for dealing with HTTP, serializing transactions,
 * and so on.  This makes the core logic in the implementation classes
 * easier to read.
 * */
public class Utils {

    public static HttpResponse get(String hostnam) throws Exception {
        URI uri = new URI(hostnam);
        return get(uri);
    }

    public static HttpResponse get(String hostname ,  String resource, List<? extends NameValuePair> params) throws Exception {
        System.out.println("getting http "+hostname);
        //URI uri = URIUtils.createURI("http", "www.google.com", -1, "/search",
          URI uri =
                  URIUtils.createURI("http", hostname, -1, resource,
                  URLEncodedUtils.format(params, "UTF-8"), null);
        System.out.println(uri.toASCIIString());
       HttpResponse respo =  get(uri);
       return respo;
    }

    public static HttpResponse get(URI uri) throws Exception {
        HttpGet httppost = new HttpGet(uri);
        HttpClient httpclient = HttpClients.createDefault();
        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httppost);
            if(response.getStatusLine().getStatusCode()!=200)
                System.err.println("FAILURE! " + response.getStatusLine().getStatusCode());
            return response;
        }
        catch (Throwable t) {
            System.out.println("failed, sleeping");
            Thread.sleep(10000);
        }
        System.err.println("FAILURE getting URI " + uri.toASCIIString());
        return null;
    }

    public static String toJson(Transaction t) throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(t) ;
    }

    /**
     * Borrowed from apache StringUtils.
     */
    public static String join(Collection var0, Object var1) {
        StringBuffer var2 = new StringBuffer();

        for(Iterator var3 = var0.iterator(); var3.hasNext(); var2.append(var3.next())) {
            if(var2.length() != 0) {
                var2.append(var1);
            }
        }

        return var2.toString();
    }

}