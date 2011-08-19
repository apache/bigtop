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
# Run this script once as root user before the test run. For example:
# mysql -u root -p < /path/to/this/script.sql
#

#
# Drop old databases
#
drop database if exists testhive;

#
# Create new database
#
create database testhive;

#
# Grant permissions to the testhiveuser
#
use mysql;
grant all privileges on testhive.* to 'testhiveuser'@'localhost';
grant all privileges on testhive.* to 'testhiveuser'@'%';
grant all privileges on testhive.* to 'root'@'%';
flush privileges;
