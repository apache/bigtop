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

while [[ $# -gt 1 ]]
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
    -n|--nexus)
    NEXUS="--net=container:nexus"
    CONFIGURE_NEXUS="configure-nexus"
    shift
    ;;
    *)
    echo "usage build.sh --os debian-8|centos-7|... --target hadoop|tez|..."
    exit 1 # unknown option
    ;;
esac
shift
done

# prepare source image
cat >Dockerfile <<EOF
FROM bigtop/slaves:$OS
# copy source to container
COPY . /var/lib/jenkins/bigtop
# make it belong to compile account
RUN chown -R jenkins /var/lib/jenkins/bigtop
# define default user jenkins
USER jenkins
WORKDIR /var/lib/jenkins/bigtop
ENTRYPOINT [ "bigtop-ci/entrypoint.sh" ]
# initialize gradle
RUN bigtop-ci/entrypoint.sh
# make it a volume, performancewise
VOLUME /var/lib/jenkins
EOF

# build source image
docker build -t image-$OS .

# run build
docker run --name container-$OS-$TARGET-$$ $NEXUS image-$OS $CONFIGURE_NEXUS $TARGET

# save result
mkdir -p output
docker cp container-$OS-$TARGET-$$:/var/lib/jenkins/bigtop/output .
docker rm -v container-$OS-$TARGET-$$
docker rmi image-$OS
rm -rf Dockerfile
