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

BIGTOP_HOME=`cd $(dirname $0)/.. && pwd`

usage() {
    echo "usage build.sh --prefix trunk|1.4.0|1.3.0|... --os debian-11|centos-7|... --target hadoop|tez|..."
    echo "   [--nexus] [--preferred-java-version 8|11] [--mvn-cache-volume true|false] [--docker-run-option ...]"
    exit 1 # unknown option
}

 if [ $# -eq 0 ]; then
     usage
 fi

while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -t|--target)
    TARGET="$2"
    shift
    ;;
    -o|--os)
    OS="$2"
    shift
    ;;
    -p|--prefix)
    PREFIX="$2"
    shift
    ;;
    -n|--nexus)
    NEXUS="--net=container:nexus"
    CONFIGURE_NEXUS="configure-nexus"
    shift
    ;;
    --preferred-java-version)
    BIGTOP_PREFERRED_JAVA_VERSION="$2"
    shift
    ;;
    --mvn-cache-volume)
    MVN_CACHE_VOLUME="$2"
    shift
    ;;
    --docker-run-option)
    DOCKER_RUN_OPTION="$2"
    shift
    ;;
    *)
    usage
    ;;
esac
shift
done

if [ -z ${PREFIX+x} ]; then
    echo "PREFIX is required";
    UNSATISFIED=true
fi
if [ -z ${OS+x} ]; then
    echo "OS is required";
    UNSATISFIED=true
fi
if [ "$UNSATISFIED" == true ]; then
    usage
fi

IMAGE_NAME=bigtop/slaves:$PREFIX-$OS
ARCH=$(uname -m)
if [ "x86_64" != $ARCH ]; then
    IMAGE_NAME=$IMAGE_NAME-$ARCH
fi

if [ -n "${BIGTOP_PREFERRED_JAVA_VERSION}" ]; then
  if [ "${BIGTOP_PREFERRED_JAVA_VERSION}" == "8" ]; then
    BIGTOP_PREFERRED_JAVA_VERSION="1.8.0"
  fi
  DOCKER_RUN_OPTION="${DOCKER_RUN_OPTION} -e BIGTOP_PREFERRED_JAVA_VERSION=${BIGTOP_PREFERRED_JAVA_VERSION}"
fi

# Add / Delete named volume for maven cache
MVN_CACHE_VOLUME_NAME="bigtop-mvn-cache-${OS}"
if [ "${MVN_CACHE_VOLUME}" == "true" ]; then
    # Create volume and change the ownership if the volume does not exsit
    if [ -z "$(docker volume ls -q -f name=${MVN_CACHE_VOLUME_NAME})" ]; then
        docker volume create $MVN_CACHE_VOLUME_NAME
        docker run --rm \
          -v ${MVN_CACHE_VOLUME_NAME}:/var/lib/jenkins/.m2 \
          --name bigtop-mvn-cache-init $IMAGE_NAME \
          chown -R jenkins:jenkins /var/lib/jenkins/.m2
    fi
    DOCKER_RUN_OPTION="${DOCKER_RUN_OPTION} -v ${MVN_CACHE_VOLUME_NAME}:/var/lib/jenkins/.m2"
elif [ "${MVN_CACHE_VOLUME}" == "false" ]; then
  docker volume rm --force $MVN_CACHE_VOLUME_NAME
fi

# Start up build container
CONTAINER_ID=`docker run -d $DOCKER_RUN_OPTION $NEXUS $IMAGE_NAME /sbin/init`
trap "docker rm -f $CONTAINER_ID" EXIT

# Copy bigtop repo into container
docker cp $BIGTOP_HOME $CONTAINER_ID:/bigtop
docker cp $BIGTOP_HOME/bigtop-ci/entrypoint.sh $CONTAINER_ID:/bigtop/entrypoint.sh
docker exec $CONTAINER_ID bash -c "chown -R jenkins:jenkins /bigtop"

# Build
docker exec --user jenkins $CONTAINER_ID bash -c "cd /bigtop && ./entrypoint.sh $CONFIGURE_NEXUS $TARGET --info"
RESULT=$?

# save result
mkdir -p output
docker cp $CONTAINER_ID:/bigtop/build .
docker cp $CONTAINER_ID:/bigtop/output .
docker rm -f $CONTAINER_ID

if [ $RESULT -ne 0 ]; then
    exit 1
fi
