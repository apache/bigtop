    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

------------------------------------------------------------------------------------------------------------------------------------------------------

#BigTop docker provisioner

## Overview

The Vagrantfile definition and wrapper script that creates Bigtop virtual Hadoop cluster on top of Docker containers for you, by pulling from existing publishing bigtop repositories.
This cluster can be used:

- to test bigtop smoke tests
- to test bigtop puppet recipes

These containers start sshd daemons, which vagrant uses to provision and install the hadoop cluster.

This has been verified on docker client 1.2.0, with api version 1.15, and vagrant 1.6.5 on Fedora 20 as well as Centos 6.

## Prerequisites

### OS X and Windows

* Install [VirtualBox](https://www.virtualbox.org/wiki/Downloads)

* Install [Vagrant](http://www.vagrantup.com/downloads.html)

### Linux

* [Kernel Requirements](http://docker.readthedocs.org/en/v0.5.3/installation/kernel/)

* Install [Docker](https://docs.docker.com/installation/)

* Install [Vagrant](http://www.vagrantup.com/downloads.html)

## Getting Started

* Create a 3 node Bigtop Hadoop cluster from scratch

NOTE : SELinux can PREVENT you from ssh'ing into your docker container.
As a brute force way to disable it  - remove it from vi /etc/sysconfig/docker arguments
(fedora and centos may by default launch docker daemon with the --selinux-enabled option)!
In the future, lets update this README with the RIGHT way to allow selinux without breaking 
ssh into a docker container!

```
service docker restart
docker pull bigtop/seed
```

No, you can start your cluster:

```
cd bigtop/bigtop-deploy/vm/docker-puppet
./docker-hadoop.sh --build-image --create 3
```
In case of errors you can attempt running as root, or else, ping the mailing list.

## USAGE

1) Build up the base Docker image that supports Vagrant.

```
./docker-hadoop.sh --build-image
```

2) Create a Bigtop Hadoop cluster by given # of node. (will place a file called config.rb)

```
./docker-hadoop.sh --create 3
```

3) Destroy the cluster.

```
./docker-hadoop.sh --destroy
```

4) Update your cluster after doing configuration changes. (re-run puppet apply)

```
./docker-hadoop.sh --provision
```

5) Chain your operations with-in one command.

```
./docker-hadoop.sh --build-image --create 5 --destroy
```

Commands will be executed by following order:

```
build-image => create 5 node cluster => destroy the cluster
```

6) Run hbase-test.sh to evaluate the deployment.

```
../vagrant-puppet/hbase-test.sh
```

7) See helper message:

```
./docker-hadoop.sh -h
usage: docker-hadoop.sh [options]
       -b, --build-image                         Build base Docker image for Bigtop Hadoop
                                                 (must be exectued at least once before creating cluster)
       -c NUM_INSTANCES, --create NUM_INSTANCES  Create a docker based Bigtop Hadoop cluster
       -p, --provision                           Deploy configuration changes
       -d, --destroy                             Destroy the cluster
       -h, --help
```

##Configure Apache Hadoop ecosystem components

* Choose the ecosystem you want to be deployed by modifying components in provision.sh.

```
     components,hadoop,hbase,yarn,mapred-app,...
```

By default, Apache Hadoop, YARN, and Apache HBase will be installed.
See `bigtop-deploy/puppet/config/site.csv.example` for more details.

##Notes

* Users currently using vagrant 1.6+ is strongly recommanded to upgrade to 1.6.4+, otherwise you will encounter the [issue](https://github.com/mitchellh/vagrant/issues/3769) when installing plguins
