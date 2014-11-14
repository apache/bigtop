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
package org.apache.bigtop.bigpetstore.etl;

import java.io.Serializable;

public class LineItem implements Serializable{

    public LineItem(String appName, String storeCode, Integer lineId, String firstName, String lastName, String timestamp, Double price, String description){
        super();
        this.appName=appName;
        this.storeCode=storeCode;
        this.lineId=lineId;
        this.firstName=firstName;
        this.lastName=lastName;
        this.timestamp=timestamp;
        this.price=price;
        this.description=description;
    }

    String appName;
    String storeCode;
    Integer lineId;
    String firstName;
    String lastName;
    String timestamp;
    Double price;
    String description;

    public LineItem(){
        super();
    }

    public String getAppName(){
        return appName;
    }

    public void setAppName(String appName){
        this.appName=appName;
    }

    public String getStoreCode(){
        return storeCode;
    }

    public void setStoreCode(String storeCode){
        this.storeCode=storeCode;
    }

    public int getLineId(){
        return lineId;
    }

    public void setLineId(int lineId){
        this.lineId=lineId;
    }

    public String getFirstName(){
        return firstName;
    }

    public void setFirstName(String firstName){
        this.firstName=firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public void setLastName(String lastName){
        this.lastName=lastName;
    }

    public String getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(String timestamp){
        this.timestamp=timestamp;
    }

    public double getPrice(){
        return price;
    }

    public void setPrice(double price){
        this.price=price;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description=description;
    }

    // other constructors, parsers, etc.
}