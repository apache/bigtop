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

#
# Run this script as testhiveuser, specifically for the testhive database.
# This script must be run after the mysql-create-db.sql has been run as root.
# Example of command to run this script:
# mysql testhive -u testhiveuser < /path/to/this/script.sql
#

#
# Drop test_table
#
drop table if exists test_table;

#
# Create test_table
#
create table test_table (a integer primary key, b varchar(32));

#
# Load table data
#
insert into test_table values (1, 'one'), (2, 'two'), (3, 'three'), (4, 'four'),
   (5, 'five'), (6, 'six'), (7, 'seven'), (8, 'eight'), (9, 'nine'), (10, 'ten'),
   (11, 'eleven'), (12, 'twelve');
