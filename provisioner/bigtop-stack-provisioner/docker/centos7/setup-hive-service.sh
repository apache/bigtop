#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo -e "\033[32mCreating database/user/passwd on ambari-server\033[0m"
docker exec ambari-server bash -c "mysql -uroot -p'root' -e \"CREATE USER 'hive'@'%' IDENTIFIED BY 'hive'\""
docker exec ambari-server bash -c "mysql -uroot -p'root' -e \"GRANT ALL PRIVILEGES ON *.* TO 'hive'@'%' IDENTIFIED BY 'hive'\""
docker exec ambari-server bash -c "mysql -uroot -p'root' -e \"CREATE DATABASE hive\""
docker exec ambari-server bash -c "mysql -uroot -p'root' -e \"FLUSH PRIVILEGES\""

echo -e "\033[32mSetting up jdbc driver on ambari-server\033[0m"
docker exec ambari-server bash -c "ambari-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"

echo -e "\033[32mRestarting ambari-server\033[0m"
docker exec ambari-server bash -c "ambari-server restart --debug"

echo -e "\033[33m
Please select [Existing MySQL/MariaDB]
# MySQL HOST: ambari-server
# MySQL PORT: 3306
# DATABASE NAME: hive
# DATABASE USER NAME: hive
# DATABASE PASSWORD: hive
\033[0m"

echo -e '\033[32mDone!!!\033[0m'