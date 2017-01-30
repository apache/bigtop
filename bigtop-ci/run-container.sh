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

docker run --name container-$OS-$TARGET-$$ $NEXUS image-$OS $CONFIGURE_NEXUS $TARGET-pkg

# save output
mkdir -p output
docker cp container-$OS-$TARGET-$$:/var/lib/jenkins/bigtop/output .
# copy to results container
docker cp output/  container-$OS:/var/lib/jenkins/bigtop/output
docker rm -v container-$OS-$TARGET-$$
