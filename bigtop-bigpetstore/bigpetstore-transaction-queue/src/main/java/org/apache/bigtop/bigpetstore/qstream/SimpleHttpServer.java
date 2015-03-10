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
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A simple http server for unit testing of HTTP connections.
 */
public class SimpleHttpServer {

    /**
     * Run a web server for args[0] milliseconds...
     * @param args
     */
    public static void main(String[] args){
        try{
            SimpleHttpServer s=new SimpleHttpServer(8123);
            Thread.sleep(Integer.parseInt(args[0]));
            s.stop();
        }
        catch(Exception e){
            e.printStackTrace();;
        }
    }

    HttpServer server = null;
    public SimpleHttpServer(int port) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost",port), 1);
        server.createContext("/", new GetHandler());
        server.createContext("/info", new InfoHandler());
        //server.createContext("/get", new GetHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public void stop(){
        server.stop(1);
    }


    // http://localhost:8000/info
    static class InfoHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = "Add parameters to the request and they will be returned in response.";
            SimpleHttpServer.writeResponse(httpExchange, response.toString());
        }
    }

    /**
     * Response lines that are embedded in the response.
     * This can be used by external programs which need to confirm that
     * their parameters are being correctly encoded.
     */
    public static String responseLine(String key, String value){
        return "the value of " + key + " is " + value;
    }

    static class GetHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            StringBuilder response = new StringBuilder();

            Map <String,String> parms = SimpleHttpServer.queryToMap(httpExchange.getRequestURI().toASCIIString());
            response.append("<html><body>");
            for(String k:parms.keySet())
                response.append(" " + responseLine(k,parms.get(k)));
            response.append("</body></html>");
            SimpleHttpServer.writeResponse(httpExchange, response.toString());
        }
    }

    public static void writeResponse(HttpExchange httpExchange, String response) throws IOException {
        try {
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        catch(Throwable t){
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    public static Map<String, String> queryToMap(String q){
        //after the slash.
        String query=q.substring(2);
        Map<String, String> result = new HashMap<String, String>();
        if(query==null){
            System.out.println("QUERY IS NULL") ;
            return result;
        }
        System.out.println(query);
        for (String param : query.split("&")) {
            System.out.println("reading param on server : " + param + " " + query);
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

}
