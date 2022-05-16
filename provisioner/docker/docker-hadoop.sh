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
    echo "usage: $PROG [-C file] [-F file] args"
    echo "       -C file                                   - Use alternate file for config.yaml"
    echo "       -F file, --docker-compose-yml file        - Use alternate file for docker-compose.yml"
    echo "  commands:"
    echo "       -c NUM_INSTANCES, --create NUM_INSTANCES  - Create a Docker based Bigtop Hadoop cluster"
    echo "       -d, --destroy                             - Destroy the cluster"
    echo "       -e, --exec INSTANCE_NO|INSTANCE_NAME      - Execute command on a specific instance. Instance can be specified by name or number"
    echo "                                                   For example: $PROG --exec 1 bash"
    echo "                                                                $PROG --exec docker_bigtop_1 bash"
    echo "       -E, --env-check                           - Check whether required tools has been installed"
    echo "       -G, --disable-gpg-check                   - Disable gpg check for the Bigtop repository"
    echo "       -k, --stack COMPONENTS                    - Overwrite the components to deploy defined in config file"
    echo "                                                   COMPONENTS is a comma separated string"
    echo "                                                   For example: $PROG -c 3 --stack hdfs"
    echo "                                                                $PROG -c 3 --stack 'hdfs, yarn, spark'"
    echo "       -l, --list                                - List out container status for the cluster"
    echo "       -L, --enable-local-repo                   - Whether to use repo created at local file system. You can get one by $ ./gradlew repo"
    echo "       -m, --memory MEMORY_LIMIT                 - Overwrite the memory_limit defined in config file"
    echo "       -n, --nexus NEXUS_URL                     - Configure Nexus proxy to speed up test execution"
    echo "                                                   NEXUS_URL is optional. If not specified, default to http://NEXUS_IP:8081/nexus"
    echo "                                                   Where NEXUS_IP is the ip of container named nexus"
    echo "       -p, --provision                           - Deploy configuration changes"
    echo "       -r, --repo REPO_URL                       - Overwrite the yum/apt repo defined in config file"
    echo "       -s, --smoke-tests COMPONENTS              - Run Bigtop smoke tests"
    echo "                                                   COMPONENTS is optional. If not specified, default to smoke_test_components in config file"
    echo "                                                   COMPONENTS is a comma separated string"
    echo "                                                   For example: $PROG -c 3 --smoke-tests hdfs"
    echo "                                                                $PROG -c 3 --smoke-tests 'hdfs, yarn, mapreduce'"
    echo "       -h, --help"
    exit 1
}

create() {
    if [ -e .provision_id ]; then
        log "Cluster already exist! Run ./$PROG -d to destroy the cluster or delete .provision_id file and containers manually."
        exit 1;
    fi
    echo "`date +'%Y%m%d_%H%M%S'`_r$RANDOM" > .provision_id
    PROVISION_ID=`cat .provision_id`
    # Create a shared /etc/hosts and hiera.yaml that will be both mounted to each container soon
    mkdir -p config/hieradata 2> /dev/null
    echo > ./config/hiera.yaml
    echo > ./config/hosts
    # set correct image name based on running architecture
    if [ -z ${image_name+x} ]; then
        image_name=$(get-yaml-config docker image)
    fi
    running_arch=$(uname -m)
    if [ "x86_64" == ${running_arch} ]; then
        image_name=${image_name}
    else
        image_name=${image_name}-${running_arch}
    fi
    export DOCKER_IMAGE=${image_name}

    if [ -z ${memory_limit+x} ]; then
        memory_limit=$(get-yaml-config docker memory_limit)
    fi
    export MEM_LIMIT=${memory_limit}

    # Startup instances
    $DOCKER_COMPOSE_CMD -p $PROVISION_ID up -d --scale bigtop=$1 --no-recreate
    if [ $? -ne 0 ]; then
        log "Docker container(s) startup failed!";
        exit 1;
    fi

    # Get the headnode FQDN
    # shellcheck disable=SC2207
    NODES=(`$DOCKER_COMPOSE_CMD -p $PROVISION_ID ps -q`)
    # shellcheck disable=SC1083
    hadoop_head_node=`docker inspect --format {{.Config.Hostname}}.{{.Config.Domainname}} ${NODES[0]}`

    # Fetch configurations form specificed yaml config file
    if [ -z ${repo+x} ]; then
        repo=$(get-yaml-config repo)
    fi
    if [ -z ${components+x} ]; then
        components="[`echo $(get-yaml-config components) | sed 's/ /, /g'`]"
    fi
    if [ -z ${distro+x} ]; then
        distro=$(get-yaml-config distro)
    fi
    if [ -z ${enable_local_repo+x} ]; then
        enable_local_repo=$(get-yaml-config enable_local_repo)
    fi
    if [ -z ${gpg_check+x} ]; then
        disable_gpg_check=$(get-yaml-config disable_gpg_check)
        if [ -z "${disable_gpg_check}" ]; then
            if [ "${enable_local_repo}" = true ]; then
                # When enabling local repo, set gpg check to false
                gpg_check=false
            else
                gpg_check=true
            fi
        elif [ "${disable_gpg_check}" = true ]; then
            gpg_check=false
        else
            gpg_check=true
        fi
    fi
    generate-config "$hadoop_head_node" "$repo" "$components"

    # Start provisioning
    generate-hosts
    bootstrap $distro $enable_local_repo
    provision
}

