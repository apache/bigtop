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
[Apache Spark](https://spark.apache.org/) in standalone HA mode as provided
by [Apache Bigtop](http://bigtop.apache.org/). Ganglia and rsyslog
applications are included to monitor cluster health and syslog activity.

## Bundle Composition

The applications that comprise this bundle are spread across 7 units as
follows:

  * Spark (Master and Worker)
    * 3 separate units
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
up, please follow the
[getting-started](https://jujucharms.com/docs/2.0/getting-started)
instructions prior to deploying this bundle.

Once ready, deploy this bundle with the `juju deploy` command:

    juju deploy spark-processing

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, use [juju-quickstart](https://launchpad.net/juju-quickstart) with the
following syntax: `juju quickstart spark-processing`.

The charms in this bundle can also be built from their source layers in the
[Bigtop charm repository][].  See the [Bigtop charm README][] for instructions
on building and deploying these charms locally.

[Bigtop charm repository]: https://github.com/apache/bigtop/tree/master/bigtop-packages/src/charm
[Bigtop charm README]: https://github.com/apache/bigtop/blob/master/bigtop-packages/src/charm/README.md


# Verifying

## Status
The applications that make up this bundle provide status messages to
indicate when they are ready:

    juju status

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, perform application smoke tests
to verify that the bundle is working as expected.

## Smoke Test
The spark charm provides a `smoke-test` action that can be used to verify the
application is functioning as expected. Run it as follows:

    juju run-action spark/0 smoke-test

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action do spark/0 smoke-test`.

You can watch the progress of the smoke test action with:

    watch -n 0.5 juju show-action-status

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action status`.

Eventually, the smoke test should settle to `status: completed`.  If
it reports `status: failed`, Spark is not working as expected. Get
more information about the smoke-test action

    juju show-action-output <action-id>

> **Note**: The above assumes Juju 2.0 or greater. If using an earlier version
of Juju, the syntax is `juju action fetch <action-id>`.


# Monitoring

This bundle includes Ganglia for system-level monitoring of the spark units.
Metrics are sent to a centralized ganglia unit for easy viewing in a browser.
To view the ganglia web interface, first expose the service:

    juju expose ganglia

Now find the ganglia public IP address:

    juju status ganglia

The ganglia web interface will be available at:

    http://GANGLIA_PUBLIC_IP/ganglia


# Logging

This bundle includes rsyslog to collect syslog data from the spark unit. These
logs are sent to a centralized rsyslog unit for easy syslog analysis. One
method of viewing this log data is to simply cat syslog from the rsyslog unit:

    juju run --unit rsyslog/0 'sudo cat /var/log/syslog'

Logs may also be forwarded to an external rsyslog processing service. See
the *Forwarding logs to a system outside of the Juju environment* section of
the [rsyslog README](https://jujucharms.com/rsyslog/) for more information.


# Benchmarking

The `spark` charm in this bundle provides several benchmarks to gauge
the performance of the Spark cluster. Each benchmark is an action that can be
run with `juju run-action`:

    $ juju actions spark | grep Bench
    logisticregression                Run the Spark Bench LogisticRegression benchmark.
    matrixfactorization               Run the Spark Bench MatrixFactorization benchmark.
    pagerank                          Run the Spark Bench PageRank benchmark.
    sql                               Run the Spark Bench SQL benchmark.
    streaming                         Run the Spark Bench Streaming benchmark.
    svdplusplus                       Run the Spark Bench SVDPlusPlus benchmark.
    svm                               Run the Spark Bench SVM benchmark.
    trianglecount                     Run the Spark Bench TriangleCount benchmark.

    $ juju run-action spark/0 pagerank
    Action queued with id: 339cec1f-e903-4ee7-85ca-876fb0c3d28e

    $ juju show-action-output 339cec1f-e903-4ee7-85ca-876fb0c3d28e
    results:
      meta:
        composite:
          direction: asc
          units: secs
          value: ".982000"
        raw: |
          PageRank,0,.982000,,,,PageRank-MLlibConfig,,,,,10,12,,200000,4.0,1.3,0.15
        start: 2016-09-22T21:52:26Z
        stop: 2016-09-22T21:52:33Z
      results:
        duration:
          direction: asc
          units: secs
          value: ".982000"
        throughput:
          direction: desc
          units: x/sec
          value: ""
    status: completed
    timing:
      completed: 2016-09-22 21:52:36 +0000 UTC
      enqueued: 2016-09-22 21:52:09 +0000 UTC
      started: 2016-09-22 21:52:13 +0000 UTC


# Scaling

By default, three spark units are deployed. To increase the amount of spark
workers, simply add more units. To add one unit:

    juju add-unit spark

Multiple units may be added at once.  For example, add four more spark units:

    juju add-unit -n4 spark


# Network-Restricted Environments

Charms can be deployed in environments with limited network access. To deploy
in this environment, configure a Juju model with appropriate
proxy and/or mirror options. See
[Configuring Models](https://jujucharms.com/docs/2.0/models-config) for more
information.


# Contact Information

- <bigdata@lists.ubuntu.com>


# Resources

- [Apache Bigtop](http://bigtop.apache.org/) home page
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
