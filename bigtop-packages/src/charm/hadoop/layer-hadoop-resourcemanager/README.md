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

This charm deploys the ResourceManager component of the Apache Bigtop platform
to provide YARN master resources.


## Usage

This charm is intended to be deployed via one of the
[apache bigtop bundles](https://jujucharms.com/u/bigdata-dev/#bundles).
For example:

    juju deploy hadoop-processing

> Note: With Juju versions < 2.0, you will need to use [juju-deployer][] to
deploy the bundle.

This will deploy the Apache Bigtop platform with a workload node
preconfigured to work with the cluster.

You can also manually load and run map-reduce jobs via the plugin charm
included in the bundles linked above:

    juju scp my-job.jar plugin/0:
    juju ssh plugin/0
    hadoop jar my-job.jar


[juju-deployer]: https://pypi.python.org/pypi/juju-deployer/


## Status and Smoke Test

Apache Bigtop charms provide extended status reporting to indicate when they
are ready:

    juju status --format=tabular

This is particularly useful when combined with `watch` to track the on-going
progress of the deployment:

    watch -n 0.5 juju status --format=tabular

The message for each unit will provide information about that unit's state.
Once they all indicate that they are ready, you can perform a "smoke test"
to verify HDFS or YARN services are working as expected. Trigger the
`smoke-test` action by:

    juju action do namenode/0 smoke-test
    juju action do resourcemanager/0 smoke-test

After a few seconds or so, you can check the results of the smoke test:

    juju action status

You will see `status: completed` if the smoke test was successful, or
`status: failed` if it was not.  You can get more information on why it failed
via:

    juju action fetch <action-id>


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


## Hadoop

- [Apache Bigtop](http://bigtop.apache.org/) home page
- [Apache Bigtop issue tracking](http://bigtop.apache.org/issue-tracking.html)
- [Apache Bigtop mailing lists](http://bigtop.apache.org/mail-lists.html)
- [Apache Bigtop charms](https://jujucharms.com/q/apache/bigtop)
