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

The Docker Compose definition and wrapper script that creates Bigtop virtual Hadoop cluster on top of Docker containers for you, by pulling from existing publishing bigtop repositories.
This cluster can be used:

- to test bigtop smoke tests
- to test bigtop puppet recipes
- to run integration test with your application

This has been verified on Docker Engine 1.9.1, with api version 1.15, and Docker Compose 1.5.2 on Amazon Linux 2015.09 release.

## Prerequisites

### OS X and Windows

* Install [Docker Toolbox](https://www.docker.com/docker-toolbox)

### Linux

* Install [Docker](https://docs.docker.com/installation/)

* Install [Docker Compose](https://docs.docker.com/compose/install/)

* Start the Docker daemon

```
service docker start
```

## USAGE

1) Create a Bigtop Hadoop cluster by given # of node.

```
./docker-hadoop.sh --create 3
```

2) Destroy the cluster.

```
./docker-hadoop.sh --destroy
```

3) Update your cluster after doing configuration changes on ./config. (re-run puppet apply)

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
usage: docker-hadoop.sh [-C file ] args
       -C file                                   Use alternate file for config.yaml
  commands:
       -c NUM_INSTANCES, --create=NUM_INSTANCES  Create a Docker based Bigtop Hadoop cluster
       -p, --provision                           Deploy configuration changes
       -s, --smoke-tests                         Run Bigtop smoke tests
       -d, --destroy                             Destroy the cluster
       -h, --help
```

##Configurations

* There are several parameters can be configured in config.yaml:

1) Modify memory limit for Docker containers

```
docker:
        memory_size: "2048"

```

2) Use different host ports mapping for web UIs

```
namenode_ui_port: "50070"
yarn_ui_port: "8088"
hbase_ui_port: "60010"

```
Note: If running on OS X or Windows, the boot2docker VM should be reloaded after ports changed


##Configure Apache Hadoop ecosystem components
* Choose the ecosystem you want to be deployed by modifying components in config.yaml

```
components: "hadoop, hbase, yarn,..."
```

By default, Apache Hadoop and YARN will be installed.
