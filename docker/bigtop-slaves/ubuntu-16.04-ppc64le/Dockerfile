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
FROM bigtop/puppet:ubuntu-16.04-ppc64le
MAINTAINER Amir Sanjar

COPY bigtop_toolchain /etc/puppet/modules/bigtop_toolchain

RUN puppet apply -e "include bigtop_toolchain::installer"
COPY . /tmp/bigtop
#RUN git git clone https://github.com/OpenPOWER-BigData/frontend-maven-plugin.git
RUN wget https://github.com/OpenPOWER-BigData/frontend-maven-plugin/archive/master.zip && \
    unzip master.zip && \
    cd /frontend-maven-plugin-master && \
    /usr/local/apache-maven-3.3.9/bin/mvn clean install -DskipTests
RUN cd ..
RUN rm -rf frontend-maven-plugin-master
RUN wget https://github.com/ibmsoe/phantomjs/releases/download/2.1.1/phantomjs-2.1.1-linux-ppc64.tar.bz2
RUN tar xvf phantomjs-2.1.1-linux-ppc64.tar.bz2 && \
    cp /phantomjs-2.1.1-linux-ppc64/bin/phantomjs /usr/bin && \
    rm -rf phantomjs-2.1.1-linux-ppc64
RUN cd /tmp/bigtop && ./gradlew && cd && rm -rf /tmp/bigtop
