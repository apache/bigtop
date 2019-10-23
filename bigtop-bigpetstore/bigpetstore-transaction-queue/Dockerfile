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
FROM gradle:5.6.3-jdk12 as build

ADD . /bigpetstore-transaction-queue
WORKDIR /bigpetstore-transaction-queue

RUN [ "gradle", "fatJar" ]

FROM openjdk:14-ea-12-jdk-alpine3.10
LABEL maintainer="jay@apache.org"

WORKDIR /opt/

COPY --from=build /bigpetstore-transaction-queue/build/libs/bigpetstore-transaction-queue-all-1.0.jar /opt

ENTRYPOINT ["java", "-jar", "bigpetstore-transaction-queue-all-1.0.jar"]
