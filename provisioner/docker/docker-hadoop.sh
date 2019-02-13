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

usage() {
    echo "usage: $PROG [-C file] args"
    echo "       -C file                                   Use alternate file for config.yaml"
    echo "  commands:"
    echo "       -c NUM_INSTANCES, --create NUM_INSTANCES  Create a Docker based Bigtop Hadoop cluster"
    echo "       -d, --destroy                             Destroy the cluster"
    echo "       -e, --exec INSTANCE_NO|INSTANCE_NAME      Execute command on a specific instance. Instance can be specified by name or number."
    echo "                                                 For example: $PROG --exec 1 bash"
    echo "                                                              $PROG --exec docker_bigtop_1 bash"
    echo "       -E, --env-check                           Check whether required tools has been installed"
    echo "       -l, --list                                List out container status for the cluster"
    echo "       -p, --provision                           Deploy configuration changes"
    echo "       -n, --nexus [NEXUS_URL]                   Configure Nexus proxy to speed up test execution"
    echo "                                                 If no NEXUS_URL specified, default to http://NEXUS_IP:8081/nexus,"
    echo "                                                 where NEXUS_IP is the ip of the container named nexus"
    echo "       -s, --smoke-tests                         Run Bigtop smoke tests"
    echo "       -h, --help"
    exit 1
}

create() {
    if [ -e .provision_id ]; then
        log "Cluster already exist! Run ./$PROG -d to destroy the cluster or delete .provision_id file and containers manually."
        exit 1;
    fi
    echo "`date +'%Y%m%d_%H%M%S'`_R$RANDOM" > .provision_id
    PROVISION_ID=`cat .provision_id`
    # Create a shared /etc/hosts and hiera.yaml that will be both mounted to each container soon
    mkdir -p config/hieradata 2> /dev/null
    echo > ./config/hiera.yaml
    echo > ./config/hosts
    # set correct image name based on running architecture
    image_name=$(get-yaml-config docker image)
    running_arch=$(uname -m)
    if [ "x86_64" == ${running_arch} ]; then
        image_name=${image_name}
    else
        image_name=${image_name}-${running_arch}
    fi
    export DOCKER_IMAGE=${image_name}
    export MEM_LIMIT=$(get-yaml-config docker memory_limit)

    # Startup instances
    docker-compose -p $PROVISION_ID up -d --scale bigtop=$1 --no-recreate
    if [ $? -ne 0 ]; then
        log "Docker container(s) startup failed!";
        exit 1;
    fi

    # Get the headnode FQDN
    NODES=(`docker-compose -p $PROVISION_ID ps -q`)
    hadoop_head_node=`docker inspect --format {{.Config.Hostname}}.{{.Config.Domainname}} ${NODES[0]}`

    # Fetch configurations form specificed yaml config file
    repo=$(get-yaml-config repo)
    components="[`echo $(get-yaml-config components) | sed 's/ /, /g'`]"
    distro=$(get-yaml-config distro)
    enable_local_repo=$(get-yaml-config enable_local_repo)
    generate-config "$hadoop_head_node" "$repo" "$components"

    # Start provisioning
    generate-hosts
    bootstrap $distro $enable_local_repo
    provision
}

generate-hosts() {
    for node in ${NODES[*]}; do
        entry=`docker inspect --format "{{.NetworkSettings.IPAddress}} {{.Config.Hostname}}.{{.Config.Domainname}} {{.Config.Hostname}}" $node`
        docker exec ${NODES[0]} bash -c "echo $entry >> /etc/hosts"
    done
    wait
    # This must be the last entry in the /etc/hosts
    docker exec ${NODES[0]} bash -c "echo '127.0.0.1 localhost' >> ./etc/hosts"
}

generate-config() {
    log "Bigtop Puppet configurations are shared between instances, and can be modified under config/hieradata"
    cat $BIGTOP_PUPPET_DIR/hiera.yaml >> ./config/hiera.yaml
    cp -vfr $BIGTOP_PUPPET_DIR/hieradata ./config/
    cat > ./config/hieradata/site.yaml << EOF
bigtop::hadoop_head_node: $1
hadoop::hadoop_storage_dirs: [/data/1, /data/2]
bigtop::bigtop_repo_uri: $2
hadoop_cluster_node::cluster_components: $3
EOF
}

copy-to-instances() {
    for node in ${NODES[*]}; do
        docker cp  $1 $node:$2 &
    done
    wait
}

bootstrap() {
    for node in ${NODES[*]}; do
        docker exec $node bash -c "/bigtop-home/provisioner/utils/setup-env-$1.sh $2" &
    done
    wait
}

provision() {
    rm -f .error_msg_*
    for node in ${NODES[*]}; do
        (
        bigtop-puppet $node
        result=$?
        if [ $result != 0 ]; then
            log "Failed to provision container $node with exit code $result" > .error_msg_$node
        fi
        ) &
    done
    wait
    cat .error_msg_* 2>/dev/null && exit 1
}

