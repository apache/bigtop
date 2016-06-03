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
## Overview

The Apache Hadoop software library is a framework that allows for the
distributed processing of large data sets across clusters of computers
using a simple programming model.

It is designed to scale up from single servers to thousands of machines,
each offering local computation and storage. Rather than rely on hardware
to deliver high-avaiability, the library itself is designed to detect
and handle failures at the application layer, so delivering a
highly-availabile service on top of a cluster of computers, each of
which may be prone to failures.

This bundle provides a complete deployment of the core components of the
[Apache Bigtop](http://bigtop.apache.org/)
platform to perform distributed data analytics at scale.  These components
include:

  * NameNode (HDFS)
  * ResourceManager (YARN)
  * Slaves (DataNode and NodeManager)
  * Client (Bigtop hadoop client)
    * Plugin (subordinate cluster facilitator)

Deploying this bundle gives you a fully configured and connected Apache Bigtop
cluster on any supported cloud, which can be easily scaled to meet workload
demands.


## Deploying this bundle

In this deployment, the aforementioned components are deployed on separate
units. To deploy this bundle, simply use:

    juju deploy hadoop-processing

This will deploy this bundle and all the charms from the [charm store][].

> Note: With Juju versions < 2.0, you will need to use [juju-deployer][] to
deploy the bundle.

You can also build all of the charms from their source layers in the
[Bigtop repository][].  See the [charm package README][] for instructions
to build and deploy the charms.

The default bundle deploys three slave nodes and one node of each of
the other services. To scale the cluster, use:

    juju add-unit slave -n 2

This will add two additional slave nodes, for a total of five.

[charm store]: https://jujucharms.com/
[Bigtop repository]: https://github.com/apache/bigtop
[charm package README]: ../../../bigtop-packages/src/charm/README.md
[juju-deployer]: https://pypi.python.org/pypi/juju-deployer/


## Status and Smoke Test

The services provide extended status reporting to indicate when they are ready:

    juju status --format=tabular

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status --format=tabular

The charms for each master component (namenode, resourcemanager)
also each provide a `smoke-test` action that can be used to verify that each
component is functioning as expected.  You can run them all and then watch the
action status list:

    juju action do namenode/0 smoke-test
    juju action do resourcemanager/0 smoke-test
    watch -n 0.5 juju action status

Eventually, all of the actions should settle to `status: completed`.  If
any go instead to `status: failed` then it means that component is not working
as expected.  You can get more information about that component's smoke test:

    juju action fetch <action-id>


## Monitoring

This bundle includes Ganglia for system-level monitoring of the namenode,
resourcemanager, and slave units. Metrics are sent to a central
ganglia unit for easy viewing in a browser. To view the ganglia web interface,
first expose the service:

    juju expose ganglia

Now find the ganglia public IP address:

    juju status ganglia

The ganglia web interface will be available at:

    http://GANGLIA_PUBLIC_IP/ganglia


## Benchmarking

This charm provides several benchmarks to gauge the performance of your
environment.

The easiest way to run the benchmarks on this service is to relate it to the
[Benchmark GUI][].  You will likely also want to relate it to the
[Benchmark Collector][] to have machine-level information collected during the
benchmark, for a more complete picture of how the machine performed.

[Benchmark GUI]: https://jujucharms.com/benchmark-gui/
[Benchmark Collector]: https://jujucharms.com/benchmark-collector/

However, each benchmark is also an action that can be called manually:

        $ juju action do resourcemanager/0 nnbench
        Action queued with id: 55887b40-116c-4020-8b35-1e28a54cc622
        $ juju action fetch --wait 0 55887b40-116c-4020-8b35-1e28a54cc622

        results:
          meta:
            composite:
              direction: asc
              units: secs
              value: "128"
            start: 2016-02-04T14:55:39Z
            stop: 2016-02-04T14:57:47Z
          results:
            raw: '{"BAD_ID": "0", "FILE: Number of read operations": "0", "Reduce input groups":
              "8", "Reduce input records": "95", "Map output bytes": "1823", "Map input records":
              "12", "Combine input records": "0", "HDFS: Number of bytes read": "18635", "FILE:
              Number of bytes written": "32999982", "HDFS: Number of write operations": "330",
              "Combine output records": "0", "Total committed heap usage (bytes)": "3144749056",
              "Bytes Written": "164", "WRONG_LENGTH": "0", "Failed Shuffles": "0", "FILE:
              Number of bytes read": "27879457", "WRONG_MAP": "0", "Spilled Records": "190",
              "Merged Map outputs": "72", "HDFS: Number of large read operations": "0", "Reduce
              shuffle bytes": "2445", "FILE: Number of large read operations": "0", "Map output
              materialized bytes": "2445", "IO_ERROR": "0", "CONNECTION": "0", "HDFS: Number
              of read operations": "567", "Map output records": "95", "Reduce output records":
              "8", "WRONG_REDUCE": "0", "HDFS: Number of bytes written": "27412", "GC time
              elapsed (ms)": "603", "Input split bytes": "1610", "Shuffled Maps ": "72", "FILE:
              Number of write operations": "0", "Bytes Read": "1490"}'
        status: completed
        timing:
          completed: 2016-02-04 14:57:48 +0000 UTC
          enqueued: 2016-02-04 14:55:14 +0000 UTC
          started: 2016-02-04 14:55:27 +0000 UTC


## Deploying in Network-Restricted Environments

Charms can be deployed in environments with limited network access. To deploy
in this environment, you will need a local mirror to serve required packages.


### Mirroring Packages

You can setup a local mirror for apt packages using squid-deb-proxy.
For instructions on configuring juju to use this, see the
[Juju Proxy Documentation](https://juju.ubuntu.com/docs/howto-proxies.html).


## Contact Information

- <bigdata@lists.ubuntu.com>


## Resources

- [Apache Bigtop](http://bigtop.apache.org/) home page
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Juju Bigtop charms](https://jujucharms.com/q/apache/bigtop)
- [Juju mailing list](https://lists.ubuntu.com/mailman/listinfo/juju)
- [Juju community](https://jujucharms.com/community)
