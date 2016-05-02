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

#BigTop Docker provisioner

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

* Install [Vagrant](http://www.vagrantup.com/downloads.html). Need version 1.6.5 or higher.

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
docker pull bigtop/deploy:centos-6
```
Now, you can start your cluster:

In case of errors you can attempt running as root, or else, ping the mailing list.

## USAGE

1) Create a Bigtop Hadoop cluster by given # of node. (will place a file called config.rb)

```
./docker-hadoop.sh --create 3
```

2) Destroy the cluster.

```
./docker-hadoop.sh --destroy
```

3) Update your cluster after doing configuration changes. (re-run puppet apply)

```
./docker-hadoop.sh --provision
```

4) Run Bigtop smoke tests

```
./docker-hadoop.sh --smoke-tests
```

5) Chain your operations with-in one command.

```
./docker-hadoop.sh --create 5 --smoke-tests --destroy
```

Commands will be executed by following order:

```
create 5 node cluster => run smoke tests => destroy the cluster
```

6) See helper message:

```
./docker-hadoop.sh -h
usage: docker-hadoop.sh [options]
       -c NUM_INSTANCES, --create=NUM_INSTANCES  Create a docker based Bigtop Hadoop cluster
       -p, --provision                           Deploy configuration changes
       -s, --smoke-tests                         Run Bigtop smoke tests
       -d, --destroy                             Destroy the cluster
       -h, --help

```

##Configurations

* There are several parameters can be configured in the vagrantconfig.yaml:

1) Modify memory limit for Docker containers

```
docker:
        memory_size: "2048"

```

2)  If you're running Docker provisioner on OS X or Windows, you can customize the boot2docker VM settings

```
boot2docker:
        memory_size: "4096"
        number_cpus: "2"
```

3) Use different host ports mapping for web UIs

```
namenode_ui_port: "50070"
yarn_ui_port: "8088"
hbase_ui_port: "60010"

```
Note: If running on OS X or Windows, the boot2docker VM should be reloaded after ports changed


##Configure Apache Hadoop ecosystem components
* Choose the ecosystem you want to be deployed by modifying components in vagrantconfig.yaml

```
components: "hadoop,hbase,yarn,..."
```

By default, Apache Hadoop, YARN, and Apache HBase will be installed.
See `bigtop-deploy/puppet/config/site.csv.example` for more details.

##Notes

* Users currently using vagrant 1.6+ is strongly recommanded to upgrade to 1.6.4+, otherwise you will encounter the [issue](https://github.com/mitchellh/vagrant/issues/3769) when installing plugins
