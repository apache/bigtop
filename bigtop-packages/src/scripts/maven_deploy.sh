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

if [ "${DO_MAVEN_DEPLOY}" = "true" ]; then

  SCHEME=${MAVEN_REPO_URI%%:*}
  case ${SCHEME} in
  http | https)
    ;;
  s3)
    mkdir -p ./.mvn
    cp $(dirname ${0})/aws-maven.xml ./.mvn/extensions.xml
    ;;
  *)
    echo "Unsupported URI scheme \"${SCHEME}\""
    exit 1
    ;;
  esac

  if [ "${MAVEN_DEPLOY_SOURCE}" = "true" ]; then
    EXTRA_GOALS+=" source:jar"
  fi
  if [ ! -z "${MAVEN_REPO_URI}" ]; then
    MAVEN_OPTS+=" -DaltDeploymentRepository=${MAVEN_REPO_ID:-default}::default::${MAVEN_REPO_URI}"
  fi
  EXTRA_GOALS+=" deploy"
  # Conditionally add Maven extension for S3 URIs
fi