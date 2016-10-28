<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Overview

This bundle provides a complete deployment of
[Apache Spark][] in standalone HA mode as provided
by [Apache Bigtop][]. Ganglia and rsyslog
applications are included to monitor cluster health and syslog activity.

[Apache Spark]: http://spark/apache.org/
[Apache Bigtop]: http://bigtop.apache.org/

## Bundle Composition

The applications that comprise this bundle are spread across 6 units as
follows:

  * Spark (Master and Worker)
    * 2 separate units
  * Zookeeper
    * 3 separate units
  * Ganglia (Web interface for monitoring cluster metrics)
  * Rsyslog (Aggregate cluster syslog events in a single location)
    * Colocated on the Ganglia unit

Deploying this bundle results in a fully configured Apache Bigtop Spark
cluster on any supported cloud, which can be easily scaled to meet workload
demands.


# Deploying

A working Juju installation is assumed to be present. If Juju is not yet set
up, please follow the [getting-started][] instructions prior to deploying this
bundle.

> **Note**: This bundle requires hardware resources that may exceed limits
of Free-tier or Trial accounts on some clouds. To deploy to these
environments, modify a local copy of [bundle.yaml][] with
`zookeeper: num_units: 1` and `machines: 'X': constraints: mem=3G` as needed
to satisfy account limits.

Deploy this bundle from the Juju charm store with the `juju deploy` command:

    juju deploy spark-processing

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart][] with the following syntax: `juju quickstart
spark-processing`.

Alternatively, deploy a locally modified `bundle.yaml` with:

    juju deploy /path/to/bundle.yaml

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart][] with the following syntax: `juju quickstart
/path/to/bundle.yaml`.

The charms in this bundle can also be built from their source layers in the
[Bigtop charm repository][].  See the [Bigtop charm README][] for instructions
on building and deploying these charms locally.

## Network-Restricted Environments
Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate proxy and/or
mirror options. See [Configuring Models][] for more information.

[getting-started]: https://jujucharms.com/docs/stable/getting-started
[bundle.yaml]: https://github.com/apache/bigtop/blob/master/bigtop-deploy/juju/spark-processing/bundle.yaml
[juju-quickstart]: https://launchpad.net/juju-quickstart
[Bigtop charm repository]: https://github.com/apache/bigtop/tree/master/bigtop-packages/src/charm
[Bigtop charm README]: https://github.com/apache/bigtop/blob/master/bigtop-packages/src/charm/README.md
[Configuring Models]: https://jujucharms.com/docs/stable/models-config


# Verifying

## Status
The applications that make up this bundle provide status messages to indicate
when they are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 2 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, perform application smoke tests
to verify that the bundle is working as expected.

## Smoke Test
The spark and zookeeper charms provide a `smoke-test` action that can be used
to verify the respective application is functioning as expected. Run these
actions as follows:

    juju run-action spark/0 smoke-test
    juju run-action zookeeper/0 smoke-test

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do <application>/0 smoke-test`.

Watch the progress of the smoke test actions with:

    watch -n 2 juju show-action-status

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`.

Eventually, all of the actions should settle to `status: completed`.  If
any report `status: failed`, that application is not working as expected. Get
more information about the smoke-test action

    juju show-action-output <action-id>

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`.

## Utilities
Applications in this bundle include Zookeeper command line and Spark web
utilities that can be used to verify information about the cluster.

From the command line, show the list of Zookeeper nodes with the following:

    juju run --unit zookeeper/0 'echo "ls /" | /usr/lib/zookeeper/bin/zkCli.sh'

To access the Spark web console, find the `PUBLIC-ADDRESS` of the spark
application and expose it:

    juju status spark
    juju expose spark

The web interface will be available at the following URL:

    http://SPARK_PUBLIC_IP:8080


# Monitoring

This bundle includes Ganglia for system-level monitoring of the spark and
zookeeper units. Metrics are sent to a centralized ganglia unit for easy
viewing in a browser. To view the ganglia web interface, find the
`PUBLIC-ADDRESS` of the Ganglia application and expose it:

    juju status ganglia
    juju expose ganglia

The web interface will be available at:

    http://GANGLIA_PUBLIC_IP/ganglia


# Logging

This bundle includes rsyslog to collect syslog data from the spark and
zookeeper units. These logs are sent to a centralized rsyslog unit for easy
syslog analysis. One method of viewing this log data is to simply cat syslog
from the rsyslog unit:

    juju run --unit rsyslog/0 'sudo cat /var/log/syslog'

Logs may also be forwarded to an external rsyslog processing service. See
the *Forwarding logs to a system outside of the Juju environment* section of
the [rsyslog README](https://jujucharms.com/rsyslog/) for more information.


# Benchmarking

The `spark` charm in this bundle provides several benchmarks to gauge
the performance of the Spark cluster. Each benchmark is an action that can be
run with `juju run-action`:

    $ juju actions spark | grep Bench
    connectedcomponent                Run the Spark Bench ConnectedComponent benchmark.
    decisiontree                      Run the Spark Bench DecisionTree benchmark.
    kmeans                            Run the Spark Bench KMeans benchmark.
    linearregression                  Run the Spark Bench LinearRegression benchmark.
    logisticregression                Run the Spark Bench LogisticRegression benchmark.
    matrixfactorization               Run the Spark Bench MatrixFactorization benchmark.
    pagerank                          Run the Spark Bench PageRank benchmark.
    pca                               Run the Spark Bench PCA benchmark.
    pregeloperation                   Run the Spark Bench PregelOperation benchmark.
    shortestpaths                     Run the Spark Bench ShortestPaths benchmark.
    sql                               Run the Spark Bench SQL benchmark.
    stronglyconnectedcomponent        Run the Spark Bench StronglyConnectedComponent benchmark.
    svdplusplus                       Run the Spark Bench SVDPlusPlus benchmark.
    svm                               Run the Spark Bench SVM benchmark.

    $ juju run-action spark/0 svdplusplus
    Action queued with id: 339cec1f-e903-4ee7-85ca-876fb0c3d28e

    $ juju show-action-output 339cec1f-e903-4ee7-85ca-876fb0c3d28e
    results:
      meta:
        composite:
          direction: asc
          units: secs
          value: "200.754000"
        raw: |
          SVDPlusPlus,2016-11-02-03:08:26,200.754000,85.974071,.428255,0,SVDPlusPlus-MLlibConfig,,,,,10,,,50000,4.0,1.3,
        start: 2016-11-02T03:08:26Z
        stop: 2016-11-02T03:11:47Z
      results:
        duration:
          direction: asc
          units: secs
          value: "200.754000"
        throughput:
          direction: desc
          units: x/sec
          value: ".428255"
    status: completed
    timing:
      completed: 2016-11-02 03:11:48 +0000 UTC
      enqueued: 2016-11-02 03:08:21 +0000 UTC
      started: 2016-11-02 03:08:26 +0000 UTC


# Scaling

By default, three spark and three zookeeper units are deployed. Scaling these
applications is as simple as adding more units. To add one unit:

    juju add-unit spark
    juju add-unit zookeeper

Multiple units may be added at once.  For example, add four more spark units:

    juju add-unit -n4 spark


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Bigtop](http://bigtop.apache.org/) home page
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