smoke-tests() {
    hadoop_head_node=${NODES:0:12}
    smoke_test_components="`echo $(get-yaml-config smoke_test_components) | sed 's/ /,/g'`"
    docker exec $hadoop_head_node bash -c "bash -x /bigtop-home/provisioner/utils/smoke-tests.sh $smoke_test_components"
}

destroy() {
    docker exec ${NODES[0]} bash -c "umount /etc/hosts; rm -f /etc/hosts"
    if [ -n "$PROVISION_ID" ]; then
        docker-compose -p $PROVISION_ID stop
        docker-compose -p $PROVISION_ID rm -f
    fi
    rm -rvf ./config .provision_id .error_msg*
}

bigtop-puppet() {
    if docker exec $1 bash -c "puppet --version" | grep ^3 >/dev/null ; then
      future="--parser future"
    fi
    docker exec $1 bash -c "puppet apply --detailed-exitcodes $future --modulepath=/bigtop-home/bigtop-deploy/puppet/modules:/etc/puppet/modules:/usr/share/puppet/modules:/etc/puppetlabs/code/environments/production/modules /bigtop-home/bigtop-deploy/puppet/manifests"
}

get-yaml-config() {
    RUBY_EXE=ruby
    if [ $# -eq 1 ]; then
        RUBY_SCRIPT="data = YAML::load(STDIN.read); puts data['$1'];"
    elif [ $# -eq 2 ]; then
        RUBY_SCRIPT="data = YAML::load(STDIN.read); puts data['$1']['$2'];"
    else
        echo "The yaml config retrieval function can only take 1 or 2 parameters.";
        exit 1;
    fi
    cat ${yamlconf} | $RUBY_EXE -ryaml -e "$RUBY_SCRIPT" | tr -d '\r'
}

execute() {
    re='^[0-9]+$'
    if [[ $1 =~ $re ]] ; then
        no=$1
        shift
        docker exec -ti ${NODES[$((no-1))]} $@
    else
        name=$1
        shift
        docker exec -ti $name $@
    fi
}

env-check() {
    echo "Environment check..."
    echo "Check docker:"
    docker -v || exit 1
    echo "Check docker-compose:"
    docker-compose -v || exit 1
    echo "Check ruby:"
    ruby -v || exit 1
}

list() {
    local msg
    msg=$(docker-compose -p $PROVISION_ID ps 2>&1)
    if [ $? -ne 0 ]; then
        msg="Cluster hasn't been created yet."
    fi
    echo "$msg"
}

log() {
    echo -e "\n[LOG] $1\n"
}

configure-nexus() {
    for node in ${NODES[*]}; do
        docker exec $node bash -c "cd /bigtop-home; ./gradlew -PNEXUS_URL=$1 configure-nexus"
    done
    wait
}


PROG=`basename $0`

if [ $# -eq 0 ]; then
    usage
fi

yamlconf="config.yaml"

BIGTOP_PUPPET_DIR=../../bigtop-deploy/puppet
if [ -e .provision_id ]; then
    PROVISION_ID=`cat .provision_id`
fi
if [ -n "$PROVISION_ID" ]; then
    NODES=(`docker-compose -p $PROVISION_ID ps -q`)
fi

while [ $# -gt 0 ]; do
    case "$1" in
    -c|--create)
        if [ $# -lt 2 ]; then
          echo "Create requires a number" 1>&2
          usage
        fi
        env-check
        create $2
        shift 2;;
    -C|--conf)
        if [ $# -lt 2 ]; then
          echo "Alternative config file for config.yaml" 1>&2
          usage
        fi
	yamlconf=$2
        shift 2;;
    -d|--destroy)
        destroy
        shift;;
    -e|--exec)
        if [ $# -lt 3 ]; then
          echo "exec command takes 2 parameters: 1) instance no 2) command to be executed" 1>&2
          usage
        fi
        shift
        execute $@
        shift $#;;
    -E|--env-check)
        env-check
        shift;;
    -l|--list)
        list
        shift;;
    -n|--nexus)
        if [ $# -lt 2 ] || [[ $2 == -* ]]; then
            NEXUS_IP=`docker inspect --format "{{.NetworkSettings.IPAddress}}" nexus`
            if [ $? != 0 ]; then
                log "No container named nexus exists. To create one:\n $ docker run -d --name nexus sonatype/nexus"
                exit 1
            fi
            configure-nexus "http://$NEXUS_IP:8081/nexus"
            shift
        else
            configure-nexus $2
            shift 2
        fi
        ;;
    -p|--provision)
        provision
        shift;;
    -s|--smoke-tests)
        smoke-tests
        shift;;
    -h|--help)
        usage
        shift;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done
