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
FROM centos:7
MAINTAINER jay@apache.org
RUN yum update -y
RUN yum install -y java-1.7.0-openjdk unzip wget
RUN yum install -y java-1.7.0-openjdk-devel

WORKDIR /opt/

# Get Bigtop
# This comes with a gradlew wrapper we can use.
RUN wget http://www.apache.org/dist/bigtop/bigtop-1.0.0/bigtop-1.0.0-project.tar.gz
RUN  tar -xvf bigtop-1.0.0-project.tar.gz

# Install bigpetstore transaction queue
WORKDIR /opt/bigtop-1.0.0/bigtop-bigpetstore/bigpetstore-transaction-queue
RUN /opt/bigtop-1.0.0/gradlew distZip
RUN unzip build/distributions/bigpetstore-transaction-queue-1.0.zip
RUN mv ./bigpetstore-transaction-queue-1.0 /opt/bigpetstore-transaction-queue-1.0/
CMD /opt/bigpetstore-transaction-queue-1.0/bigpetstore-transaction-queue-1.0/bin/bigpetstore-transaction-queue
