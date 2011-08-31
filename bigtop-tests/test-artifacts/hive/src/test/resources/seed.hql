-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
DROP TABLE srcpart;
CREATE TABLE srcpart(key string, value string) PARTITIONED BY (ds string, hr string) STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/kv1.txt' OVERWRITE INTO TABLE srcpart PARTITION (ds='2008-04-08',hr='11');
LOAD DATA LOCAL INPATH 'seed_data_files/kv1.txt' OVERWRITE INTO TABLE srcpart PARTITION (ds='2008-04-08',hr='12');
LOAD DATA LOCAL INPATH 'seed_data_files/kv1.txt' OVERWRITE INTO TABLE srcpart PARTITION (ds='2008-04-09',hr='11');
LOAD DATA LOCAL INPATH 'seed_data_files/kv1.txt' OVERWRITE INTO TABLE srcpart PARTITION (ds='2008-04-09',hr='12');
DROP TABLE srcbucket;
CREATE TABLE srcbucket(key int, value string) CLUSTERED BY (key) INTO 2 BUCKETS STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/srcbucket0.txt' INTO TABLE srcbucket;
LOAD DATA LOCAL INPATH 'seed_data_files/srcbucket1.txt' INTO TABLE srcbucket;
DROP TABLE srcbucket2;
CREATE TABLE srcbucket2(key int, value string) CLUSTERED BY (key) INTO 4 BUCKETS STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/srcbucket20.txt' INTO TABLE srcbucket2;
LOAD DATA LOCAL INPATH 'seed_data_files/srcbucket21.txt' INTO TABLE srcbucket2;
LOAD DATA LOCAL INPATH 'seed_data_files/srcbucket22.txt' INTO TABLE srcbucket2;
LOAD DATA LOCAL INPATH 'seed_data_files/srcbucket23.txt' INTO TABLE srcbucket2;
DROP TABLE src;
CREATE TABLE src(key string, value string) STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/kv1.txt' INTO TABLE src;
DROP TABLE src1;
CREATE TABLE src1(key string, value string) STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/kv3.txt' INTO TABLE src1;
DROP TABLE src_sequencefile;
CREATE TABLE src_sequencefile(key string, value string) STORED AS SEQUENCEFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/kv1.seq' INTO TABLE src_sequencefile;
DROP TABLE src_thrift;
CREATE TABLE src_thrift(key string, value string) 
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.thrift.ThriftDeserializer'
WITH SERDEPROPERTIES ('serialization.class' = 'org.apache.hadoop.hive.serde2.thrift.test.Complex',
                      'serialization.format' = 'com.facebook.thrift.protocol.TBinaryProtocol')
STORED AS SEQUENCEFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/complex.seq' INTO TABLE src_thrift;
DROP TABLE src_json;
CREATE TABLE src_json(json string) STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'seed_data_files/json.txt' INTO TABLE src_json;
