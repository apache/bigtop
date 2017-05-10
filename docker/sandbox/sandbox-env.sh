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

BIGTOP_VERSION=1.2.0
# REPO="YOUR CUSTOM REPO"

RPMS=( \
    centos-6 \
    centos-7 \
    fedora-25 \
    opensuse-42.1 \
)
RPM_JDK=java-1.8.0-openjdk-devel.x86_64

DEBS=( \
    debian-8 \
    ubuntu-14.04 \
    ubuntu-16.04 \
)
DEB_JDK=openjdk-8-jdk

OS_TO_CODE_NAME() {
    case $1 in
        "ubuntu-14.04") OS_WITH_CODE_NAME="ubuntu-trusty" ;;
        "ubuntu-16.04") OS_WITH_CODE_NAME="ubuntu-xenial" ;;
    esac
    echo $OS_WITH_CODE_NAME
}