generate-hosts() {
    get_nodes
    for node in ${NODES[*]}; do
        entry=`docker inspect --format "{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}} {{.Config.Hostname}}.{{.Config.Domainname}} {{.Config.Hostname}}" $node`
        docker exec ${NODES[0]} bash -c "echo $entry >> /etc/hosts"
    done
    wait
    # This must be the last entry in the /etc/hosts
    docker exec ${NODES[0]} bash -c "echo '127.0.0.1 localhost' >> ./etc/hosts"
}

generate-config() {
    log "Bigtop Puppet configurations are shared between instances, and can be modified under config/hieradata"
    # add ip of all nodes to config
    get_nodes
    for node in ${NODES[*]}; do
        this_node_ip=`docker inspect -f "{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}" $node`
        node_list="$node_list$this_node_ip "
    done
    node_list=$(echo "$node_list" | xargs | sed 's/ /, /g')
    cat $BIGTOP_PUPPET_DIR/hiera.yaml >> ./config/hiera.yaml
    cp -vfr $BIGTOP_PUPPET_DIR/hieradata ./config/

    # A workaround for starting Elasticsearch on ppc64le.
    # See BIGTOP-3574 for details.
    running_arch=$(uname -m)
    if [ "ppc64le" == ${running_arch} ]; then
        elasticsearch_bootstrap_system_call_filter=false
    else
        elasticsearch_bootstrap_system_call_filter=true
    fi

    # Using FairScheduler instead of CapacityScheduler here is a workaround for BIGTOP-3406.
    # Due to the default setting of the yarn.scheduler.capacity.maximum-am-resource-percent
    # property defined in capacity-scheduler.xml (=0.1), some oozie jobs are not assigned
    # enough resource to succeed. But this property can't be set via hiera for now,
    # so we use FairScheduler as an easy workaround.
    cat > ./config/hieradata/site.yaml << EOF
bigtop::hadoop_head_node: $1
hadoop::hadoop_storage_dirs: [/data/1, /data/2]
bigtop::bigtop_repo_uri: $2
bigtop::bigtop_repo_gpg_check: $gpg_check
hadoop_cluster_node::cluster_components: $3
hadoop_cluster_node::cluster_nodes: [$node_list]
hadoop::common_yarn::yarn_resourcemanager_scheduler_class: org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.FairScheduler
elasticsearch::bootstrap::system_call_filter: $elasticsearch_bootstrap_system_call_filter
EOF
}

copy-to-instances() {
    get_nodes
    for node in ${NODES[*]}; do
        docker cp  $1 $node:$2 &
    done
    wait
}

bootstrap() {
    get_nodes
    for node in ${NODES[*]}; do
        docker exec $node bash -c "/bigtop-home/provisioner/utils/setup-env-$1.sh $2" &
    done
    wait
}

provision() {
    rm -f .error_msg_*
    get_nodes
    for node in ${NODES[*]}; do
        (
        bigtop-puppet $node
        result=$?
        # 0: The run succeeded with no changes or failures; the system was already in the desired state.
        # 1: The run failed, or wasn't attempted due to another run already in progress.
        # 2: The run succeeded, and some resources were changed.
        # 4: The run succeeded, and some resources failed.
        # 6: The run succeeded, and included both changes and failures.
        if [ $result != 0 ] && [ $result != 2 ]; then
            log "Failed to provision container $node with exit code $result" > .error_msg_$node
        fi
        ) &
    done
    wait
    cat .error_msg_* 2>/dev/null && exit 1
}

smoke-tests() {
    get_nodes
    hadoop_head_node=${NODES:0:12}
    if [ -z ${smoke_test_components+x} ]; then
        smoke_test_components="`echo $(get-yaml-config smoke_test_components) | sed 's/ /,/g'`"
    fi
    docker exec $hadoop_head_node bash -c "bash -x /bigtop-home/provisioner/utils/smoke-tests.sh $smoke_test_components"
}

destroy() {
    if [ -z ${PROVISION_ID+x} ]; then
        echo "No cluster exists!"
    else
        get_nodes
        docker exec ${NODES[0]} bash -c "umount /etc/hosts; rm -f /etc/hosts"
        if [ -n "$PROVISION_ID" ]; then
            $DOCKER_COMPOSE_CMD -p $PROVISION_ID stop
            $DOCKER_COMPOSE_CMD -p $PROVISION_ID rm -f
        fi
        rm -rvf ./config .provision_id .error_msg*
    fi
}

