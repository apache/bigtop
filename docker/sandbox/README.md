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

# Bigtop Docker Sandbox

A tool to build and run big data pseudo cluster using Docker.

## How to run

* Make sure you have Docker installed. We've tested this using [Docker for Mac](https://docs.docker.com/docker-for-mac/)

* Currently supported OS list:

 * centos-6
 * debian-8
 * ubuntu-14.04

* Running Hadoop

```
docker run -ti --privileged -p 50070:50070 bigtop/sandbox:centos-6_hadoop
```

* Running Spark (Standalone mode)

```
docker run -ti --privileged -p 8080:8080 bigtop/sandbox:debian-8_spark
```

* Running Hadoop + HBase

```
docker run -ti --privileged -p 50070:50070 -p 60010:60010 bigtop/sandbox:ubuntu-14.04_hbase
```

## How to build

### Examples

* Build sandbox image that has Hadoop provisioned

```
./build.sh -a bigtop -o debian-8 -c hadoop
```

* Build sandbox image that has Hadoop and Spark provisioned

```
./build.sh -a bigtop -o debian-8 -c "hadoop, spark"
```

* Build sandbox image that has Hadoop and HBase provisioned

```
./build.sh -a bigtop -o debian-8 -c "hadoop, yarn, hbase"
```

### Change the repository of packages

* Change the repository to Bigtop's nightly centos-6 repo

```
export REPO=http://ci.bigtop.apache.org:8080/job/Bigtop-trunk-repos/BUILD_ENVIRONMENTS=centos-6%2Clabel=docker-slave-06//ws/output
./build.sh -a bigtop -o centos-6 -c "hadoop, spark, ignite"
```

### Customize your Hadoop stack

* Edit *site.yaml.template.centos-6_hadoop* to create your own prefered stack

```
cp site.yaml.template.centos-6_hadoop site.yaml.template.centos-6_hadoop_ignite
vim site.yaml.template.centos-6_hadoop_ignite
```

* Add ignite in *hadoop_cluster_node::cluster_components* array and leave the other unchanged

```
...
hadoop_cluster_node::cluster_components: [hadoop, yarn, ignite]
...
```

* Build

```
./build.sh -a bigtop -o centos-6 -f site.yaml.template.centos-6_hadoop_ignite -t my_ignite_stack
```

## Known issues

### Fail to start daemons using systemd

Since systemd requires CAP_SYS_ADMIN, currently any OS using systemd can not successfully started up daemons during image build time.

Daemons can be brought up only if --privileged specified using docker run command.
