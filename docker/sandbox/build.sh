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

source sandbox-env.sh

build() {
    # prepare puppet recipes
    cp -r ../../bigtop-deploy/puppet bigtop-puppet

    # docker build
    docker-compose build --force-rm --no-cache --pull

    if [ $? -eq 0 ]; then
        echo "-------------------------------------------------"
        echo "Image $ACCOUNT/sandbox:$TAG built"
        echo "-------------------------------------------------"
    fi
}

cleanup() {
    rm -rf bigtop-puppet site.yaml.template Dockerfile
}

generate_config() {
    cat > site.yaml.template << EOF
bigtop::hadoop_head_node: ${HEAD_NODE:-"head.node.fqdn"}
hadoop::hadoop_storage_dirs: [/data/1, /data/2]
bigtop::bigtop_repo_uri: ${REPO}
hadoop_cluster_node::cluster_components: [${COMPONENTS}]
bigtop::jdk_package_name: ${JDK}
EOF
}

generate_dockerfile() {
    cat > Dockerfile << EOF
FROM bigtop/puppet:${OS}
ADD startup.sh /startup.sh
ADD bigtop-puppet /bigtop-puppet
ADD bigtop-puppet/hiera.yaml /etc/puppet/hiera.yaml
ADD bigtop-puppet/hieradata /etc/puppet/hieradata
ADD site.yaml.template /etc/puppet/hieradata/site.yaml.template
RUN /startup.sh --bootstrap
CMD /startup.sh --foreground
EOF
}

generate_tag() {
    if [ -z "$TAG" ]; then
        TAG="${OS}_`echo ${COMPONENTS//,/_} | tr -d ' '`"
    fi
}

detect_jdk() {
    for RPM in ${RPMS[*]}; do
        [[ $OS == $RPM ]] && JDK=$RPM_JDK
    done
    for DEB in ${DEBS[*]}; do
        [[ $OS == $DEB ]] && JDK=$DEB_JDK
    done
}

detect_repo() {
    OS_WITH_CODE_NAME=${OS/ubuntu-14.04/ubuntu-trusty}
    REPO=${REPO:-"http://bigtop-repos.s3.amazonaws.com/releases/${BIGTOP_VERSION}/${OS_WITH_CODE_NAME/-//}/x86_64"}
}

image_config_validator() {
    invalid=1
    if [ -z "$ACCOUNT" ]; then
        echo "account unset!"
        invalid=0
    fi
    if [ -z "$OS" ]; then
        echo "operating system unset!"
        invalid=0
    fi
    if [ -z "$TAG" ]; then
        echo "tag unset!"
        invalid=0
    fi
    if [ $invalid -eq 0 ]; then
        usage
    fi
}

deploy_config_validator() {
    invalid=1
    if [ -z "$REPO" ]; then
        echo "repository unset!"
        invalid=0
    fi
    if [ -z "$JDK" ]; then
        echo "jdk unset!"
        invalid=0
    fi
    if [ -z "$COMPONENTS" ]; then
        echo "components unset!"
        invalid=0
    fi
    if [ $invalid -eq 0 ]; then
        usage
    fi
}

show_image_configs() {
    echo "-------------------------------------------------"
    echo "IMAGE CONFIGS:"
    echo "ACCOUNT    $ACCOUNT"
    echo "OS         $OS"
    echo "TAG        $TAG"
    echo "IMAGE      $ACCOUNT/sandbox:$TAG"
    echo "-------------------------------------------------"
}

show_deploy_configs() {
    echo "-------------------------------------------------"
    echo "DEPLOY CONFIGS:"
    echo "REPOSITORY $REPO"
    echo "COMPONENTS $COMPONENTS"
    echo "JDK        $JDK"
    echo "-------------------------------------------------"
}

usage() {
    echo "usage: $PROG args"
    echo "       -a, --account                            Specify account name for image."
    echo "       -c, --components                         Specify components to build."
    echo "                                                    You need to specify a comma separated, quoted string."
    echo "                                                    For example: --components \"hadoop, yarn\""
    echo "       -f, --file                               Specify a written site.yaml config file."
    echo "       -o, --operating-system                   Specify an OS from Bigtop supported OS list."
    echo "                                                    RPM base: ${RPMS[*]}"
    echo "                                                    DEB base: ${DEBS[*]}"
    echo "       -t, --tag                                Specify tag for image."
    echo "                                                    If not specified, this will be auto filled by specified OS and components."
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
    -a|--account)
        if [ $# -lt 2 ]; then
            usage
        fi
        ACCOUNT=$2
        shift 2;;
    -c|--components)
        if [ $# -lt 2 ]; then
            usage
        fi
        COMPONENTS=$2
        shift 2;;
    -f|--file)
        if [ $# -lt 2 ]; then
            usage
        fi
        FILE=$2
        shift 2;;
    -o|--operating-system)
        if [ $# -lt 2 ]; then
            usage
        fi
        OS=$2
        shift 2;;
    -t|--tag)
        if [ $# -lt 2 ]; then
            usage
        fi
        TAG=$2
        shift 2;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done

cleanup
generate_tag
image_config_validator
show_image_configs

if [ -z "$FILE" ]; then
    detect_jdk
    detect_repo
    deploy_config_validator
    generate_config
    show_deploy_configs
else
    if [ -f "$FILE" ]; then
        cp -vfr $FILE site.yaml.template
    else
        echo "$FILE not exist!"
	exit 1
    fi
fi

export ACCOUNT
export TAG
generate_dockerfile
build
cleanup