bigtop-puppet() {
    if docker exec $1 bash -c "puppet --version" | grep ^3 >/dev/null ; then
      future="--parser future"
    fi
    # BIGTOP-3401 Modify Puppet modulepath for puppetlabs-4.12
    docker exec $1 bash -c "puppet apply --detailed-exitcodes $future --hiera_config=/etc/puppet/hiera.yaml --modulepath=/bigtop-home/bigtop-deploy/puppet/modules:/etc/puppet/modules:/usr/share/puppet/modules:/etc/puppetlabs/code/modules:/etc/puppet/code/modules /bigtop-home/bigtop-deploy/puppet/manifests"
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
        get_nodes
        docker exec -ti ${NODES[$((no-1))]} "$@"
    else
        name=$1
        shift
        docker exec -ti $name "$@"
    fi
}

env-check() {
    echo "Environment check..."
    echo "Check docker:"
    docker -v || exit 1
    echo "Check docker-compose:"
    $DOCKER_COMPOSE_CMD -v || exit 1
    echo "Check ruby:"
    ruby -v || exit 1
}

list() {
    local msg
    msg=$($DOCKER_COMPOSE_CMD -p $PROVISION_ID ps 2>&1)
    if [ $? -ne 0 ]; then
        msg="Cluster hasn't been created yet."
    fi
    echo "$msg"
}

log() {
    echo -e "\n[LOG] $1\n"
}

configure-nexus() {
    get_nodes
    for node in ${NODES[*]}; do
        docker exec $node bash -c "cd /bigtop-home; ./gradlew -PNEXUS_URL=$1 configure-nexus"
    done
    wait
}

get_nodes() {
    if [ -n "$PROVISION_ID" ]; then
        # shellcheck disable=SC2207
        NODES=(`$DOCKER_COMPOSE_CMD -p $PROVISION_ID ps -q`)
    fi
}

PROG=`basename $0`

if [ $# -eq 0 ]; then
    usage
fi

yamlconf="config.yaml"
DOCKER_COMPOSE_CMD="docker-compose"

BIGTOP_PUPPET_DIR=../../bigtop-deploy/puppet
if [ -e .provision_id ]; then
    PROVISION_ID=`cat .provision_id`
fi

while [ $# -gt 0 ]; do
    case "$1" in
    -c|--create)
        if [ $# -lt 2 ]; then
          echo "Create requires a number" 1>&2
          usage
        fi
        env-check
        READY_TO_LAUNCH=true
        NUM_INSTANCES=$2
        shift 2;;
    -C|--conf)
        if [ $# -lt 2 ]; then
          echo "Alternative config file for config.yaml" 1>&2
          usage
        fi
	      yamlconf=$2
        shift 2;;
    -F|--docker-compose-yml)
        if [ $# -lt 2 ]; then
          echo "Alternative config file for docker-compose.yml" 1>&2
          usage
        fi
	      DOCKER_COMPOSE_CMD="${DOCKER_COMPOSE_CMD} -f ${2}"
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
        execute "$@"
        shift $#;;
    -E|--env-check)
        env-check
        shift;;
    -G|--disable-gpg-check)
        gpg_check=false
        shift;;
    -k|--stack)
        if [ $# -lt 2 ]; then
          log "No stack specified"
          usage
        fi
        components="[$2]"
        shift 2;;
    -i|--image)
        if [ $# -lt 2 ]; then
          log "No image specified"
          usage
        fi
        image_name=$2
        # Determine distro to bootstrap provisioning environment
        case "${image_name}" in
          *-centos-*|*-fedora-*|*-opensuse-*|*-rockylinux-*)
            distro=centos
            ;;
          *-debian-*|*-ubuntu-*)
            distro=debian
            ;;
          *)
            echo "Unsupported distro [${image_name}]"
            exit 1
            ;;
        esac
        shift 2;;
    -l|--list)
        list
        shift;;
    -L|--enable-local-repo)
        enable_local_repo=true
        shift;;
    -m|--memory)
        if [ $# -lt 2 ]; then
          log "No memory specified. Try --memory 4g"
          usage
        fi
        memory_limit=$2
        shift 2;;
    -n|--nexus)
        if [ $# -lt 2 ] || [[ $2 == -* ]]; then
            NEXUS_IP=`docker inspect --format "{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}" nexus`
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
    -r|--repo)
        if [ $# -lt 2 ]; then
          log "No yum/apt repo specified"
          usage
        fi
        repo=$2
        shift 2;;
    -s|--smoke-tests)
        if [ $# -lt 2 ] || [[ $2 == -* ]]; then
            shift
        else
            smoke_test_components=$2
            shift 2
        fi
        READY_TO_TEST=true
        ;;
    -h|--help)
        usage
        shift;;
    *)
        echo "Unknown argument: '$1'" 1>&2
        usage;;
    esac
done

if [ "$READY_TO_LAUNCH" = true ]; then
    create $NUM_INSTANCES
fi
if [ "$READY_TO_TEST" = true ]; then
    smoke-tests
fi
