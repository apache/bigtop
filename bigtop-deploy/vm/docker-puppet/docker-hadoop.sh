#!/bin/bash

usage() {
    echo "usage: $PROG [options]"
    echo "       -b, --build-image                         Build base Docker image for Bigtop Hadoop"
    echo "                                                 (must be exectued at least once before creating cluster)"
    echo "       -c NUM_INSTANCES, --create=NUM_INSTANCES  Create a docker based Bigtop Hadoop cluster"
    echo "       -p, --provision                           Deploy configuration changes"
    echo "       -d, --destroy                             Destroy the cluster"
    echo "       -h, --help"
    exit 1
}

build-image() {
    vagrant up image --provider docker
    {
        echo "echo -e '\nBUILD IMAGE SUCCESS.\n'" |vagrant ssh image
    } || {
        >&2 echo -e "\nBUILD IMAGE FAILED!\n"
	exit 2
    }
    vagrant destroy image -f
}

create() {
    echo "\$num_instances = $1" > config.rb
    vagrant up --no-parallel
    nodes=(`vagrant status |grep running |awk '{print $1}'`)
    hadoop_head_node=(`echo "hostname -f" |vagrant ssh ${nodes[0]} |tail -n 1`)
    echo "/vagrant/provision.sh $hadoop_head_node" |vagrant ssh ${nodes[0]}
    bigtop-puppet ${nodes[0]}
    for ((i=1 ; i<${#nodes[*]} ; i++)); do
        (
        echo "/vagrant/provision.sh $hadoop_head_node" |vagrant ssh ${nodes[$i]}
        bigtop-puppet ${nodes[$i]}
        ) &
    done
    wait
}

provision() {
    nodes=(`vagrant status |grep running |awk '{print $1}'`)
    for node in $nodes; do
        bigtop-puppet $node &
    done
    wait
}

destroy() {
    vagrant destroy -f
    rm -f ./hosts
}

bigtop-puppet() {
    echo "puppet apply -d --confdir=/bigtop-puppet --modulepath=/bigtop-puppet/modules /bigtop-puppet/manifests/site.pp" |vagrant ssh $1
}

PROG=`basename $0`
ARGS=`getopt -o "bc:pdh" -l "build-image,create:,provision,destroy,help" -n $PROG -- "$@"`

if [ $? -ne 0 ]; then
    usage
fi

eval set -- "$ARGS"

while true; do
    case "$1" in
    -b|--build-image)
	build-image
        shift;;
    -c|--create)
        create $2
        shift 2;;
    -p|--provision)
        provision
        shift;;
    -d|--destroy)
	destroy
        shift;;
    -h|--help)
        usage
        shift;;
    --)
        shift
        break;;
    esac
done
