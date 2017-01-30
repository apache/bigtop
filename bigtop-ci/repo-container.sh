#!/bin/bash

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

docker commit container-$OS repo-$OS
case $OS in 
*debian*|*ubuntu*) R=apt;;
*) R=yum;;
esac
docker run --name repo-container-$OS repo-$OS $R
mkdir -p output
docker cp repo-container-$OS:/var/lib/jenkins/bigtop/output .
docker rm -v repo-container-$OS
